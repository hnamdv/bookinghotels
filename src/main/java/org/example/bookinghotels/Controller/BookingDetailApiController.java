package org.example.bookinghotels.Controller;

import org.springframework.security.access.prepost.PreAuthorize;

import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.example.bookinghotels.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/booking-details")
@PreAuthorize("isAuthenticated()")
public class BookingDetailApiController {

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private RoomRepository roomRepository;

    // API lấy danh sách phòng trống cho booking detail
    @GetMapping("/{id}/available-rooms")
    public ResponseEntity<?> getAvailableRooms(@PathVariable Integer id) {
        Optional<BookingDetail> opt = bookingDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không tìm thấy booking detail"));
        }

        BookingDetail bd = opt.get();

        if (!"PENDING".equals(bd.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không thể chọn phòng cho booking đã " + bd.getStatus()));
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

    // API gán phòng (chỉ lưu room_id, KHÔNG duyệt)
    @PutMapping("/{id}/assign-room")
    public ResponseEntity<?> assignRoom(@PathVariable Integer id, @RequestParam Integer roomId) {
        Optional<BookingDetail> opt = bookingDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không tìm thấy booking detail"));
        }

        BookingDetail bd = opt.get();

        if (!"PENDING".equals(bd.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Chỉ có thể gán phòng cho booking đang PENDING"));
        }

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Phòng không tồn tại"));
        }

        Room room = roomOpt.get();

        // Kiểm tra phòng trống
        List<Room> availableRooms = roomRepository.findAvailableRooms(
                bd.getRoomType().getId(),
                bd.getBooking().getCheckinDate(),
                bd.getBooking().getCheckoutDate()
        );
        boolean isAvailable = availableRooms.stream().anyMatch(r -> r.getId().equals(roomId));
        if (!isAvailable) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Phòng đã được đặt trong khoảng thời gian này"));
        }

        // Chỉ gán phòng, không đổi status
        bd.setRoom(room);
        bookingDetailRepository.save(bd);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "✅ Đã gán phòng " + room.getRoomNumber() + " cho booking #" + id,
                "roomNumber", room.getRoomNumber()
        ));
    }

    // API duyệt + gán phòng (gọi từ popup "Duyệt & Lưu")
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveWithRoom(@PathVariable Integer id, @RequestParam Integer roomId) {
        Optional<BookingDetail> opt = bookingDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không tìm thấy booking detail"));
        }

        BookingDetail bd = opt.get();

        if (!"PENDING".equals(bd.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Booking đã được xử lý"));
        }

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Phòng không tồn tại"));
        }

        Room room = roomOpt.get();

        List<Room> availableRooms = roomRepository.findAvailableRooms(
                bd.getRoomType().getId(),
                bd.getBooking().getCheckinDate(),
                bd.getBooking().getCheckoutDate()
        );
        boolean isAvailable = availableRooms.stream().anyMatch(r -> r.getId().equals(roomId));
        if (!isAvailable) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Phòng đã được đặt trong khoảng thời gian này"));
        }

        // Gán phòng + duyệt
        bd.setRoom(room);
        bd.setStatus("CONFIRMED");
        bookingDetailRepository.save(bd);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "✅ Đã duyệt booking #" + id + " - Phòng " + room.getRoomNumber(),
                "newStatus", "CONFIRMED",
                "roomNumber", room.getRoomNumber()
        ));
    }

    // API từ chối
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectBookingDetail(@PathVariable Integer id) {
        Optional<BookingDetail> opt = bookingDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không tìm thấy booking detail"));
        }

        BookingDetail bd = opt.get();

        if (!"PENDING".equals(bd.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Booking đã được xử lý"));
        }

        bd.setStatus("CANCELLED");
        bookingDetailRepository.save(bd);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "❌ Đã từ chối booking #" + id,
                "newStatus", "CANCELLED"
        ));
    }
}