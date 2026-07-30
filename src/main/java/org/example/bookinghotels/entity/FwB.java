package org.example.bookinghotels.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import java.util.Map;

@Entity
@Table(name = "fwb")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FwB {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "booking_fwb_id")
    private Integer bookingFwbId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String status;

    // Sửa hàm lấy tên dịch vụ: Chống crash khi dữ liệu không phải JSON
    public String getName() {
        if (this.description == null || this.description.trim().isEmpty()) {
            return "Dịch vụ phòng khách sạn";
        }

        String trimmed = this.description.trim();
        // Kiểm tra xem chuỗi có dạng JSON hợp lệ hay không (bắt đầu bằng { )
        if (trimmed.startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> data = mapper.readValue(trimmed, Map.class);
                if (data != null && data.containsKey("name")) {
                    return data.get("name").toString();
                }
            } catch (Exception e) {
                // Nếu parse JSON lỗi thì fallback xuống xử lý như chuỗi thường bên dưới
            }
        }

        // Nếu không phải JSON (Ví dụ chuỗi: "Ăn cơm", "Nước uống"), trả về chính nó luôn
        return trimmed;
    }

    // Sửa hàm lấy giá: Tránh sập luồng khi dữ liệu là text thường
    public double getPrice() {
        if (this.description == null || this.description.trim().isEmpty()) {
            return 0.0;
        }

        String trimmed = this.description.trim();
        if (trimmed.startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> data = mapper.readValue(trimmed, Map.class);
                if (data != null && data.containsKey("price")) {
                    Object priceObj = data.get("price");
                    if (priceObj instanceof Number) {
                        return ((Number) priceObj).doubleValue();
                    }
                }
            } catch (Exception e) {
                // Bỏ qua lỗi để hệ thống tiếp tục chạy mượt mà
            }
        }

        // Mặc định trả về 0 nếu trường text không chứa cấu trúc giá JSON
        return 0.0;
    }
}