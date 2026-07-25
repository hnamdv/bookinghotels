package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.service.OrderBookingService;
import org.example.bookinghotels.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class BankWebhookController {

    @Autowired
    private OrderBookingService bookingService;

    @Autowired
    private EmailService emailService;

    // --- THÊM HÀM NÀY ĐỂ FIX LỖI PING THỬ CỦA SEPAY ---
    @GetMapping("/handler")
    public ResponseEntity<String> verifyWebhook() {
        System.out.println("-> Nhận lệnh ping kiểm tra kết nối từ SePay.");
        return ResponseEntity.ok("Webhook is active!");
    }

    @PostMapping("/handler")
    public ResponseEntity<?> handleBankNotification(@RequestBody Map<String, Object> requestBody) {
        try {
            String content = (String) requestBody.get("content");

            // Nếu SePay gửi content trong gateway_data
            if (content == null && requestBody.containsKey("gateway_data")) {
                Map<String, Object> gatewayData = (Map<String, Object>) requestBody.get("gateway_data");
                content = (String) gatewayData.get("content");
            }

            // Backup nếu content nằm ở description
            if (content == null) {
                content = (String) requestBody.get("description");
            }

            System.out.println("-> Nhận tín hiệu Webhook Ngân hàng. Nội dung: " + content);

            if (content != null && content.toUpperCase().contains("FEELHOMEBK")) {

                // Gửi nguyên content cho service xử lý
                bookingService.updateStatusToPaid(content);

                System.out.println("✅ Xác nhận thanh toán thành công cho hóa đơn.");

                Booking booking = bookingService.getBookingById(content);

                if (booking != null) {
                    String roomName =
                            (booking.getBookingDetails() != null && !booking.getBookingDetails().isEmpty())
                                    ? booking.getBookingDetails().get(0).getRoomType().getNameType()
                                    : "Phòng nghỉ FeelHome";

                    double totalAmount =
                            (booking.getBookingDetails() != null && !booking.getBookingDetails().isEmpty())
                                    ? booking.getBookingDetails().get(0).getPrice()
                                    : 0.0;

                    emailService.sendBookingConfirmation(
                            booking.getEmail(),
                            booking.getName(),
                            roomName,
                            totalAmount
                    );
                } else {
                    System.err.println("⚠ Không tìm thấy Booking để gửi mail.");
                }

                return ResponseEntity.ok(
                        Map.of(
                                "success", true,
                                "message", "Đã xác nhận thanh toán và gửi mail thành công"
                        )
                );
            }

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Nội dung chuyển khoản không hợp lệ"
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }
}