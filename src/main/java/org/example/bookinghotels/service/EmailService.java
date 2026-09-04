package org.example.bookinghotels.service;

import jakarta.mail.internet.MimeMessage;
import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.repository.HotelsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {
    @Autowired
    private HotelsRepository  hotelsRepository;
    @Autowired
    private JavaMailSender mailSender;
    private Hotels ifHotels() {
        try {
            List<Hotels> hotels = hotelsRepository.findAllHotels();
            if (hotels != null && !hotels.isEmpty()) {
                return hotels.get(0); // Lấy hotel đầu tiên
            }
            return null;
        } catch (Exception e) {
            System.err.println("❌ Lỗi lấy thông tin hotel: " + e.getMessage());
            return null;
        }
    }

    // =========================================================
    // EMAIL XÁC NHẬN THANH TOÁN
    // =========================================================
    public void sendBookingConfirmation(String toEmail, String customerName, String roomName, double amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("◈ FEELHOME HOTEL - XÁC NHẬN THANH TOÁN THÀNH CÔNG");

            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #eaeaea; padding: 20px;'>"
                    + "<h2 style='color: #c5a880; border-bottom: 2px solid #111; padding-bottom: 10px; text-align: center;'>CẢM ƠN BẠN ĐÃ ĐẶT PHÒNG TẠI FEELHOME</h2>"
                    + "<p>Xin chào <strong>" + customerName + "</strong>,</p>"
                    + "<p>Hệ thống tài chính FeelHome Hotel đã ghi nhận khoản thanh toán chuyển khoản của bạn thành công.</p>"
                    + "<div style='background-color: #f8f6f2; padding: 15px; border-left: 4px solid #c5a880; margin: 20px 0;'>"
                    + "<p style='margin: 5px 0;'><strong>Loại phòng:</strong> " + roomName + "</p>"
                    + "<p style='margin: 5px 0;'><strong>Trạng thái:</strong> <span style='color: green; font-weight: bold;'>Đã thanh toán thành công</span></p>"
                    + "<p style='margin: 5px 0;'><strong>Tổng số tiền:</strong> <strong style='color: #d93838;'>" + String.format("%,.0f", amount) + " ₫</strong></p>"
                    + "</div>"
                    + "<p>Mã phòng và hướng dẫn nhận phòng chi tiết sẽ được gửi trước giờ check-in 2 tiếng. Chúc bạn có một kỳ nghỉ tuyệt vời!</p>"
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

    // =========================================================
    // EMAIL THÔNG BÁO LƯU TRÚ
    // =========================================================
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
            System.out.println("📬 Đã gửi email lưu trú tới: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Không gửi được email lưu trú: " + e.getMessage());
        }
    }

    // =========================================================
    // EMAIL KHI TRẠNG THÁI BOOKING THAY ĐỔI
    // =========================================================
    public void sendStatusChangeNotification(BookingDetail detail, String newStatus) {
        try {
            Booking booking = detail.getBooking();
            if (booking == null || booking.getEmail() == null || booking.getEmail().isBlank()) {
                System.out.println("⚠️ Không có email khách hàng, bỏ qua gửi email");
                return;
            }

            String toEmail = booking.getEmail();
            String customerName = booking.getName();
            String roomNumber = detail.getRoom() != null ? detail.getRoom().getRoomNumber() : "Chưa gán";
            String roomTypeName = detail.getRoomType() != null ? detail.getRoomType().getNameType() : "";
            java.time.LocalDate checkinDate = booking.getCheckinDate();
            java.time.LocalDate checkoutDate = booking.getCheckoutDate();
            double price = detail.getPrice() != null ? detail.getPrice() : 0;

            switch (newStatus) {
                case "APPROVED":
                    sendApprovedEmail(toEmail, customerName, roomNumber, roomTypeName, checkinDate, checkoutDate, price);
                    break;
                case "PAID":
                    sendPaidEmail(toEmail, customerName, roomNumber, roomTypeName, checkinDate, checkoutDate, price);
                    break;
                case "CHECKED_OUT":
                    sendCheckedOutEmail(toEmail, customerName, roomNumber, roomTypeName, checkinDate, checkoutDate, price);
                    break;
                case "CHECKED_IN":
                    sendCheckedInEmail(toEmail, customerName, roomNumber, roomTypeName, checkinDate, checkoutDate, price);
                    break;
                case "CANCELLED":
                    sendCancelledEmail(toEmail, customerName, roomNumber, roomTypeName, checkinDate, checkoutDate, price);
                    break;
                default:
                    System.out.println("⚠️ Trạng thái không cần gửi email: " + newStatus);
                    return;
            }

            System.out.println("✅ Đã gửi email thông báo [" + newStatus + "] cho: " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Lỗi gửi email trạng thái: " + e.getMessage());
        }
    }

    // =========================================================
    // EMAIL: XÁC NHẬN ĐẶT PHÒNG (APPROVED)
    // =========================================================

    private void sendApprovedEmail(String toEmail, String customerName, String roomNumber,
                                   String roomTypeName, java.time.LocalDate checkinDate,
                                   java.time.LocalDate checkoutDate, double price) {
        String subject = "Xác nhận đặt phòng thành công";
        String body = "Đơn đặt phòng của bạn đã được <strong style='color:#1E6CD4;'>XÁC NHẬN</strong>. "
                + "Cảm ơn bạn đã tin tưởng lựa chọn ! "+ ifHotels().getName()+"<br>"
                + "Vui lòng hoàn tất thanh toán trước khi nhận phòng.";
        sendStatusEmail(toEmail, subject, customerName, body, roomNumber, roomTypeName,
                checkinDate, checkoutDate, price, "#1E6CD4", "ĐÃ XÁC NHẬN");
    }

    // =========================================================
    // EMAIL: XÁC NHẬN THANH TOÁN (PAID)
    // =========================================================

    private void sendPaidEmail(String toEmail, String customerName, String roomNumber,
                               String roomTypeName, java.time.LocalDate checkinDate,
                               java.time.LocalDate checkoutDate, double price) {
        String subject = "Xác nhận thanh toán thành công";
        String body = "Chúng tôi đã nhận được thanh toán cho đơn đặt phòng của bạn. "
                + "Cảm ơn bạn đã hoàn tất thanh toán!<br>"
                + "Mã phòng và hướng dẫn nhận phòng sẽ được gửi trước giờ check-in.";
        sendStatusEmail(toEmail, subject, customerName, body, roomNumber, roomTypeName,
                checkinDate, checkoutDate, price, "#059669", "ĐÃ THANH TOÁN");
    }
    // =========================================================
    // EMAIL: Nhận phòng (CHECKED_IN)
    // =========================================================

    private void sendCheckedInEmail(String toEmail, String customerName, String roomNumber,
                                     String roomTypeName, java.time.LocalDate checkinDate,
                                     java.time.LocalDate checkoutDate, double price) {
        String subject = "Xin Chào quý khách đã đến với khách sạn " + ifHotels().getName();
        String body = "Cảm ơn bạn đã lưu trú tại "+ ifHotels().getName()
                + "Chúng tôi hy vọng bạn có một kỳ nghỉ tuyệt vời.<br>"
                + "Chúc bạn trải nghiệm dịch vụ thật vui vẻ!";
        sendStatusEmail(toEmail, subject, customerName, body, roomNumber, roomTypeName,
                checkinDate, checkoutDate, price, "#8B5CF6", "ĐÃ NHẬN PHÒNG");
    }

    // =========================================================
    // EMAIL: CẢM ƠN SAU KHI TRẢ PHÒNG (CHECKED_OUT)
    // =========================================================

    private void sendCheckedOutEmail(String toEmail, String customerName, String roomNumber,
                                     String roomTypeName, java.time.LocalDate checkinDate,
                                     java.time.LocalDate checkoutDate, double price) {
        String subject = "Cảm ơn bạn đã lưu trú tại "+ ifHotels().getName();
        String body = "Cảm ơn bạn đã lưu trú tại "+ ifHotels().getName()
                + "Chúng tôi hy vọng bạn có một kỳ nghỉ tuyệt vời.<br>"
                + "Hẹn gặp lại bạn trong những lần sau!";
        sendStatusEmail(toEmail, subject, customerName, body, roomNumber, roomTypeName,
                checkinDate, checkoutDate, price, "#8B5CF6", "ĐÃ TRẢ PHÒNG");
    }

    // =========================================================
    // EMAIL: THÔNG BÁO HỦY (CANCELLED)
    // =========================================================

    private void sendCancelledEmail(String toEmail, String customerName, String roomNumber,
                                    String roomTypeName, java.time.LocalDate checkinDate,
                                    java.time.LocalDate checkoutDate, double price) {
        String subject = "Đơn đặt phòng đã bị hủy";
        String body = "Đơn đặt phòng của bạn đã bị <strong style='color:#EF4444;'>HỦY</strong>. "
                + "Nếu có bất kỳ thắc mắc nào, vui lòng liên hệ với chúng tôi qua hotline: "+ ifHotels().getName();
        sendStatusEmail(toEmail, subject, customerName, body, roomNumber, roomTypeName,
                checkinDate, checkoutDate, price, "#EF4444", "ĐÃ HỦY");
    }

    // =========================================================
    // PHƯƠNG THỨC GỬI EMAIL CHUNG
    // =========================================================
    private void sendStatusEmail(String toEmail, String subject, String customerName, String body,
                                 String roomNumber, String roomTypeName,
                                 java.time.LocalDate checkinDate, java.time.LocalDate checkoutDate,
                                 double price, String statusColor, String statusText) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("◈ "+ ifHotels().getName()+" - " + subject);

            String htmlContent = buildEmailTemplate(customerName, body, roomNumber, roomTypeName,
                    checkinDate, checkoutDate, price, statusColor, statusText);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi gửi email: " + e.getMessage());
        }
    }

    // =========================================================
    // TEMPLATE EMAIL ĐẸP
    // =========================================================
    private String buildEmailTemplate(String customerName, String body,
                                      String roomNumber, String roomTypeName,
                                      java.time.LocalDate checkinDate, java.time.LocalDate checkoutDate,
                                      double price, String statusColor, String statusText) {

        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Arial,sans-serif;margin:0;padding:0;background:#F5F7FC'>"
                + "<div style='max-width:620px;margin:20px auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08)'>"

                // Header
                + "<div style='background:#1E2A3E;padding:30px 24px;text-align:center'>"
                + "<h1 style='color:#C5A880;margin:0;font-size:28px;letter-spacing:2px'> " + ifHotels().getName() +"</h1>"
                + "<p style='color:#94A3B8;margin:8px 0 0;font-size:13px'>Kỳ nghỉ hoàn hảo bắt đầu từ đây</p>"
                + "</div>"

                // Content
                + "<div style='padding:24px'>"
                + "<p style='margin:0 0 16px;font-size:16px'>Xin chào <strong>" + customerName + "</strong>,</p>"
                + "<p style='margin:0 0 16px;font-size:14px;line-height:1.6'>" + body + "</p>"

                // Status badge
                + "<div style='text-align:center;margin:20px 0'>"
                + "<span style='display:inline-block;background:" + statusColor + ";color:#fff;padding:8px 20px;border-radius:20px;font-size:14px;font-weight:bold'>" + statusText + "</span>"
                + "</div>"


                + "<div style='background:#FAF7F2;padding:20px;border-radius:12px;border-left:4px solid #C5A880;margin:20px 0'>"
                + "<h3 style='margin:0 0 12px;color:#1E2A3E;font-size:16px'>📋 Thông tin đặt phòng</h3>"
                + "<table style='width:100%;font-size:14px'>"
                + "<tr><td style='padding:6px 0;color:#6B7280'>Phòng:</td><td style='padding:6px 0;font-weight:bold'>" + roomNumber + "</td></tr>"
                + "<tr><td style='padding:6px 0;color:#6B7280'>Loại phòng:</td><td style='padding:6px 0;font-weight:bold'>" + roomTypeName + "</td></tr>"
                + "<tr><td style='padding:6px 0;color:#6B7280'>Nhận phòng:</td><td style='padding:6px 0;font-weight:bold'>" + checkinDate + "</td></tr>"
                + "<tr><td style='padding:6px 0;color:#6B7280'>Trả phòng:</td><td style='padding:6px 0;font-weight:bold'>" + checkoutDate + "</td></tr>"
                + "<tr><td style='padding:6px 0;color:#6B7280'>Tổng tiền:</td><td style='padding:6px 0;font-weight:bold;color:#D97706'>" + String.format("%,.0f", price) + " VND</td></tr>"
                + "</table>"
                + "</div>"
                + "</div>"


                + "<div style='background:#F5F7FC;padding:16px;text-align:center'>"
                + "<p style='margin:0;font-size:12px;color:#6B7280'>"+ ifHotels().getName()+" • Hotline: </p>"+ ifHotels().getName()
                + "</div>"

                + "</div>"
                + "</body>"
                + "</html>";
    }
}