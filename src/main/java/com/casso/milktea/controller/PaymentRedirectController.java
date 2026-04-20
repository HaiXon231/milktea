package com.casso.milktea.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment")
public class PaymentRedirectController {

    @GetMapping(value = "/success", produces = "text/html;charset=UTF-8")
    public String paymentSuccess() {
        return """
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Thanh toán thành công</title>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; text-align: center; padding: 50px; background-color: #f0fdf4; color: #166534; }
                        .container { background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); max-width: 500px; margin: 0 auto; }
                        h1 { color: #15803d; }
                        p { font-size: 18px; margin-top: 20px; }
                        .btn { display: inline-block; margin-top: 30px; padding: 10px 20px; background-color: #16a34a; color: white; text-decoration: none; border-radius: 6px; font-weight: bold; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>✅ Thanh toán thành công!</h1>
                        <p>Mẹ đã nhận được tiền của con rồi nha.</p>
                        <p>Con có thể đóng trang này và quay lại Telegram để chat tiếp với mẹ nhé! 🧋</p>
                        <a href="https://t.me/home_milktea_bot" class="btn">Quay lại Telegram</a>
                    </div>
                </body>
                </html>
                """;
    }

    @GetMapping(value = "/cancel", produces = "text/html;charset=UTF-8")
    public String paymentCancel() {
        return """
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Hủy thanh toán</title>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; text-align: center; padding: 50px; background-color: #fff1f2; color: #9f1239; }
                        .container { background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); max-width: 500px; margin: 0 auto; }
                        h1 { color: #be123c; }
                        p { font-size: 18px; margin-top: 20px; }
                        .btn { display: inline-block; margin-top: 30px; padding: 10px 20px; background-color: #e11d48; color: white; text-decoration: none; border-radius: 6px; font-weight: bold; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>❌ Đã hủy thanh toán</h1>
                        <p>Con chưa thanh toán đơn hàng này.</p>
                        <p>Quay lại Telegram để đặt lại khi nào con muốn nha! 🧋</p>
                        <a href="https://t.me/home_milktea_bot" class="btn">Quay lại Telegram</a>
                    </div>
                </body>
                </html>
                """;
    }
}
