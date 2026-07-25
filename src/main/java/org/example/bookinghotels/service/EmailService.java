package org.example.bookinghotels.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendBookingConfirmation(String toEmail, String customerName, String roomName, double amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("◈ FEELHOME HOTEL - XÁC NHẬN THANH TOÁN THÀNH CÔNG");

            // Thiết kế nội dung Mail dạng HTML chuyên nghiệp
            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #eaeaea; padding: 20px;'>"
                    + "<h2 style='color: #c5a880; border-bottom: 2px solid #111; padding-bottom: 10px; text-align: center;'>CẢM ƠN BẠN ĐÃ ĐẶT PHÒNG TẠI FEELHOME</h2>"
                    + "<p>Xin chào <strong>" + customerName + "</strong>,</p>"
                    + "<p>Hệ thống tài chính FeelHome Hotel đã ghi nhận khoản thanh toán chuyển khoản của bạn thành công.</p>"
                    + "<div style='background-color: #f8f6f2; padding: 15px; border-left: 4px solid #c5a880; margin: 20px 0;'>"
                    + "<p style='margin: 5px 0;'><strong>Loại phòng:</strong> " + roomName + "</p>"
                    + "<p style='margin: 5px 0;'><strong>Trạng thái:</strong> <span style='color: green; fw-bold;'>Đã thanh toán thành công</span></p>"
                    + "<p style='margin: 5px 0;'><strong>Tổng số tiền:</strong> <strong style='color: #d93838;'>" + String.format("%,.0f", amount) + " ₫</strong></p>"
                    + "</div>"
                    + "<p>Mã phòng và hướng dẫn nhận phòng chi tiết sẽ được gửi trước giờ check-in 2 tiếng. Chúc bạn có một kỳ nghỉ tuyệt vời!</p>"
                    + "<hr style='border: 0; border-top: 1px solid #eaeaea; margin: 20px 0;' />"
                    + "<p style='font-size: 0.85rem; color: #777; text-align: center;'>&copy; 2026 FeelHome Hotel. Hệ thống nghỉ dưỡng cao cấp.</p>"
                    + "</div>";

            helper.setText(htmlContent, true); // true nghĩa là gửi dưới dạng HTML

            mailSender.send(message);
            System.out.println("📬 Đã gửi email xác nhận thành công tới: " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi gửi email: " + e.getMessage());
        }
    }

    public void sendStayNotification(String toEmail, String customerName, String subject, String body,
                                     String roomNumber, java.time.LocalDate checkinDate, java.time.LocalDate checkoutDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("◈ FEELHOME HOTEL - " + subject);
            String htmlContent = "<div style='font-family:Arial,sans-serif;max-width:620px;margin:auto;border:1px solid #e5e7eb;padding:24px'>"
                    + "<h2 style='text-align:center;color:#b18a55'>FEELHOME HOTEL</h2>"
                    + "<p>Xin chào <strong>" + customerName + "</strong>,</p>"
                    + "<p>" + body + "</p>"
                    + "<div style='background:#faf7f2;padding:16px;border-left:4px solid #c5a880'>"
                    + "<p><strong>Phòng:</strong> " + roomNumber + "</p>"
                    + "<p><strong>Nhận phòng:</strong> " + checkinDate + "</p>"
                    + "<p><strong>Trả phòng:</strong> " + checkoutDate + "</p>"
                    + "</div><p style='color:#777;font-size:13px'>Email được gửi tự động từ hệ thống FeelHome.</p></div>";
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Không gửi được email lưu trú: " + e.getMessage());
        }
    }
}
