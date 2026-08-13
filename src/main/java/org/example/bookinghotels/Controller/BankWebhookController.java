package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.example.bookinghotels.service.OrderBookingService;
import org.example.bookinghotels.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/webhook")
public class BankWebhookController {

    @Autowired
    private OrderBookingService bookingService;

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private EmailService emailService;

    @GetMapping("/handler")
    public ResponseEntity<String> verifyWebhook() {
        return ResponseEntity.ok("Webhook is active!");
    }

    @PostMapping("/handler")
    public ResponseEntity<?> handleBankNotification(@RequestBody Map<String, Object> requestBody) {
        try {
            String content = (String) requestBody.get("content");
            if (content == null && requestBody.containsKey("gateway_data")) {
                content = (String) ((Map<String, Object>) requestBody.get("gateway_data")).get("content");
            }
            if (content == null) content = (String) requestBody.get("description");

            System.out.println("-> Nhận Webhook: " + content);

            if (content != null && content.toUpperCase().contains("FEELHOMEBK")) {

                // ===== DÙNG REGEX TRÍCH XUẤT ĐÚNG ID SAU CHỮ FEELHOMEBK =====
                Integer bookingId = null;
                Pattern pattern = Pattern.compile("FEELHOMEBK\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    bookingId = Integer.parseInt(matcher.group(1));
                }

                if (bookingId == null) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không tìm thấy mã Booking hợp lệ"));
                }

                System.out.println("-> Tách thành công Booking ID: " + bookingId);

                // 1. Cập nhật trạng thái Booking chính
                bookingService.updateStatusToPaid(content);

                // 2. Cập nhật TẤT CẢ BookingDetail liên quan thành PAID
                List<BookingDetail> details = bookingDetailRepository.findByBookingId(bookingId);
                if (details != null && !details.isEmpty()) {
                    for (BookingDetail detail : details) {
                        detail.setStatus("PAID");
                    }
                    bookingDetailRepository.saveAll(details);
                    System.out.println("✅ Đã cập nhật trạng thái PAID cho " + details.size() + " BookingDetail.");
                }

                // 3. Gửi mail xác nhận
                Booking booking = bookingService.getBookingById(content);
                if (booking != null) {
                    String roomName = (booking.getBookingDetails() != null && !booking.getBookingDetails().isEmpty())
                            ? booking.getBookingDetails().get(0).getRoomType().getNameType() : "Phòng nghỉ FeelHome";
                    double totalAmount = (booking.getBookingDetails() != null && !booking.getBookingDetails().isEmpty())
                            ? booking.getBookingDetails().get(0).getPrice() : 0.0;

                    emailService.sendBookingConfirmation(booking.getEmail(), booking.getName(), roomName, totalAmount);
                }

                return ResponseEntity.ok(Map.of("success", true, "message", "Đã xác nhận thanh toán thành công"));
            }

            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Nội dung chuyển khoản không hợp lệ"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}