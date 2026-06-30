package org.example.bookinghotels.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async; // Đã thêm import Async
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async // <--- QUAN TRỌNG: Đẩy tiến trình này chạy ngầm riêng, giúp giải phóng luồng chính ngay lập tức
    public void sendBookingConfirmation(String toEmail, String customerName, String roomName, double amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("◈ FEELHOME HOTEL - XÁC NHẬN THANH TOÁN THÀNH CÔNG");

            // Thiết kế nội dung Mail dạng HTML chuyên nghiệp và đồng bộ quy trình đón tiếp tại quầy
            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #eaeaea; padding: 20px;'>"
                    + "<h2 style='color: #c5a880; border-bottom: 2px solid #111; padding-bottom: 10px; text-align: center;'>CẢM ƠN BẠN ĐÃ ĐẶT PHÒNG TẠI FEELHOME</h2>"
                    + "<p>Xin chào <strong>" + customerName + "</strong>,</p>"
                    + "<p>Hệ thống tài chính FeelHome Hotel đã ghi nhận khoản thanh toán chuyển khoản của bạn thành công.</p>"
                    + "<div style='background-color: #f8f6f2; padding: 15px; border-left: 4px solid #c5a880; margin: 20px 0;'>"
                    + "<p style='margin: 5px 0;'><strong>Loại phòng:</strong> " + roomName + "</p>"
                    + "<p style='margin: 5px 0;'><strong>Trạng thái:</strong> <span style='color: green; font-weight: bold;'>Đã thanh toán thành công</span></p>"
                    + "<p style='margin: 5px 0;'><strong>Tổng số tiền:</strong> <strong style='color: #d93838;'>" + String.format("%,.0f", amount) + " ₫</strong></p>"
                    + "</div>"
                    + "<p style='line-height: 1.6;'>Quý khách vui lòng xuất trình thông tin xác nhận này tại quầy lễ tân khi đến nhận phòng. "
                    + "Nhân viên của FeelHome sẽ trực tiếp trao đổi, hỗ trợ hoàn tất thủ tục check-in và bàn giao phòng cho quý khách.</p>"
                    + "<p style='line-height: 1.6; font-weight: bold;'>Chúc bạn có một kỳ nghỉ tuyệt vời tại FeelHome Hotel!</p>"
                    + "<hr style='border: 0; border-top: 1px solid #eaeaea; margin: 20px 0;' />"
                    + "<p style='font-size: 0.85rem; color: #777; text-align: center;'>&copy; 2026 FeelHome Hotel. Hệ thống nghỉ dưỡng cao cấp.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("📬 Đã gửi email xác nhận thành công tới: " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi gửi email: " + e.getMessage());
        }
    }
}