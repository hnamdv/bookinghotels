package org.example.bookinghotels.Controller;

import org.example.bookinghotels.service.OrderBookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class BankWebhookController {

    @Autowired
    private OrderBookingService bookingService;

    // --- KIỂM TRA ĐƯỜNG TRUYỀN PING TỰ ĐỘNG TỪ SEPAY ---
    @GetMapping("/handler")
    public ResponseEntity<String> verifyWebhook() {
        System.out.println("-> Nhận lệnh ping kiểm tra kết nối từ SePay.");
        return ResponseEntity.ok("Webhook is active!");
    }

    @PostMapping("/handler")
    public ResponseEntity<?> handleBankNotification(@RequestBody Map<String, Object> requestBody) {
        try {
            String content = (String) requestBody.get("content");

            // Nếu SePay đóng gói cấu trúc content nằm sâu trong gateway_data
            if (content == null && requestBody.containsKey("gateway_data")) {
                Map<String, Object> gatewayData = (Map<String, Object>) requestBody.get("gateway_data");
                content = (String) gatewayData.get("content");
            }

            // Phương án dự phòng bóc tách nếu chuỗi nằm ở description
            if (content == null) {
                content = (String) requestBody.get("description");
            }

            System.out.println("-> Nhận tín hiệu Webhook Ngân hàng. Nội dung: " + content);

            if (content != null && content.toUpperCase().contains("FEELHOMEBK")) {

                // ĐÃ TỐI ƯU DỨT ĐIỂM:
                // Chỉ cần gọi duy nhất hàm này. Bản thân hàm updateStatusToPaid bên trong
                // OrderBookingServiceImpl đã bao gồm: Cập nhật hóa đơn PAID + Tự load Repo an toàn + Gửi mail hóa đơn chuẩn kèm món ăn.
                bookingService.updateStatusToPaid(content);

                System.out.println("✅ Xác nhận thanh toán dứt điểm và kích hoạt luồng gửi mail tự động từ Service.");

                return ResponseEntity.ok(
                        Map.of(
                                "success", true,
                                "message", "Đã cập nhật trạng thái PAID và xử lý email thành công"
                        )
                );
            }

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Nội dung cú pháp chuyển khoản không chứa mã nhận diện FeelHome"
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