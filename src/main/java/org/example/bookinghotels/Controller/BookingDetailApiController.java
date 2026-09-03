package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.*;
import org.example.bookinghotels.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/booking-details")
public class BookingDetailApiController {

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private FwbRepository fwbRepository;

    @Autowired
    private BookingFBRepository bookingFBRepository;

    // ===== API LẤY THÔNG TIN CHI TIẾT BOOKING =====
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingDetail(@PathVariable Integer id) {
        Optional<BookingDetail> opt = bookingDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy booking detail"
            ));
        }

        BookingDetail bd = opt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("id", bd.getId());
        response.put("status", bd.getStatus());
        response.put("bookingId", bd.getBooking().getId());
        response.put("customerName", bd.getBooking().getName());
        response.put("customerPhone", bd.getBooking().getPhone());
        response.put("customerEmail", bd.getBooking().getEmail());

        // Thông tin phòng
        if (bd.getRoom() != null) {
            response.put("roomId", bd.getRoom().getId());
            response.put("roomNumber", bd.getRoom().getRoomNumber());
        } else {
            response.put("roomId", null);
            response.put("roomNumber", null);
        }

        // Thông tin loại phòng
        if (bd.getRoomType() != null) {
            response.put("roomTypeId", bd.getRoomType().getId());
            response.put("roomTypeName", bd.getRoomType().getNameType());
        }

        response.put("checkinDate", bd.getBooking().getCheckinDate().toString());
        response.put("checkoutDate", bd.getBooking().getCheckoutDate().toString());
        response.put("adultCount", bd.getAdultCount());
        response.put("childCount", bd.getChildCount());
        response.put("price", bd.getPrice());

        return ResponseEntity.ok(response);
    }

    // ===== API LẤY DANH SÁCH F&B =====
    @GetMapping("/{id}/foods")
    public ResponseEntity<?> getFoods(@PathVariable Integer id) {
        Optional<BookingDetail> opt = bookingDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy booking detail"
            ));
        }

        // Lấy tất cả F&B đang active
        List<FwB> foods = fwbRepository.findAll().stream()
                .filter(f -> f != null)
                .filter(f -> f.getStatus() == null || f.getStatus().isBlank() || "ACTIVE".equalsIgnoreCase(f.getStatus()))
                .filter(f -> f.getPrice() > 0D)
                .toList();

        // Lấy các F&B đã đặt cho booking detail này
        List<BookingFB> existingOrders = bookingFBRepository.findByBookingDetailId(id);

        // Tạo map để kiểm tra số lượng đã đặt
        Map<Integer, Integer> orderedQuantities = new HashMap<>();
        for (BookingFB order : existingOrders) {
            if (order.getFwb() != null) {
                orderedQuantities.put(order.getFwb().getId(), order.getQuantity());
            }
        }

        // Tạo response
        List<Map<String, Object>> foodList = foods.stream().map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("name", f.getName() != null ? f.getName() : f.getDescription());
            map.put("description", f.getDescription());
            map.put("price", f.getPrice());
            map.put("quantity", orderedQuantities.getOrDefault(f.getId(), 0));
            return map;
        }).toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "foods", foodList
        ));
    }

    // ===== API LƯU F&B ORDER =====
    @PostMapping("/{id}/foods")
    public ResponseEntity<?> saveFoods(@PathVariable Integer id, @RequestBody List<Map<String, Object>> items) {
        Optional<BookingDetail> opt = bookingDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy booking detail"
            ));
        }

        BookingDetail bd = opt.get();

        // Xóa các F&B cũ
        bookingFBRepository.deleteByBookingDetailId(id);

        // Thêm F&B mới
        for (Map<String, Object> item : items) {
            Integer fwbId = null;
            Integer quantity = null;

            // Xử lý các kiểu dữ liệu khác nhau
            Object fwbIdObj = item.get("fwbId");
            Object qtyObj = item.get("quantity");

            if (fwbIdObj instanceof Number) {
                fwbId = ((Number) fwbIdObj).intValue();
            } else if (fwbIdObj instanceof String) {
                try {
                    fwbId = Integer.parseInt((String) fwbIdObj);
                } catch (NumberFormatException e) {
                    // Bỏ qua nếu không parse được
                }
            }

            if (qtyObj instanceof Number) {
                quantity = ((Number) qtyObj).intValue();
            } else if (qtyObj instanceof String) {
                try {
                    quantity = Integer.parseInt((String) qtyObj);
                } catch (NumberFormatException e) {
                    // Bỏ qua nếu không parse được
                }
            }

            if (fwbId != null && quantity != null && quantity > 0) {
                Optional<FwB> fwbOpt = fwbRepository.findById(fwbId);
                if (fwbOpt.isPresent()) {
                    BookingFB bookingFB = new BookingFB();
                    bookingFB.setBookingDetail(bd);
                    bookingFB.setFwb(fwbOpt.get());
                    bookingFB.setQuantity(quantity);
                    bookingFB.setPriceAtOrder(fwbOpt.get().getPrice());
                    bookingFBRepository.save(bookingFB);
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "✅ Đã cập nhật dịch vụ F&B thành công"
        ));
    }

    // ===== API LẤY DANH SÁCH PHÒNG TRỐNG =====
    @GetMapping("/{id}/available-rooms")
    public ResponseEntity<?> getAvailableRooms(@PathVariable Integer id) {
        Optional<BookingDetail> opt = bookingDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy booking detail"
            ));
        }

        BookingDetail bd = opt.get();

        if ("CHECKED_OUT".equals(bd.getStatus()) || "CANCELLED".equals(bd.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Booking đã kết thúc hoặc đã hủy, không thể chọn phòng"
            ));
        }

        List<Room> availableRooms = roomRepository.findAvailableRooms(
                bd.getRoomType().getId(),
                bd.getBooking().getCheckinDate(),
                bd.getBooking().getCheckoutDate()
        );

        List<Map<String, Object>> rooms = availableRooms.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("roomNumber", r.getRoomNumber());
            map.put("slug", r.getSlug());
            map.put("thumbnail", r.getThumbnail());
            return map;
        }).toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "rooms", rooms,
                "roomTypeName", bd.getRoomType().getNameType()
        ));
    }

    // ===== API GÁN PHÒNG (Chỉ gán, không đổi trạng thái) =====
    @PutMapping("/{id}/assign-room")
    public ResponseEntity<?> assignRoom(@PathVariable Integer id, @RequestParam Integer roomId) {
        Optional<BookingDetail> opt = bookingDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy booking detail"
            ));
        }

        BookingDetail bd = opt.get();

        if ("CHECKED_OUT".equals(bd.getStatus()) || "CANCELLED".equals(bd.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Booking đã kết thúc hoặc đã hủy, không thể gán phòng"
            ));
        }

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Phòng không tồn tại"
            ));
        }

        Room room = roomOpt.get();

        List<Room> availableRooms = roomRepository.findAvailableRooms(
                bd.getRoomType().getId(),
                bd.getBooking().getCheckinDate(),
                bd.getBooking().getCheckoutDate()
        );
        boolean isAvailable = availableRooms.stream().anyMatch(r -> r.getId().equals(roomId));
        if (!isAvailable) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Phòng đã được đặt trong khoảng thời gian này"
            ));
        }

        bd.setRoom(room);
        bookingDetailRepository.save(bd);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "✅ Đã gán phòng " + room.getRoomNumber() + " cho booking #" + id,
                "roomNumber", room.getRoomNumber()
        ));
    }

    // ===== API DUYỆT BOOKING (PENDING → APPROVED) =====
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveWithRoom(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer roomId) {

        Optional<BookingDetail> opt = bookingDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy booking detail"
            ));
        }

        BookingDetail bd = opt.get();

        if (!"PENDING".equals(bd.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Chỉ có thể duyệt booking đang PENDING"
            ));
        }

        if (roomId != null) {
            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Phòng không tồn tại"
                ));
            }

            Room room = roomOpt.get();

            List<Room> availableRooms = roomRepository.findAvailableRooms(
                    bd.getRoomType().getId(),
                    bd.getBooking().getCheckinDate(),
                    bd.getBooking().getCheckoutDate()
            );
            boolean isAvailable = availableRooms.stream().anyMatch(r -> r.getId().equals(roomId));
            if (!isAvailable) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Phòng đã được đặt trong khoảng thời gian này"
                ));
            }
            bd.setRoom(room);
        } else {
            if (bd.getRoom() == null || bd.getRoom().getId() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Vui lòng chọn phòng trước khi duyệt"
                ));
            }
        }

        bd.setStatus("APPROVED");
        bookingDetailRepository.save(bd);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "✅ Đã duyệt booking #" + id,
                "newStatus", "APPROVED",
                "roomNumber", bd.getRoom() != null ? bd.getRoom().getRoomNumber() : null
        ));
    }

    // ===== API THANH TOÁN (APPROVED → PAID) =====
    @PutMapping("/{id}/mark-paid")
    public ResponseEntity<?> markAsPaid(@PathVariable Integer id) {
        Optional<BookingDetail> opt = bookingDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy booking detail"
            ));
        }

        BookingDetail bd = opt.get();

        if (!"APPROVED".equals(bd.getStatus()) && !"PENDING".equals(bd.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Chỉ có thể thanh toán booking đã duyệt hoặc đang chờ"
            ));
        }

        if ("PENDING".equals(bd.getStatus()) && (bd.getRoom() == null || bd.getRoom().getId() == null)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Vui lòng duyệt và gán phòng trước khi thanh toán"
            ));
        }

        bd.setStatus("PAID");
        bookingDetailRepository.save(bd);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "✅ Đã xác nhận thanh toán booking #" + id,
                "newStatus", "PAID"
        ));
    }

    // ===== API CHECK-OUT (PAID → CHECKED_OUT) =====
    @PutMapping("/{id}/check-out")
    public ResponseEntity<?> checkOut(@PathVariable Integer id) {
        Optional<BookingDetail> opt = bookingDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy booking detail"
            ));
        }

        BookingDetail bd = opt.get();

        if (!"PAID".equals(bd.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Chỉ có thể trả phòng khi đã thanh toán"
            ));
        }

        bd.setStatus("CHECKED_OUT");
        bookingDetailRepository.save(bd);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "✅ Đã trả phòng booking #" + id,
                "newStatus", "CHECKED_OUT"
        ));
    }

    // ===== API HỦY BOOKING (→ CANCELLED) =====
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Integer id,
            @RequestParam(required = false) String reason) {

        Optional<BookingDetail> opt = bookingDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy booking detail"
            ));
        }

        BookingDetail bd = opt.get();

        if ("CHECKED_OUT".equals(bd.getStatus()) || "CANCELLED".equals(bd.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Booking đã kết thúc hoặc đã hủy"
            ));
        }

        bd.setStatus("CANCELLED");
        bookingDetailRepository.save(bd);

        String message = "❌ Đã hủy booking #" + id;
        if (reason != null && !reason.isEmpty()) {
            message += " - Lý do: " + reason;
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", message,
                "newStatus", "CANCELLED"
        ));
    }

    // ===== API MỞ LẠI BOOKING ĐÃ HỦY (CANCELLED → PENDING) =====
    @PutMapping("/{id}/reopen")
    public ResponseEntity<?> reopenBooking(@PathVariable Integer id) {
        Optional<BookingDetail> opt = bookingDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy booking detail"
            ));
        }

        BookingDetail bd = opt.get();

        if (!"CANCELLED".equals(bd.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Chỉ có thể mở lại booking đã hủy"
            ));
        }

        bd.setStatus("PENDING");
        bookingDetailRepository.save(bd);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "✅ Đã mở lại booking #" + id,
                "newStatus", "PENDING"
        ));
    }
}