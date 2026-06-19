package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    // Load tất cả booking kèm detail và room type (không filter)
    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.bookingDetails bd " +
            "LEFT JOIN FETCH bd.roomType rt")
    List<Booking> findAllWithDetails();

    // Filter theo status, ngày, roomType
    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.bookingDetails bd " +
            "LEFT JOIN FETCH bd.roomType rt " +
            "WHERE (:roomTypeId IS NULL OR rt.id = :roomTypeId) " +
            "AND (:status IS NULL OR " +
            "     (:status = 'CHECKED' AND b.checkoutDate < CURRENT_DATE) OR " +
            "     (:status = 'PENDING' AND b.checkinDate > CURRENT_DATE) OR " +
            "     (:status = 'CONFIRMED' AND b.checkinDate <= CURRENT_DATE AND b.checkoutDate >= CURRENT_DATE)) " +
            "AND (:startDate IS NULL OR b.checkinDate >= :startDate) " +
            "AND (:endDate IS NULL OR b.checkinDate <= :endDate) " +
            "ORDER BY b.bookingDate DESC")
    List<Booking> findFilteredWithDetails(@Param("roomTypeId") Integer roomTypeId,
                                          @Param("status") String status,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}