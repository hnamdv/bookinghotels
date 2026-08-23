package org.example.bookinghotels.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chỉ dùng để HIỂN THỊ cột "Mô tả" trong Activity Logs sang tiếng Việt.
 * Không đụng tới dữ liệu gốc trong DB, không ảnh hưởng tới logic
 * existsByActionAndTableNameAndDescriptionContaining(...) đang dùng
 * chuỗi gốc để check trùng.
 *
 * Nhận diện được format nào thì dịch, không nhận diện được thì trả về
 * nguyên bản gốc — không bao giờ làm mất/méo thông tin.
 */
public class ActivityLogDisplay {

    // VD: LATE_CHECKIN|DETAIL#262|DATE#2026-08-23
    private static final Pattern LATE_CHECKIN_PATTERN =
            Pattern.compile("^LATE_CHECKIN\\|DETAIL#(\\d+)\\|DATE#(.+)$");

    // VD: UPDATE RoomType (id=8) | user=admin
    // VD: CREATE Room (id=171) | user=admin
    // VD: CREATE Room (id=170)                <- không có phần user
    // VD: UPDATE Hotels (id=4)                 <- không có phần user
    private static final Pattern CRUD_PATTERN =
            Pattern.compile("^(CREATE|UPDATE|DELETE)\\s+(\\w+)\\s*\\(id=([^)]+)\\)(?:\\s*\\|\\s*user=(.+))?$");

    public static String humanize(String description) {
        if (description == null) return "";

        Matcher lateCheckin = LATE_CHECKIN_PATTERN.matcher(description);
        if (lateCheckin.matches()) {
            return "Khách nhận phòng trễ - Chi tiết đặt phòng #" + lateCheckin.group(1)
                    + " (" + lateCheckin.group(2) + ")";
        }

        Matcher crud = CRUD_PATTERN.matcher(description);
        if (crud.matches()) {
            String actionLabel = switch (crud.group(1)) {
                case "CREATE" -> "Tạo mới";
                case "UPDATE" -> "Cập nhật";
                case "DELETE" -> "Xóa";
                default -> crud.group(1);
            };
            String entity = translateEntity(crud.group(2));
            String id = crud.group(3);
            String user = crud.group(4); // có thể null nếu log không kèm user

            String result = actionLabel + " " + entity + " #" + id;
            if (user != null && !user.isBlank()) {
                result += " bởi " + user;
            }
            return result;
        }

        // Không khớp format nào đã biết -> giữ nguyên bản gốc
        return description;
    }

    // Bổ sung dần khi thấy còn entity nào chưa được dịch
    private static String translateEntity(String entity) {
        return switch (entity) {
            case "RoomType" -> "loại phòng";
            case "Room" -> "phòng";
            case "Promotion" -> "khuyến mãi";
            case "Booking" -> "đặt phòng";
            case "BookingDetail" -> "chi tiết đặt phòng";
            case "Invoice" -> "hóa đơn";
            case "User" -> "người dùng";
            default -> entity;
        };
    }
}