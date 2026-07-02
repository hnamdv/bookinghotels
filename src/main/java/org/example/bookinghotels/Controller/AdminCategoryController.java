package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.service.HotelRoomService;
import org.example.bookinghotels.service.RoomTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_HOTEL')")
public class AdminCategoryController {

    @Autowired
    private HotelRoomService hotelRoomService;

    @Autowired
    private RoomTypeService roomTypeService;

    // ==================== 1. CRUD KHÁCH SẠN (HOTELS) ====================
    @GetMapping("/hotels")
    public List<Hotels> getAllHotels() {
        return hotelRoomService.getAllHotels();
    }

    @PostMapping("/hotels")
    public Hotels createHotel(@RequestBody Hotels hotel) {
        return hotelRoomService.createHotel(hotel);
    }

    @PutMapping("/hotels/{id}")
    public ResponseEntity<Hotels> updateHotel(@PathVariable Integer id, @RequestBody Hotels hotelDetails) {
        Hotels updatedHotel = hotelRoomService.updateHotel(id, hotelDetails);
        return ResponseEntity.ok(updatedHotel);
    }

    @DeleteMapping("/hotels/{id}")
    public ResponseEntity<?> deleteHotel(@PathVariable Integer id) {
        hotelRoomService.deleteHotel(id);
        return ResponseEntity.ok(Map.of("message", "Xóa chi nhánh khách sạn thành công!"));
    }

    // ==================== 2. CRUD LOẠI PHÒNG & UPLOAD ẢNH CHỐNG LAG ====================
    @GetMapping("/room-types")
    public List<RoomType> getAllRoomTypes() {
        return roomTypeService.getAllRoomTypes();
    }

    @PostMapping("/room-types")
    public RoomType createRoomType(@RequestBody RoomType roomType) {
        return roomTypeService.createRoomType(roomType);
    }

    @PutMapping("/room-types/{id}")
    public ResponseEntity<RoomType> updateRoomType(@PathVariable Integer id, @RequestBody RoomType roomTypeDetails) {
        RoomType updatedRoomType = roomTypeService.updateRoomType(id, roomTypeDetails);
        return ResponseEntity.ok(updatedRoomType);
    }

    @DeleteMapping("/room-types/{id}")
    public ResponseEntity<?> deleteRoomType(@PathVariable Integer id) {
        roomTypeService.deleteRoomType(id);
        return ResponseEntity.ok(Map.of("message", "Xóa loại phòng thành công!"));
    }

    @PostMapping("/room-types/{id}/upload-image")
    public ResponseEntity<?> uploadImage(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        try {
            String url = roomTypeService.uploadRoomTypeImage(id, file);
            return ResponseEntity.ok(Map.of("message", "Upload và nén ảnh thành công!", "url", url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi xử lý file hệ thống!"));
        }
    }

    // ==================== 3. CRUD PHÒNG VẬT LÝ (101, 102...) ====================
    @GetMapping("/rooms")
    public List<Room> getAllRooms() {
        return hotelRoomService.getAllRooms();
    }

    @PostMapping("/rooms")
    public Room createRoom(@RequestBody Room room) {
        return hotelRoomService.createRoom(room);
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Integer id, @RequestBody Room roomDetails) {
        Room updatedRoom = hotelRoomService.updateRoom(id, roomDetails);
        return ResponseEntity.ok(updatedRoom);
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Integer id) {
        try {
            hotelRoomService.deleteRoom(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Xóa phòng vật lý thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể xóa phòng này vì đang dính dữ liệu đặt phòng!"));
        }
    }

    // ==================== 4. CẬP NHẬT TRẠNG THÁI LỊCH PHÒNG THỦ CÔNG ====================
    @PostMapping("/rooms/{roomId}/status-manual")
    public ResponseEntity<?> setRoomStatus(
            @PathVariable Integer roomId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("status") String status) {

        hotelRoomService.updateRoomStatusManually(roomId, date, status);
        return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái phòng thành công!"));
    }
}