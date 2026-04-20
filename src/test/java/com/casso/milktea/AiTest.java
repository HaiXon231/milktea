package com.casso.milktea;
import com.casso.milktea.ai.AiChatService;
import com.casso.milktea.model.Customer;
import com.casso.milktea.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AiTest {
    @Autowired AiChatService aiChatService;
    @Autowired CustomerService customerService;

    @Test
    public void testChat() {
        try {
            Customer customer = customerService.getOrCreateCustomer(999L, "Tester");
            AiChatService.ChatResult res = aiChatService.chat(customer, "cho xem giỏ hàng");
            System.out.println("AI RESPONSE: " + res.message());
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
