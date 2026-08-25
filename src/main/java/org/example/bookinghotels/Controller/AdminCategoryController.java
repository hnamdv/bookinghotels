package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.service.HotelRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/hotels")
@PreAuthorize("hasAuthority('ROLE_HOTEL')")
public class AdminCategoryController {

    private final HotelRoomService hotelRoomService;

    public AdminCategoryController(HotelRoomService hotelRoomService) {
        this.hotelRoomService = hotelRoomService;
    }

    // Danh sách chi nhánh
    @GetMapping
    public ResponseEntity<List<Hotels>> getAllHotels() {
        return ResponseEntity.ok(hotelRoomService.getAllHotels());
    }

    // Thêm chi nhánh
    @PostMapping
    public ResponseEntity<Hotels> createHotel(@RequestBody Hotels hotel) {
        return ResponseEntity.ok(hotelRoomService.createHotel(hotel));
    }

    // Cập nhật chi nhánh
    @PutMapping("/{id}")
    public ResponseEntity<Hotels> updateHotel(@PathVariable Integer id,
                                              @RequestBody Hotels hotel) {
        return ResponseEntity.ok(hotelRoomService.updateHotel(id, hotel));
    }

    // Xóa chi nhánh
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteHotel(@PathVariable Integer id) {

        hotelRoomService.deleteHotel(id);

        return ResponseEntity.ok(
                Map.of("message", "Xóa chi nhánh thành công!")
        );
    }
}