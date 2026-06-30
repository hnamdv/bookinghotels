package org.example.bookinghotels.service;

import jakarta.mail.internet.MimeMessage;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.BookingFB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // --- HÀM 1: Nhận 4 tham số để cứu nguy cho BankWebhookController không bị lỗi compile ---
    @Async
    public void sendBookingConfirmation(String toEmail, String customerName, String roomName, double amount) {
        // Gọi lại hàm 5 tham số và truyền tham số cuối cùng là null
        sendBookingConfirmation(toEmail, customerName, roomName, amount, null);
    }

    // --- HÀM 2: Nhận đầy đủ 5 tham số để in hóa đơn kèm món ăn dịch vụ ---
    @Async
    public void sendBookingConfirmation(String toEmail, String customerName, String roomName, double amount, BookingDetail bookingDetail) {
        try {
            if (toEmail == null || toEmail.trim().isEmpty()) {
                System.err.println("⚠️ Không thể gửi email do địa chỉ email trống!");
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("◈ FEELHOME HOTEL - XÁC NHẬN THANH TOÁN THÀNH CÔNG");

            // Tạo bảng dịch vụ đi kèm động dạng HTML nếu khách hàng có chọn mua thêm
            StringBuilder serviceRowsHtml = new StringBuilder();
            if (bookingDetail != null && bookingDetail.getBookingFBs() != null && !bookingDetail.getBookingFBs().isEmpty()) {
                serviceRowsHtml.append("<h4 style='color: #111; margin-top: 20px; margin-bottom: 8px; font-family: Arial, sans-serif;'>DỊCH VỤ & THỰC ĐƠN ĐẶT KÈM:</h4>");
                serviceRowsHtml.append("<table style='width:100%; border-collapse: collapse; font-size: 13px; font-family: Arial, sans-serif; margin-bottom: 15px;'>");
                serviceRowsHtml.append("<tr style='background-color: #f1eae0; text-align: left;'><th style='padding: 8px; border: 1px solid #ddd;'>Tên dịch vụ</th><th style='padding: 8px; border: 1px solid #ddd; text-align: center;'>Số lượng</th><th style='padding: 8px; border: 1px solid #ddd; text-align: right;'>Đơn giá</th></tr>");

                for (BookingFB fb : bookingDetail.getBookingFBs()) {
                    String serviceName = (fb.getFwb() != null) ? fb.getFwb().getName() : "Dịch vụ phòng";
                    serviceRowsHtml.append(String.format(
                            "<tr><td style='padding: 8px; border: 1px solid #ddd;'>%s</td><td style='padding: 8px; border: 1px solid #ddd; text-align: center;'>%d</td><td style='padding: 8px; border: 1px solid #ddd; text-align: right;'>%,.0f ₫</td></tr>",
                            serviceName, fb.getQuantity(), fb.getPriceAtOrder()
                    ));
                }
                serviceRowsHtml.append("</table>");
            }

            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #eaeaea; padding: 20px;'>"
                    + "<h2 style='color: #c5a880; border-bottom: 2px solid #111; padding-bottom: 10px; text-align: center;'>CẢM ƠN BẠN ĐÃ ĐẶT PHÒNG TẠI FEELHOME</h2>"
                    + "<p>Xin chào <strong>" + customerName + "</strong>,</p>"
                    + "<p>Hệ thống tài chính FeelHome Hotel đã ghi nhận khoản thanh toán chuyển khoản của bạn thành công.</p>"
                    + "<div style='background-color: #f8f6f2; padding: 15px; border-left: 4px solid #c5a880; margin: 20px 0;'>"
                    + "<p style='margin: 5px 0;'><strong>Loại phòng nghỉ:</strong> " + roomName + "</p>"
                    + "<p style='margin: 5px 0;'><strong>Trạng thái giao dịch:</strong> <span style='color: green; font-weight: bold;'>Đã quyết toán thành công</span></p>"

                    + serviceRowsHtml.toString() // Nhúng danh sách dịch vụ nếu có

                    + "<p style='margin: 15px 0 5px 0; border-top: 1px dashed #ccc; padding-top: 10px;'><strong>Tổng số tiền quyết toán đơn:</strong> <strong style='color: #d93838; font-size: 16px;'>" + String.format("%,.0f", amount) + " ₫</strong></p>"
                    + "</div>"
                    + "<p style='line-height: 1.6;'>Quý khách vui lòng xuất trình thông tin xác nhận này tại quầy lễ tân khi đến nhận phòng.</p>"
                    + "<p style='line-height: 1.6; font-weight: bold;'>Chúc bạn có một kỳ nghỉ tuyệt vời tại FeelHome Hotel!</p>"
                    + "<hr style='border: 0; border-top: 1px solid #eaeaea; margin: 20px 0;' />"
                    + "<p style='font-size: 0.85rem; color: #777; text-align: center;'>&copy; 2026 FeelHome Hotel. Hệ thống nghỉ dưỡng cao cấp.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("📬 Đã gửi email xác nhận thành công tới: " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi gửi email: " + e.getMessage());
            e.printStackTrace();
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
