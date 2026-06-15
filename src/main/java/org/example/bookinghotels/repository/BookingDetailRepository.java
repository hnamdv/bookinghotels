package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, Integer> {

    @Query("SELECT COUNT(bd) > 0 FROM BookingDetail bd " +
            "JOIN bd.booking b " +
            "WHERE bd.room.id = :roomId " +
            "AND b.checkinDate < :checkoutDate " +
            "AND b.checkoutDate > :checkinDate")
    boolean existsOverlappingBooking(
            @Param("roomId") Integer roomId,
            @Param("checkinDate") LocalDate checkinDate,
            @Param("checkoutDate") LocalDate checkoutDate);

    @Query("SELECT bd FROM BookingDetail bd " +
            "JOIN bd.booking b " +
            "WHERE bd.room.id IN :roomIds " +
            "AND b.checkinDate < :checkoutDate " +
            "AND b.checkoutDate > :checkinDate")
    List<BookingDetail> findOverlappingBookings(
            @Param("roomIds") List<Integer> roomIds,
            @Param("checkinDate") LocalDate checkinDate,
            @Param("checkoutDate") LocalDate checkoutDate);

    @Query("SELECT bd FROM BookingDetail bd JOIN FETCH bd.booking JOIN FETCH bd.room JOIN FETCH bd.roomType")
    List<BookingDetail> findAllWithDetails();

    // Lọc booking detail theo room type và date range
    @Query("SELECT bd FROM BookingDetail bd JOIN FETCH bd.booking b JOIN FETCH bd.room r JOIN FETCH bd.roomType rt " +
            "WHERE (:roomTypeId IS NULL OR rt.id = :roomTypeId) " +
            "AND (:status IS NULL OR " +
            "     (:status = 'CHECKED' AND b.checkoutDate < CURRENT_DATE) OR " +
            "     (:status = 'PENDING' AND b.checkinDate > CURRENT_DATE) OR " +
            "     (:status = 'CONFIRMED' AND b.checkinDate >= CURRENT_DATE AND b.checkoutDate <= CURRENT_DATE)) " +
            "AND (:startDate IS NULL OR b.checkinDate >= :startDate) " +
            "AND (:endDate IS NULL OR b.checkoutDate <= :endDate)")
    List<BookingDetail> filterBookings(
            @Param("roomTypeId") Integer roomTypeId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}