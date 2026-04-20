package com.casso.milktea.ai;

import com.casso.milktea.model.ConversationMessage;
import com.casso.milktea.model.Customer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Direct Gemini API integration — bypasses Spring AI completely.
 *
 * Request flow:
 * 1. IntentDetector (fast path) — handles simple, high-confidence intents
 * 2. Gemini (reasoning path) — complex conversations, ambiguous requests
 *
 * ConfirmationState tracks pending confirmations so we don't ask twice.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroqService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiToolFunctions toolFunctions;
    private final IntentDetector intentDetector;
    private final ConfirmationState confirmationState;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    // ══════════════════════════════════════════════════════════════
    // PUBLIC ENTRY POINT
    // ══════════════════════════════════════════════════════════════

    public ChatResponse chat(Customer customer, String userMessage,
            List<ConversationMessage> history) {

        String msg = userMessage != null ? userMessage.trim() : "";

        // ── Fast path: intent detection ──────────────────────────
        IntentDetector.DetectedIntent intent = intentDetector.detect(msg, customer);

        // GREETING — fast path, no Gemini needed
        if (intent != null && intent.type == IntentDetector.DetectedIntent.Type.GREETING) {
            return new ChatResponse(buildGreeting(), null, null);
        }

        // HELP — fast path
        if (intent != null && intent.type == IntentDetector.DetectedIntent.Type.HELP) {
            return new ChatResponse(buildHelpText(), null, null);
        }

        // AFFIRM / NEGATE — handle pending confirmations FIRST
        if (intent != null
                && (intent.type == IntentDetector.DetectedIntent.Type.AFFIRM
                        || intent.type == IntentDetector.DetectedIntent.Type.NEGATE)) {
            return handleConfirmation(customer, intent.type == IntentDetector.DetectedIntent.Type.AFFIRM);
        }

        // SIZE-ONLY response — user replied with size to a pending confirmation
        if (intent != null && intent.type == IntentDetector.DetectedIntent.Type.SIZE_ONLY) {
            return handleSizeOnly(customer, intent.rawMessage);
        }

        // ── Tool-call intents (fast path — no Gemini needed) ─────
        if (intent != null && intent.isToolCall()) {
            return handleDirectToolCall(customer, intent);
        }

        // ── Complex intent or unknown — Gemini reasoning ─────────
        return chatWithGemini(customer, msg, history);
    }

    // ══════════════════════════════════════════════════════════════
    // FAST PATH — DIRECT TOOL CALLS
    // ══════════════════════════════════════════════════════════════

    private ChatResponse handleDirectToolCall(Customer customer,
            IntentDetector.DetectedIntent intent) {

        return switch (intent.type) {
            case MENU -> {
                String result = toolFunctions.getMenu(intent.category);
                yield new ChatResponse(result, null, null);
            }
            case VIEW_CART -> {
                String result = toolFunctions.viewCart(customer);
                yield new ChatResponse(result, null, null);
            }
            case CLEAR_CART -> {
                String result = toolFunctions.clearCart(customer);
                yield new ChatResponse(result, null, null);
            }
            case BEST_SELLERS -> {
                String result = toolFunctions.getBestSellers(intent.category);
                yield new ChatResponse(result, null, null);
            }
            case CHECKOUT -> {
                // No delivery info yet — need to ask
                String result = toolFunctions.viewCart(customer);
                String followUp = "\n\nCon cho mẹ biết thêm thông tin giao hàng nha:\n"
                        + "👤 Tên người nhận: ...\n📞 SĐT: ...\n📍 Địa chỉ: ...";
                yield new ChatResponse(result + followUp, null, null);
            }
            default -> {
                // Shouldn't happen — treat as Gemini path
                yield null;
            }
        };
    }

    // ══════════════════════════════════════════════════════════════
    // CONFIRMATION HANDLING
    // ══════════════════════════════════════════════════════════════

    /**
     * User said YES or NO to a pending confirmation.
     */
    private ChatResponse handleConfirmation(Customer customer, boolean affirmed) {
        ConfirmationState.PendingAction pending = confirmationState.peek(customer.getId());

        if (pending == null) {
            // No pending action — treat as normal message
            return chatWithGemini(customer, affirmed ? "đúng rồi" : "không", List.of());
        }

        if (!affirmed) {
            // User said NO — clear pending and acknowledge
            confirmationState.clear(customer.getId());
            return new ChatResponse("OK con, không sao cả! Con muốn uống gì khác không? 😊", null, null);
        }

        // User affirmed — execute the pending action
        switch (pending.type()) {
            case ADD_TO_CART -> {
                String result = toolFunctions.addToCart(customer,
                        pending.itemId(), pending.size(), pending.quantity());
                confirmationState.clear(customer.getId());
                // Follow-up
                String followUp = buildAddToCartFollowUp(pending);
                return new ChatResponse(result + "\n\n" + followUp, null, null);
            }
            case CHECKOUT -> {
                // Delivery info was already confirmed — call checkout
                String[] parts = pending.context().split("\\|", -1);
                String name = parts.length > 0 ? parts[0] : null;
                String phone = parts.length > 1 ? parts[1] : null;
                String address = parts.length > 2 ? parts[2] : null;
                String note = pending.itemId(); // note stored in itemId field

                var orderResult = toolFunctions.checkout(customer, name, phone, address, note);
                confirmationState.clear(customer.getId());

                if (orderResult.success()) {
                    return new ChatResponse(orderResult.message(),
                            orderResult.paymentUrl(), orderResult.orderCode());
                } else {
                    // Delivery info missing — ask again
                    return new ChatResponse(orderResult.message(), null, null);
                }
            }
            default -> {
                confirmationState.clear(customer.getId());
                return new ChatResponse("OK con! Con muốn làm gì tiếp nha?", null, null);
            }
        }
    }

    /**
     * User replied with just a size (e.g., "L", "lớn", "size M").
     */
    private ChatResponse handleSizeOnly(Customer customer, String msg) {
        ConfirmationState.PendingAction pending = confirmationState.peek(customer.getId());

        if (pending != null && pending.type() == ConfirmationState.ActionType.ADD_TO_CART) {
            // User wants to change the size in the pending add-to-cart
            String newSize = extractSize(msg);
            if (newSize != null && !newSize.equals(pending.size())) {
                // Update pending action with new size
                var updated = new ConfirmationState.PendingAction(
                        pending.type(), pending.itemId(), pending.itemName(),
                        newSize, pending.quantity(), pending.timestamp(), pending.context());
                confirmationState.save(customer.getId(), updated);

                return new ChatResponse(String.format(
                        "OK, đổi sang size %s nhe con! %dx %s (size %s) như vậy đúng không?",
                        newSize, pending.quantity(), pending.itemName(), newSize), null, null);
            } else if (newSize != null) {
                // Same size confirmed
                return handleConfirmation(customer, true);
            }
        }

        return chatWithGemini(customer, msg, List.of());
    }

    // ══════════════════════════════════════════════════════════════
    // GEMINI REASONING PATH
    // ══════════════════════════════════════════════════════════════

    private ChatResponse chatWithGemini(Customer customer, String userMessage,
            List<ConversationMessage> history) {

        try {
            List<Map<String, Object>> contents = buildContents(customer, history, userMessage);

            // Turn 1: send to Gemini with tools
            JsonNode response = callGeminiWithRetry(contents, true);
            String text = extractText(response);
            List<Map<String, Object>> toolCalls = extractFunctionCalls(response);

            if (!toolCalls.isEmpty()) {
                // Append model's text as a content part
                if (text != null && !text.isBlank()) {
                    contents.add(part("model", text));
                }

                // Execute each tool and append results
                for (Map<String, Object> tc : toolCalls) {
                    String toolName = (String) tc.get("name");
                    JsonNode rawArgs = (JsonNode) tc.get("args");
                    String result = executeTool(toolName, rawArgs, customer, contents);

                    contents.add(functionResponsePart(toolName, result));
                    log.info("Tool [{}] → {}", toolName,
                            result.length() > 120 ? result.substring(0, 120) + "..." : result);
                }

                // Turn 2: Gemini generates natural response with tool results
                response = callGeminiWithRetry(contents, false);
                text = extractText(response);
            }

            if (text == null || text.isBlank()) {
                text = "Mẹ nghe con rồi nhưng chưa hiểu lắm 😅 Con nhắn rõ hơn giúp mẹ nha!";
            }

            return new ChatResponse(text, null, null);

        } catch (Exception e) {
            log.error("GroqService error: {}", e.getMessage(), e);
            return new ChatResponse(errorMessage(e), null, null);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // TOOL EXECUTION
    // ══════════════════════════════════════════════════════════════

    /**
     * Execute a tool and return the result string.
     * For add_to_cart/checkout: also saves pending confirmation state.
     */
    private String executeTool(String toolName, JsonNode rawArgs,
            Customer customer, List<Map<String, Object>> contents) {

        JsonNode args = rawArgs != null ? rawArgs : objectMapper.createObjectNode();

        return switch (toolName) {
            case "find_item_by_name" -> {
                // FIX: Parse FOUND: result and SAVE pending action immediately.
                // This is the CORE bug fix — without this, ConfirmationState is NEVER saved
                // and user YES → handleConfirmation → no pending → Gemini loop → infinite.
                String raw = toolFunctions.findItemByName(extractString(args, "name"));

                if (raw != null && raw.startsWith("FOUND:")) {
                    // Parse: FOUND:itemId|name|priceM|priceL
                    String[] parts = raw.substring(6).split("\\|", -1);
                    if (parts.length >= 4) {
                        String itemId = parts[0].trim();
                        String itemName = parts[1].trim();
                        String priceM = parts[2].trim();
                        String priceL = parts[3].trim();

                        // Save pending ADD_TO_CART with default M, qty 1
                        confirmationState.saveAddToCart(
                                customer.getId(), itemId, itemName, "M", 1, raw);

                        // Return formatted info for Gemini to ask confirmation
                        yield String.format(
                                "FOUND_ITEM: %s\nMã: %s\nGiá M: %sđ / Giá L: %sđ\n\n"
                                + "Hãy hỏi khách: 'Con muốn size M hay L? Mấy ly?' "
                                + "Sau khi khách trả lời đủ (size + số lượng), "
                                + "gọi add_to_cart(%s, <size>, <qty>).",
                                itemName, itemId, priceM, priceL, itemId);
                    }
                }
                // Not found or error — return as-is for Gemini to handle
                yield raw;
            }

            case "get_menu" -> toolFunctions.getMenu(extractString(args, "category"));

            case "view_cart" -> toolFunctions.viewCart(customer);

            case "add_to_cart" -> {
                String itemId = extractString(args, "item_id");
                String size = normalizeSize(extractString(args, "size"));
                int qty = extractInt(args, "quantity", 1);
                String result = toolFunctions.addToCart(customer, itemId, size, qty);
                // Clear any stale pending confirmations after adding
                confirmationState.clear(customer.getId());
                yield result;
            }

            case "remove_from_cart" -> toolFunctions.removeFromCart(customer,
                    extractString(args, "item_id"), normalizeSize(extractString(args, "size")));

            case "update_cart_item" -> toolFunctions.updateCartItem(customer,
                    extractString(args, "item_id"), normalizeSize(extractString(args, "size")),
                    extractInt(args, "quantity", 1));

            case "get_best_sellers" -> toolFunctions.getBestSellers(
                    extractString(args, "category"));

            case "checkout" -> {
                String name = extractString(args, "name");
                String phone = extractString(args, "phone");
                String address = extractString(args, "address");
                String note = extractString(args, "note");

                // Validate — if missing info, DO NOT call checkout
                String missing = validateDeliveryInfo(name, phone, address);
                if (missing != null) {
                    confirmationState.clear(customer.getId()); // clear stale state
                    yield "THIẾU_THÔNG_TIN: " + missing
                            + ". Hãy hỏi khách cung cấp thông tin còn thiếu.";
                }

                // All info present — execute checkout
                var orderResult = toolFunctions.checkout(customer, name, phone, address, note);
                confirmationState.clear(customer.getId());

                if (orderResult.success()) {
                    yield "CHECKOUT_THÀNH_CÔNG|"
                            + orderResult.orderCode() + "|"
                            + orderResult.paymentUrl() + "|"
                            + orderResult.message();
                } else {
                    yield "CHECKOUT_THẤT_BẠI: " + orderResult.message();
                }
            }

            case "recommend" -> toolFunctions.getRecommendation(
                    extractString(args, "preference"));

            case "clear_cart" -> toolFunctions.clearCart(customer);

            default -> "Hàm '" + toolName + "' không tồn tại — con nhắn lại giúp mẹ nha!";
        };
    }

    /**
     * Check if delivery info is complete.
     * Returns null if all present, error message if missing.
     */
    private String validateDeliveryInfo(String name, String phone, String address) {
        StringBuilder sb = new StringBuilder();
        if (name == null || name.isBlank())
            sb.append("tên người nhận, ");
        if (phone == null || phone.isBlank())
            sb.append("số điện thoại, ");
        if (address == null || address.isBlank())
            sb.append("địa chỉ giao hàng, ");
        if (sb.isEmpty())
            return null;
        return sb.substring(0, sb.length() - 2);
    }

    // ══════════════════════════════════════════════════════════════
    // GEMINI HTTP CALLS
    // ══════════════════════════════════════════════════════════════

    private JsonNode callGemini(List<Map<String, Object>> contents, boolean withTools)
            throws Exception {

        String url = baseUrl + "/models/" + model + ":generateContent?key=" + apiKey;

        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode genConfig = body.putObject("generationConfig");
        genConfig.put("maxOutputTokens", 1024);
        genConfig.put("temperature", 0.7);

        ArrayNode contentsArray = body.putArray("contents");
        for (Map<String, Object> c : contents) {
            contentsArray.add(objectMapper.valueToTree(c));
        }

        if (withTools) {
            ArrayNode toolsArray = body.putArray("tools");
            ObjectNode tool = toolsArray.addObject();
            ArrayNode decls = tool.putArray("functionDeclarations");
            for (Map<String, Object> f : buildFunctionDeclarations()) {
                decls.add(objectMapper.valueToTree(f));
            }
        }

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        String json = objectMapper.writeValueAsString(body);
        String responseStr = restTemplate.postForObject(url,
                new org.springframework.http.HttpEntity<>(json, headers),
                String.class);

        return objectMapper.readTree(responseStr);
    }

    private JsonNode callGeminiWithRetry(List<Map<String, Object>> contents, boolean withTools)
            throws Exception {
        int attempts = 0;
        Exception last = null;

        while (attempts < 3) {
            try {
                return callGemini(contents, withTools);
            } catch (Exception e) {
                last = e;
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("429") || msg.contains("rate limit")
                        || msg.contains("quota") || msg.contains("resource exhausted")
                        || msg.contains("connection") || msg.contains("refused")
                        || msg.contains("timeout") || msg.contains("timed out")) {
                    attempts++;
                    if (attempts < 3) {
                        int waitMs = attempts * 2000;
                        log.warn("Gemini retry {}/3 ({}): {}", attempts, e.getMessage(), waitMs);
                        try {
                            Thread.sleep(waitMs);
                        } catch (InterruptedException ie) {
                            break;
                        }
                        continue;
                    }
                }
                throw e;
            }
        }
        throw last != null ? last
                : new RuntimeException("Gemini call failed after 3 attempts");
    }

    // ══════════════════════════════════════════════════════════════
    // GEMINI CONTENTS BUILDERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Build Gemini contents[] from system prompt + history + new user message.
     */
    private List<Map<String, Object>> buildContents(Customer customer,
            List<ConversationMessage> history, String userMessage) {

        List<Map<String, Object>> contents = new ArrayList<>();

        // System prompt as first user turn
        contents.add(part("user", systemPrompt(customer)));

        // Conversation history
        for (ConversationMessage msg : history) {
            String role = "user".equals(msg.getRole()) ? "user" : "model";
            contents.add(part(role, msg.getContent()));
        }

        // New user message
        contents.add(part("user", userMessage));
        return contents;
    }

    /** Build a Gemini Part (content item with role + parts). */
    private Map<String, Object> part(String role, String text) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("role", role);
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("text", text);
        p.put("parts", List.of(inner));
        return p;
    }

    /** Build a function-response part for tool results. */
    private Map<String, Object> functionResponsePart(String name, String result) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("role", "user");
        Map<String, Object> inner = new LinkedHashMap<>();
        Map<String, Object> fr = new LinkedHashMap<>();
        fr.put("name", name);
        fr.put("response", Map.of("result", result));
        inner.put("functionResponse", fr);
        p.put("parts", List.of(inner));
        return p;
    }

    // ══════════════════════════════════════════════════════════════
    // RESPONSE EXTRACTION
    // ══════════════════════════════════════════════════════════════

    private String extractText(JsonNode response) {
        try {
            JsonNode parts = response.path("candidates").get(0)
                    .path("content").path("parts");
            StringBuilder sb = new StringBuilder();
            for (JsonNode p : parts) {
                String t = p.path("text").asText(null);
                if (t != null && !t.isBlank()) {
                    if (sb.length() > 0)
                        sb.append("\n");
                    sb.append(t);
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> extractFunctionCalls(JsonNode response) {
        List<Map<String, Object>> calls = new ArrayList<>();
        try {
            JsonNode parts = response.path("candidates").get(0)
                    .path("content").path("parts");
            for (JsonNode p : parts) {
                JsonNode fc = p.path("functionCall");
                if (!fc.isMissingNode()) {
                    Map<String, Object> call = new LinkedHashMap<>();
                    call.put("name", fc.path("name").asText());
                    call.put("args", fc.path("args"));
                    calls.add(call);
                }
            }
        } catch (Exception e) {
            log.warn("extractFunctionCalls failed: {}", e.getMessage());
        }
        return calls;
    }

    // ══════════════════════════════════════════════════════════════
    // SYSTEM PROMPT — VIETNAMESE MOTHER AS TEA SHOP OWNER
    // ══════════════════════════════════════════════════════════════

    /**
     * Build the system prompt.
     * This is the MOST IMPORTANT part — it defines how the AI behaves.
     *
     * Key principles:
     * 1. NEVER ask confirmation if user already provided all info
     * 2. Always handle the correct intent (cart, menu, etc.)
     * 3. Speak naturally as a Vietnamese mother
     * 4. Only call function when it makes sense
     */
    private String systemPrompt(Customer customer) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Ban la Me — ba chu quan tra sua Casso. Goi khach la "con".

                LUONG XU LY DAT MON (TUYET DOI PHAI DUNG THU TU):
                ==========================================

                BUOC 1: Khach noi ten mon -> goi find_item_by_name(ten_mon)
                   -> Tool tra ve: FOUND_ITEM: Ten | Ma: xxx | Gia M: xd / L: yd
                   -> GOI NGAY add_to_cart(ma, "M", 1)
                   -> KHONG hoi confirm gi ca

                BUOC 2: add_to_cart tra ve "Da them..."
                   -> Tra loi: "Da them X ly Y vao gio nha con!" 🛒
                   -> Hoi: "Con muon them gi nua khong?"

                CAC TRUONG HOP KHAC:
                ==========================================

                » "menu" / "thuc don" / "xem thuc don"
                   -> Goi get_menu() -> IN DAY DU menu

                » "gio hang" / "bill" / "xem don" / "check cart"
                   -> Goi view_cart() -> IN GIO HANG

                » "thanh toan" / "checkout"
                   -> Goi view_cart() xem gio
                   -> TRONG -> "Gio hang trong con oi!"
                   -> CO MON -> hoi: "Cho me biet: Ten, SDT, dia chi giao hang nha"

                » "xoa gio" -> Goi clear_cart()
                » "ban chay" -> Goi get_best_sellers()

                QUY TAC 1: Co itemId + size -> GOI NGAY, KHONG HOI
                QUY TAC 2: Hoi gio hang -> IN GIO HANG, khong hoi gi khac
                QUY TAC 3: KHONG bao gio hoi lai thong tin da co
                QUY TAC 4: Noi CHUYEN, dung emoji 🧋😊
                """);

        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════
    // GEMINI FUNCTION DECLARATIONS
    // ══════════════════════════════════════════════════════════════

    private List<Map<String, Object>> buildFunctionDeclarations() {
        return List.of(
                fd("find_item_by_name",
                        "Tim mon trong menu bang ten tieng Viet. "
                                + "VD: 'tra sua socola', 'matcha', 'sua tuoi'. "
                                + "Tra ve 'FOUND:itemId|ten|priceM|priceL' hoac loi khong tim thay.",
                        List.of(param("name", "string",
                                "Ten mon tieng Viet (VD: 'tra sua socola', 'matcha')"))),

                fd("get_menu",
                        "Xem TOAN BO menu: ten mon, gia M/L theo tung danh muc. "
                                + "Luon tra day du menu khi khach yeu cau.",
                        List.of(param("category", "string",
                                "Danh muc (tuy chon): Tra Sua, Tra Trai Cay, Ca Phe, Da Xay, Topping"))),

                fd("view_cart",
                        "Xem gio hang hien tai cua khach. Tra ve danh sach mon + tong tien.",
                        Collections.emptyList()),

                fd("clear_cart",
                        "Xoa toan bo gio hang.",
                        Collections.emptyList()),

                fd("add_to_cart",
                        "Them mon vao gio hang. CHI goi SAU KHI da xac nhan voi khach "
                                + "(hoac khach da noi du: ten mon + size + so luong).",
                        List.of(
                                param("item_id", "string",
                                        "Ma mon (VD: TS01, TSTC01...) — lay tu find_item_by_name"),
                                param("size", "string", "Size: M (Medium) hoac L (Large)"),
                                param("quantity", "integer", "So luong ly, mac dinh 1"))),

                fd("remove_from_cart",
                        "Xoa mot mon khoi gio hang.",
                        List.of(
                                param("item_id", "string", "Ma mon"),
                                param("size", "string", "Size M hoac L"))),

                fd("update_cart_item",
                        "Sua so luong mot mon trong gio. Neu quantity = 0 thi xoa mon.",
                        List.of(
                                param("item_id", "string", "Ma mon"),
                                param("size", "string", "Size M hoac L"),
                                param("quantity", "integer", "So luong moi"))),

                fd("get_best_sellers",
                        "Xem cac mon ban chay nhat.",
                        List.of(param("category", "string", "Danh muc (tuy chon)"))),

                fd("checkout",
                        "CHOT DON va tao link thanh toan QR payOS. "
                                + "BAT BUOC phai co: ten nguoi nhan, SDT, dia chi giao hang. "
                                + "Neu thieu thong tin -> KHONG goi, hoi khach truoc!",
                        List.of(
                                param("name", "string", "Ten nguoi nhan"),
                                param("phone", "string", "So dien thoai nguoi nhan"),
                                param("address", "string", "Dia chi giao hang day du"),
                                param("note", "string", "Ghi chu don hang (tuy chon)"))),

                fd("recommend",
                        "Go y do uong theo so thich. VD: 'socola', 'tra xanh', 'matcha'.",
                        List.of(param("preference", "string", "So thich hoac ban than (tuy chon)"))));
    }

    private Map<String, Object> fd(String name, String desc, List<Map<String, Object>> params) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name);
        f.put("description", desc);
        Map<String, Object> paramObj = new LinkedHashMap<>();
        paramObj.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (Map<String, Object> p : params) {
            String pname = (String) p.get("name");
            properties.put(pname, Map.of(
                    "type", p.get("type"),
                    "description", p.get("description")));
            required.add(pname);
        }
        paramObj.put("properties", properties);
        paramObj.put("required", required);
        f.put("parameters", paramObj);
        return f;
    }

    private Map<String, Object> param(String name, String type, String description) {
        return Map.of("name", name, "type", type, "description", description);
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    private String extractString(JsonNode args, String field) {
        JsonNode n = args.path(field);
        return n.isMissingNode() || n.isNull() ? null : n.asText();
    }

    private int extractInt(JsonNode args, String field, int fallback) {
        JsonNode n = args.path(field);
        return n.isMissingNode() || n.isNull() ? fallback : n.asInt();
    }

    private String normalizeSize(String size) {
        if (size == null || size.isBlank())
            return "M";
        String s = size.trim().toUpperCase();
        return s.startsWith("L") ? "L" : "M";
    }

    private String extractSize(String msg) {
        if (msg == null)
            return null;
        String s = msg.toUpperCase().trim();
        if (s.equals("L") || s.equals("LỚN") || s.equals("LARGE"))
            return "L";
        if (s.equals("M") || s.equals("NHỎ") || s.equals("MEDIUM"))
            return "M";
        // "size L", "size M"
        if (s.contains("L") && !s.contains("M"))
            return "L";
        if (s.contains("M"))
            return "M";
        return null;
    }

    private String buildGreeting() {
        String[] greetings = {
                "Chào con! Mẹ là Mẹ ở Casso nè 🧋 Con muốn uống gì hôm nay?",
                "Hey con! Vô quán rồi hả? Mẹ đang đợi nè! 😊",
                "Chào con! Mừng con ghé quán nha! Uống gì cho mẹ biết? 😊"
        };
        return greetings[(int) (System.currentTimeMillis() / 60_000) % greetings.length];
    }

    private String buildHelpText() {
        return """
                🧋 Mẹ là AI của quán trà sữa Casso nè!

                Con có thể:
                • "xem menu" — xem toàn bộ thực đơn
                • "bán chạy" — xem món hot nhất
                • Nói tên món con thích — VD: "trà sữa socola"
                • "xem giỏ hàng" — kiểm tra đơn đã đặt
                • "thanh toán" — tạo QR chuyển khoản

                Con muốn uống gì nào? 😊
                """;
    }

    private String buildAddToCartFollowUp(ConfirmationState.PendingAction pending) {
        return String.format(
                "Thêm %dx %s (size %s) vào giỏ rồi nha con! 🛒\n%s",
                pending.quantity(), pending.itemName(), pending.size(),
                buildContinuePrompt());
    }

    private String buildContinuePrompt() {
        return "Con muốn uống thêm gì nữa không, hay thanh toán luôn? 😊";
    }

    private String errorMessage(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("timeout") || msg.contains("timed out"))
            return "Mạng hơi chậm con ơi, con nhắn lại giúp mẹ nha 😅";
        if (msg.contains("connection") || msg.contains("refused"))
            return "Mẹ gặp chút trục trặc kết nối, con chờ một lát rồi thử lại nhe 😅";
        if (msg.contains("429") || msg.contains("rate limit") || msg.contains("quota")
                || msg.contains("resource exhausted"))
            return "Mẹ đang bận lắc con ơi, con nhắn lại sau 1-2 phút nha 😅";
        if (msg.contains("401") || msg.contains("unauthorized") || msg.contains("api key")
                || msg.contains("invalid"))
            return "Hệ thống đang bảo trì, con thử lại sau ít phút nha 😅";
        return "Mẹ đang bận xíu, con chờ một chút rồi nhắc lại nha 😅";
    }

    // ══════════════════════════════════════════════════════════════
    // RESPONSE RECORD
    // ══════════════════════════════════════════════════════════════

    public record ChatResponse(String message, String paymentUrl, Long orderCode) {
        public ChatResponse(String message) {
            this(message, null, null);
        }
    }
}
