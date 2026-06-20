package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    // Load tất cả booking kèm detail và room type (không filter)
    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.bookingDetails bd " +
            "LEFT JOIN FETCH bd.roomType rt")
    List<Booking> findAllWithDetails();


    // Logic: Tìm số lượng phòng thuộc RoomType đã bị đặt trong khoảng thời gian
    @Query("SELECT COUNT(bd) FROM BookingDetail bd " +
            "JOIN bd.booking b " +
            "WHERE bd.roomType.id = :typeId " +
            "AND b.checkinDate < :checkout " +
            "AND b.checkoutDate > :checkin")
    long countBookedRoomsByTypeId(@Param("typeId") Integer typeId,
                                  @Param("checkin") LocalDate checkin,
                                  @Param("checkout") LocalDate checkout);


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
