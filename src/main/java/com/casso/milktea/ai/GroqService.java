package com.casso.milktea.ai;

import com.casso.milktea.model.ConversationMessage;
import com.casso.milktea.model.Customer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Direct Groq API integration — bypasses Spring AI completely.
 *
 * Tool design (new):
 * - find_item_by_name(name)   → fuzzy search by Vietnamese name → FOUND:TS01|Tên|priceM|priceL
 * - get_menu(category?)       → return full menu
 * - view_cart()               → show cart
 * - add_to_cart(itemId,size,qty) → add AFTER confirmation
 * - remove_from_cart(itemId,size)
 * - update_cart_item(itemId,size,qty)
 * - get_best_sellers(category?) → top sellers
 * - checkout(name,phone,addr,note?) → create order + payOS link
 * - recommend(preference?)     → suggest by preference
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroqService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiToolFunctions toolFunctions;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    // ── Public entry point ──────────────────────────────────────

    public ChatResponse chat(Customer customer, String userMessage,
            List<ConversationMessage> history) {
        // Fast path: greetings never need Groq
        if (isGreeting(userMessage)) {
            return new ChatResponse(buildGreeting());
        }

        try {
            List<Map<String, String>> messages = buildMessages(customer, history, userMessage);

            // Turn 1
            JsonNode groqResponse = callGroqWithRetry(messages, true);
            String assistantText = extractText(groqResponse);
            JsonNode toolCalls = groqResponse.path("choices").get(0)
                    .path("message").path("tool_calls");

            if (toolCalls.isArray() && !toolCalls.isEmpty()) {
                // Groq wants to call tool(s)
                Map<String, String> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", assistantText != null ? assistantText : "");
                messages.add(assistantMsg);

                for (JsonNode tc : toolCalls) {
                    String toolName = tc.path("function").path("name").asText();
                    String rawArgs = tc.path("function").path("arguments").asText();
                    String result = executeTool(toolName, rawArgs, customer);

                    Map<String, String> toolMsg = new HashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", tc.path("id").asText());
                    toolMsg.put("content", result);
                    messages.add(toolMsg);

                    log.info("Tool [{}] → {}", toolName,
                            result.length() > 100 ? result.substring(0, 100) + "..." : result);
                }

                // Turn 2: Groq generates natural response with tool results
                groqResponse = callGroqWithRetry(messages, false);
                assistantText = extractText(groqResponse);
            }

            if (assistantText == null || assistantText.isBlank()) {
                assistantText = "Mẹ nghe con rồi nhưng chưa hiểu lắm 😅 Con nhắn rõ hơn giúp mẹ nha!";
            }

            return new ChatResponse(assistantText);

        } catch (Exception e) {
            log.error("GroqService error: {}", e.getMessage(), e);
            return new ChatResponse(errorMessage(e));
        }
    }

    // ── Greeting fast path (no Groq call) ─────────────────────────

    private boolean isGreeting(String userMessage) {
        if (userMessage == null || userMessage.length() > 40) return false;
        String msg = userMessage.trim().toLowerCase();
        return msg.contains("xin chao") || msg.contains("chào") || msg.contains("hello")
                || msg.matches(".*\\bhi\\b.*") || msg.matches(".*\\bhey\\b.*")
                || msg.contains("good morning") || msg.contains("good afternoon")
                || msg.contains("buổi sáng") || msg.contains("buổi trưa") || msg.contains("buổi chiều");
    }

    private String buildGreeting() {
        String[] greetings = {
                "Chào con! Mẹ là Mẹ ở Casso nè 🧋 Con muốn uống gì hôm nay?",
                "Hey con! Vô quán rồi hả? Mẹ đang đợi nè! 😊",
                "Chào con! Mừng con ghé quán nha! Uống gì cho mẹ biết? 😊"
        };
        return greetings[(int) (System.currentTimeMillis() / 60000) % greetings.length];
    }

    // ── HTTP calls ────────────────────────────────────────────────

    private JsonNode callGroq(List<Map<String, String>> messages, boolean withTools)
            throws Exception {
        String url = baseUrl + "/v1/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.1);   // 0.0 → 0.1 to reduce tool_call instability
        body.put("max_tokens", 1024);
        body.put("messages", messages);

        if (withTools) {
            body.put("tools", buildTools());
            body.put("tool_choice", "auto");
        }

        String json = objectMapper.writeValueAsString(body);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        String response = restTemplate.postForObject(url,
                new org.springframework.http.HttpEntity<>(json, headers),
                String.class);

        return objectMapper.readTree(response);
    }

    private JsonNode callGroqWithRetry(List<Map<String, String>> messages, boolean withTools)
            throws Exception {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < 3) {
            try {
                return callGroq(messages, withTools);
            } catch (Exception e) {
                lastException = e;
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("429") || msg.contains("rate limit")
                        || msg.contains("connection") || msg.contains("refused")
                        || msg.contains("timeout") || msg.contains("timed out")) {
                    attempts++;
                    if (attempts < 3) {
                        int waitMs = attempts * 1500;
                        log.warn("Groq retry {}/3 ({}): {}", attempts, e.getMessage(), waitMs);
                        try { Thread.sleep(waitMs); } catch (InterruptedException ie) { break; }
                        continue;
                    }
                }
                throw e;   // non-retryable error
            }
        }
        throw lastException != null ? lastException
                : new RuntimeException("Groq call failed after 3 attempts");
    }

    // ── Tool execution ──────────────────────────────────────────

    private String executeTool(String toolName, String rawArgs, Customer customer)
            throws Exception {
        JsonNode args = parseArgs(rawArgs);

        return switch (toolName) {
            // 1. Tìm món theo tên tiếng Việt (NEW — core fix)
            case "find_item_by_name" -> toolFunctions.findItemByName(
                    args.has("name") && !args.get("name").isNull()
                            ? args.get("name").asText()
                            : null);

            // 2. Menu
            case "get_menu" -> toolFunctions.getMenu(
                    args.has("category") && !args.get("category").isNull()
                            ? args.get("category").asText()
                            : null);

            // 3. Xem giỏ
            case "view_cart" -> toolFunctions.viewCart(customer);

            // 4. Thêm vào giỏ
            case "add_to_cart" -> toolFunctions.addToCart(customer,
                    args.get("item_id").asText(),
                    args.has("size") && !args.get("size").isNull()
                            ? args.get("size").asText().toUpperCase()
                            : "M",
                    args.has("quantity") && !args.get("quantity").isNull()
                            ? args.get("quantity").asInt()
                            : 1);

            // 5. Xóa khỏi giỏ
            case "remove_from_cart" -> toolFunctions.removeFromCart(customer,
                    args.get("item_id").asText(),
                    args.has("size") && !args.get("size").isNull()
                            ? args.get("size").asText().toUpperCase()
                            : "M");

            // 6. Sửa số lượng
            case "update_cart_item" -> toolFunctions.updateCartItem(customer,
                    args.get("item_id").asText(),
                    args.has("size") && !args.get("size").isNull()
                            ? args.get("size").asText().toUpperCase()
                            : "M",
                    args.has("quantity") && !args.get("quantity").isNull()
                            ? args.get("quantity").asInt()
                            : 1);

            // 7. Món bán chạy
            case "get_best_sellers" -> toolFunctions.getBestSellers(
                    args.has("category") && !args.get("category").isNull()
                            ? args.get("category").asText()
                            : null);

            // 8. Checkout + thu thập thông tin giao hàng (NEW)
            case "checkout" -> {
                var res = toolFunctions.checkout(customer,
                        args.has("name") && !args.get("name").isNull()
                                ? args.get("name").asText()
                                : null,
                        args.has("phone") && !args.get("phone").isNull()
                                ? args.get("phone").asText()
                                : null,
                        args.has("address") && !args.get("address").isNull()
                                ? args.get("address").asText()
                                : null,
                        args.has("note") && !args.get("note").isNull()
                                ? args.get("note").asText()
                                : null);
                yield res.toToolResult();
            }

            // 9. Gợi ý theo sở thích (NEW)
            case "recommend" -> toolFunctions.getRecommendation(
                    args.has("preference") && !args.get("preference").isNull()
                            ? args.get("preference").asText()
                            : null);

            default -> "Hàm '" + toolName + "' không tồn tại — con nhắn lại giúp mẹ nha!";
        };
    }

    /**
     * Safely parse JSON args. Handles "null" literal string from Groq.
     */
    private JsonNode parseArgs(String rawArgs) {
        if (rawArgs == null || rawArgs.isBlank() || "null".equals(rawArgs.trim())) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode node = objectMapper.readTree(rawArgs);
            return (node != null && !node.isNull()) ? node : objectMapper.createObjectNode();
        } catch (Exception e) {
            log.warn("Failed to parse args '{}', using empty object", rawArgs);
            return objectMapper.createObjectNode();
        }
    }

    // ── Message building ────────────────────────────────────────

    private List<Map<String, String>> buildMessages(Customer customer,
            List<ConversationMessage> history, String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt()));
        for (var msg : history) {
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));
        return messages;
    }

    private String systemPrompt() {
        return """
                Ban la Me - ba chu quan tra sua Casso, noi chuyen nhieu, goi khach la "con".

                PERSONALITY: Noi chuyen tu nhien nhu nguoi Ban than, co cam xuc, goi y tu van tot, de thuong.

                WHEN REPLYING:
                - NOI CHUYEN TU NHIEN, CO CAM XUC — khong giong doc bao.
                - LUON IN DAY DU KET QUA KHI GOI FUNCTION (menu, gio hang, bill...).
                - NEU goi find_item_by_name() va ket qua bat dau bang "FOUND:" → TRANH VIET LAI MENU,
                  chi can HOI XAC NHAN: "Minh tim thay [ten mon] — [size] nhu vay nhe con?"
                - NEU goi get_menu() → PHAI IN NGUYEN MENU, khong rut gon.

                CONVERSATION FLOW:
                1. Khach muon dat mon → goi find_item_by_name(name) truoc.
                2. Neu tim thay → HOI XAC NHAN: "[ten mon], size M/L, [so luong] nhu vay nhe con?"
                3. Sau khi khach xac nhan → goi add_to_cart().
                4. Khach hoi gio hang / xem don → goi view_cart().
                5. Khach muon thanh toan → HOI thong tin: ten, SDT, dia chi giao hang.
                   Sau khi co day du thong tin → goi checkout().
                6. Khach hoi goi y → goi recommend(preference).

                MENU CATEGORIES: Tra Sua, Tra Trai Cay, Ca Phe, Da Xay, Topping
                AVAILABLE SIZES: M (Medium) va L (Large)
                MAX_TOPPING: 3 topping 1 lan

                RULES:
                - Chi ban mon trong menu.
                - Khi xac nhan them gio → hoi: "[mon], size [M/L], [sl] ly nhu vay nhe con?"
                - Khong tu tinh tien.
                - Khong ro thi hoi lai.
                - Giao hang: thu thap TEN + SDT + DIA CHI truoc khi checkout.
                """;
    }

    // ── Tool schema (9 tools) ────────────────────────────────────

    private List<Map<String, Object>> buildTools() {
        return List.of(
                // 1. Tìm món theo tên (thay vì item_id)
                tool("find_item_by_name",
                        "Tim mon trong menu bang ten tieng Viet (VD: 'tra sua socola', 'sua tuoi'). "
                                + "Tra ve 'FOUND:itemId|ten|giaM|giaL' hoac loi khong tim thay.",
                        Map.of("name", Map.of("type", "string",
                                "description", "Ten mon tieng Viet (VD: 'tra sua socola', 'matcha')"))),

                // 2. Xem menu
                tool("get_menu",
                        "Xem toan bo menu: ten mon, gia M/L.",
                        Map.of("category", Map.of("type", "string",
                                "description", "Danh muc (tuy chon): Tra Sua, Tra Trai Cay, Ca Phe, Da Xay, Topping"))),

                // 3. Xem giỏ hàng
                tool("view_cart",
                        "Xem gio hang hien tai.",
                        Map.of()),

                // 4. Thêm vào giỏ (sau khi khách xác nhận)
                tool("add_to_cart",
                        "Them mon vao gio hang (DA xac nhan voi khach roi).",
                        Map.of(
                                "item_id", Map.of("type", "string",
                                        "description", "Ma mon (VD: TS01, TSTC...) — lay tu find_item_by_name"),
                                "size", Map.of("type", "string",
                                        "description", "Size M hoac L (mac dinh M)"),
                                "quantity", Map.of("type", "integer",
                                        "description", "So luong, mac dinh 1"))),

                // 5. Xóa khỏi giỏ
                tool("remove_from_cart",
                        "Xoa mon khoi gio hang.",
                        Map.of(
                                "item_id", Map.of("type", "string", "description", "Ma mon"),
                                "size", Map.of("type", "string", "description", "Size M hoac L"))),

                // 6. Sửa số lượng
                tool("update_cart_item",
                        "Sua so luong mon trong gio hang.",
                        Map.of(
                                "item_id", Map.of("type", "string", "description", "Ma mon"),
                                "size", Map.of("type", "string", "description", "Size M hoac L"),
                                "quantity", Map.of("type", "integer", "description", "So luong moi"))),

                // 7. Món bán chạy
                tool("get_best_sellers",
                        "Xem mon ban chay nhat.",
                        Map.of("category", Map.of("type", "string",
                                "description", "Danh muc (tuy chon)"))),

                // 8. Checkout — yêu cầu đủ thông tin giao hàng
                tool("checkout",
                        "Chot don hang va tao link thanh toan QR payOS. "
                                + "CAN: ten nguoi nhan, SDT, dia chi giao hang.",
                        Map.of(
                                "name", Map.of("type", "string",
                                        "description", "Ten nguoi nhan"),
                                "phone", Map.of("type", "string",
                                        "description", "So dien thoai nguoi nhan"),
                                "address", Map.of("type", "string",
                                        "description", "Dia chi giao hang"),
                                "note", Map.of("type", "string",
                                        "description", "Ghi chu don hang (tuy chon)"))),

                // 9. Gợi ý theo sở thích
                tool("recommend",
                        "Goi y do uong theo so thich. VD: 'socola', 'tra xanh', 'matxa'.",
                        Map.of("preference", Map.of("type", "string",
                                "description", "So thich hoac ban than (tuy chon)"))
                        )
        );
    }

    private Map<String, Object> tool(String name, String desc, Map<String, Object> params) {
        Map<String, Object> t = new HashMap<>();
        t.put("type", "function");
        Map<String, Object> fn = new HashMap<>();
        fn.put("name", name);
        fn.put("description", desc);
        Map<String, Object> paramObj = new HashMap<>();
        paramObj.put("type", "object");
        paramObj.put("properties", params);
        paramObj.put("required", params.keySet().stream().toList());
        fn.put("parameters", paramObj);
        t.put("function", fn);
        return t;
    }

    // ── Helpers ─────────────────────────────────────────────────

    private String extractText(JsonNode response) {
        try {
            return response.path("choices").get(0)
                    .path("message").path("content").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String errorMessage(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("timeout") || msg.contains("timed out"))
            return "Mạng hơi chậm con ơi, con nhắn lại giúp mẹ nha 😅";
        if (msg.contains("connection") || msg.contains("refused"))
            return "Mẹ gặp chút trục trặc kết nối, con chờ một lát rồi thử lại nhe 😅";
        if (msg.contains("rate limit") || msg.contains("429"))
            return "Mẹ đang bận lắc con ơi, con nhắn lại sau 1-2 phút nha 😅";
        if (msg.contains("401") || msg.contains("unauthorized") || msg.contains("api key"))
            return "Hệ thống đang bảo trì, con thử lại sau ít phút nha 😅";
        return "Mẹ đang bận xíu, con chờ một chút rồi nhắc lại nha 😅";
    }

    // ── Response ────────────────────────────────────────────────

    public record ChatResponse(String message, String paymentUrl, Long orderCode) {
        public ChatResponse(String message) {
            this(message, null, null);
        }
    }
}
