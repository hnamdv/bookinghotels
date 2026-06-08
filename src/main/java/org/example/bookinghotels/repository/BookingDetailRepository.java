package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, Integer> {
    // =====================================================
    // JPQL: Kiểm tra 1 phòng có bị gối lịch không
    // =====================================================
    @Query("SELECT COUNT(bd) > 0 FROM BookingDetail bd " +
            "JOIN bd.booking b " +
            "WHERE bd.room.id = :roomId " +
            "AND b.checkinDate < :checkoutDate " +
            "AND b.checkoutDate > :checkinDate")
    boolean existsOverlappingBooking(
            @Param("roomId") Integer roomId,
            @Param("checkinDate") LocalDate checkinDate,
            @Param("checkoutDate") LocalDate checkoutDate);

    // =====================================================
    // JPQL: Lấy danh sách BookingDetail bị gối lịch
    // =====================================================
    @Query("SELECT bd FROM BookingDetail bd " +
            "JOIN bd.booking b " +
            "WHERE bd.room.id IN :roomIds " +
            "AND b.checkinDate < :checkoutDate " +
            "AND b.checkoutDate > :checkinDate")
    List<BookingDetail> findOverlappingBookings(
            @Param("roomIds") List<Integer> roomIds,
            @Param("checkinDate") LocalDate checkinDate,
            @Param("checkoutDate") LocalDate checkoutDate);
}