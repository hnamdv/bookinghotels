package org.example.bookinghotels.controller;

import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.repository.BookingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/staff/reception")
@CrossOrigin(origins = "*")
public class ReceptionController {

    private final BookingRepository bookingRepository;

    public ReceptionController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayBookings() {
        try {

            LocalDate today = LocalDate.now();

            List<Booking> result = new ArrayList<>();

            for (Booking b : bookingRepository.findAll()) {

                if (b.getCheckinDate() == null || b.getCheckoutDate() == null) {
                    continue;
                }

                if (!today.isBefore(b.getCheckinDate())
                        && !today.isAfter(b.getCheckoutDate())) {

                    result.add(b);
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/checkin/{id}")
    public ResponseEntity<?> checkIn(@PathVariable Integer id) {

        try {

            Booking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

            if (booking.getActualCheckin() != null) {
                return ResponseEntity.badRequest()
                        .body("Khách đã check-in");
            }

            booking.setActualCheckin(LocalDateTime.now());

            bookingRepository.save(booking);

            return ResponseEntity.ok(
                    "Check-in thành công cho khách: " + booking.getName());

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body("Lỗi: " + e.getMessage());
        }
    }

    @PostMapping("/checkout/{id}")
    public ResponseEntity<?> checkOut(@PathVariable Integer id) {

        try {

            Booking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

            if (booking.getActualCheckin() == null) {
                return ResponseEntity.badRequest()
                        .body("Khách chưa check-in");
            }

            if (booking.getActualCheckout() != null) {
                return ResponseEntity.badRequest()
                        .body("Khách đã check-out");
            }

            booking.setActualCheckout(LocalDateTime.now());

            bookingRepository.save(booking);

            return ResponseEntity.ok(
                    "Check-out thành công cho khách: " + booking.getName());

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body("Lỗi: " + e.getMessage());
        }
    }
}