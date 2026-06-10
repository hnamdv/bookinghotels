package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    // Lấy tất cả booking
    List<Booking> findAll();

    // Lấy 5 booking gần nhất
    @Query("SELECT b FROM Booking b ORDER BY b.bookingDate DESC")
    List<Booking> findTop5ByOrderByBookingDateDesc();
}