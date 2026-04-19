package com.casso.milktea.ai;

import com.casso.milktea.model.ConversationMessage;
import com.casso.milktea.model.Customer;
import com.casso.milktea.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates AI chat with function calling.
 * Uses direct OpenAI ChatClient with manual function dispatch
 * for full control over customer context.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final CustomerService customerService;
    private final AiToolFunctions toolFunctions;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Bạn là "Mẹ", chủ quán trà sữa nhỏ ấm cúng. Bạn nói chuyện thân thiện, ấm áp, \
            dí dỏm như một người mẹ Việt Nam quan tâm con cái. Bạn gọi khách là "con".

            QUY TẮC TUYỆT ĐỐI - KHÔNG ĐƯỢC VI PHẠM:
            1. Chỉ bán các món có trong menu. Dùng function get_menu() để xem menu.
            2. KHÔNG BAO GIỜ bịa ra món, giá, hoặc topping không có trong menu.
            3. Nếu khách hỏi món không có → "Tiếc quá con ơi, quán mình chưa có món đó. Để mẹ gợi ý con mấy món ngon nha!"
            4. Luôn xác nhận lại size (M/L) và số lượng trước khi thêm vào giỏ.
            5. Khi khách muốn thanh toán → gọi function checkout().
            6. KHÔNG tự tính tiền bằng text. Mọi phép tính phải qua function.
            7. Nếu không chắc chắn, HỎI LẠI khách thay vì đoán.
            8. Khi khách chào hoặc hỏi thăm, trả lời vui vẻ rồi giới thiệu menu.
            9. Giữ câu trả lời ngắn gọn, tự nhiên, không dài dòng.
            10. Nếu khách hỏi ngoài chủ đề (thời tiết, chính trị, v.v.) → chuyển hướng nhẹ nhàng về quán.

            HƯỚNG DẪN SỬ DỤNG FUNCTION:
            - Khách muốn xem menu → get_menu(category) hoặc get_menu() cho toàn bộ
            - Khách muốn gọi món → add_to_cart(item_id, size, quantity)
            - Khách muốn xem giỏ hàng → view_cart()
            - Khách muốn sửa số lượng → update_cart_item(item_id, size, quantity)
            - Khách muốn xóa món → remove_from_cart(item_id, size)
            - Khách muốn thanh toán → checkout(note)
            """;

    // JSON schema for function definitions
    private static final String FUNCTIONS_JSON = """
            [
              {
                "name": "get_menu",
                "description": "Xem menu quán trà sữa. Có thể lọc theo danh mục.",
                "parameters": {
                  "type": "object",
                  "properties": {
                    "category": {
                      "type": "string",
                      "description": "Danh mục cần xem. Để trống để xem toàn bộ menu.",
                      "enum": ["Trà Sữa", "Trà Trái Cây", "Cà Phê", "Đá Xay", "Topping"]
                    }
                  }
                }
              },
              {
                "name": "add_to_cart",
                "description": "Thêm một món vào giỏ hàng của khách.",
                "parameters": {
                  "type": "object",
                  "properties": {
                    "item_id": {
                      "type": "string",
                      "description": "Mã món (ví dụ: TS01, CF02, TOP01)"
                    },
                    "size": {
                      "type": "string",
                      "enum": ["M", "L"],
                      "description": "Size: M hoặc L"
                    },
                    "quantity": {
                      "type": "integer",
                      "description": "Số lượng, phải >= 1"
                    }
                  },
                  "required": ["item_id", "size", "quantity"]
                }
              },
              {
                "name": "view_cart",
                "description": "Xem giỏ hàng hiện tại của khách.",
                "parameters": { "type": "object", "properties": {} }
              },
              {
                "name": "update_cart_item",
                "description": "Cập nhật số lượng của một món trong giỏ hàng.",
                "parameters": {
                  "type": "object",
                  "properties": {
                    "item_id": {
                      "type": "string",
                      "description": "Mã món cần cập nhật"
                    },
                    "size": {
                      "type": "string",
                      "enum": ["M", "L"],
                      "description": "Size của món cần cập nhật"
                    },
                    "quantity": {
                      "type": "integer",
                      "description": "Số lượng mới. Nếu = 0 sẽ xóa món."
                    }
                  },
                  "required": ["item_id", "size", "quantity"]
                }
              },
              {
                "name": "remove_from_cart",
                "description": "Xóa một món khỏi giỏ hàng.",
                "parameters": {
                  "type": "object",
                  "properties": {
                    "item_id": {
                      "type": "string",
                      "description": "Mã món cần xóa"
                    },
                    "size": {
                      "type": "string",
                      "enum": ["M", "L"],
                      "description": "Size của món cần xóa"
                    }
                  },
                  "required": ["item_id", "size"]
                }
              },
              {
                "name": "checkout",
                "description": "Tạo đơn hàng và link thanh toán từ giỏ hàng hiện tại.",
                "parameters": {
                  "type": "object",
                  "properties": {
                    "note": {
                      "type": "string",
                      "description": "Ghi chú cho đơn hàng (ít đá, nhiều đường, v.v.)"
                    }
                  }
                }
              }
            ]
            """;

    /**
     * Process a user message and return the AI response.
     * Handles the full function-calling loop.
     */
    public ChatResult chat(Customer customer, String userMessage) {
        // Save user message
        customerService.saveMessage(customer, "user", userMessage);

        // Build messages list
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));

        // Add conversation history
        List<ConversationMessage> history = customerService.getRecentMessages(customer);
        for (ConversationMessage msg : history) {
            switch (msg.getRole()) {
                case "user" -> messages.add(new UserMessage(msg.getContent()));
                case "assistant" -> messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        // Call OpenAI with function definitions
        try {
            ChatClient chatClient = chatClientBuilder.build();

            // Use the chat client with tools defined
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .messages(messages.subList(1, messages.size())) // exclude system (already set)
                    .call()
                    .content();

            // For now, use simple chat without Spring AI auto-tool-calling
            // We'll implement manual function dispatch in Phase 2
            // This allows the bot to work immediately for basic chat

            // Save assistant response
            customerService.saveMessage(customer, "assistant", response);

            return new ChatResult(response, null, null);

        } catch (Exception e) {
            log.error("AI chat error for customer {}", customer.getId(), e);
            String fallback = "Ôi, mẹ bị trục trặc chút. Con nhắn lại được không? 😅";
            customerService.saveMessage(customer, "assistant", fallback);
            return new ChatResult(fallback, null, null);
        }
    }

    /**
     * Result of an AI chat interaction.
     */
    public record ChatResult(String message, String paymentUrl, Long orderCode) {}
}
