package org.example.bookinghotels.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Gửi email thất bại: " + e.getMessage());
        }
    }
    @Async
    public void sendPasswordResetEmail(String to, String resetToken) {
        String subject = "Khôi phục mật khẩu - FeelHome Hotel";
        // Link này trỏ đến trang đổi mật khẩu của bạn
        String resetUrl = "http://localhost:8080/reset-password?token=" + resetToken;
        String text = "Chào bạn,\n\nBạn đã yêu cầu khôi phục mật khẩu. Vui lòng nhấn vào link bên dưới để đặt lại mật khẩu:\n"
                + resetUrl + "\n\nLink sẽ hết hạn sau 15 phút.\nCảm ơn!";

        sendEmail(to, subject, text);
    }
}
