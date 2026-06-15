package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    List<Booking> findAll();

    @Query("SELECT b FROM Booking b ORDER BY b.bookingDate DESC")
    List<Booking> findTop5ByOrderByBookingDateDesc();

    // Lọc booking theo status (dựa trên paymentStatus hoặc trạng thái)
    @Query("SELECT DISTINCT bd.booking FROM BookingDetail bd WHERE " +
            "(:status IS NULL OR " +
            "(:status = 'CHECKED' AND bd.booking.checkoutDate < CURRENT_DATE) OR " +
            "(:status = 'PENDING' AND bd.booking.checkinDate > CURRENT_DATE) OR " +
            "(:status = 'CANCELLED' AND 1=0))")
    List<Booking> findBookingsByStatus(@Param("status") String status);
}