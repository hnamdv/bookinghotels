package org.example.bookinghotels.Controller;


import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.service.HotelRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@CrossOrigin(origins = "*")
public class HotelController {

    @Autowired
    private HotelRoomService hotelRoomService;
    @GetMapping
    public ResponseEntity<List<Hotels>> getAllHotels() {
        List<Hotels> hotels = hotelRoomService.getAllHotels();
        return ResponseEntity.ok(hotels);
    }
}