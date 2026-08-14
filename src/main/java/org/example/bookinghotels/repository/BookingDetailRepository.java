package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
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

    // ===== TÌM THEO BOOKING ID =====
    @Query("SELECT bd FROM BookingDetail bd WHERE bd.booking.id = :bookingId")
    List<BookingDetail> findByBookingId(@Param("bookingId") Integer bookingId);

    // ===== TÌM KIẾM VÀ LỌC HÓA ĐƠN (LOẠI BỎ BẢN GHI ĐÃ XÓA MỀM) =====
    @Query("SELECT DISTINCT bd FROM BookingDetail bd " +
            "LEFT JOIN FETCH bd.booking b " +
            "LEFT JOIN FETCH bd.room r " +
            "LEFT JOIN FETCH bd.roomType rt " +
            "LEFT JOIN FETCH bd.bookingFBs bfb " +
            "WHERE (bd.deleteAt = false OR bd.deleteAt IS NULL) AND " +
            "(:status = '' OR bd.status = :status) AND " +
            "(:keyword = '%' OR b.name LIKE :keyword OR b.phone LIKE :keyword OR b.email LIKE :keyword) " +
            "ORDER BY b.bookingDate DESC")
    List<BookingDetail> searchBookingDetails(@Param("keyword") String keyword, @Param("status") String status);

    // ===== LOAD TẤT CẢ (LOẠI BỎ BẢN GHI ĐÃ XÓA MỀM) =====
    @Query("SELECT DISTINCT bd FROM BookingDetail bd " +
            "JOIN FETCH bd.booking b " +
            "LEFT JOIN FETCH bd.room r " +
            "JOIN FETCH bd.roomType rt " +
            "LEFT JOIN FETCH bd.bookingFBs bfb " +
            "LEFT JOIN FETCH bfb.fwb f " +
            "WHERE bd.deleteAt = false OR bd.deleteAt IS NULL " +
            "ORDER BY b.bookingDate DESC")
    List<BookingDetail> findAllWithDetails();

    // ===== LẤY DANH SÁCH ĐÃ XÓA MỀM (CHO THÙNG RÁC) =====
    @Query("SELECT DISTINCT bd FROM BookingDetail bd " +
            "LEFT JOIN FETCH bd.booking b " +
            "LEFT JOIN FETCH bd.room r " +
            "LEFT JOIN FETCH bd.roomType rt " +
            "WHERE bd.deleteAt = true " +
            "ORDER BY b.bookingDate DESC")
    List<BookingDetail> findAllByDeleteAtTrue();

    // ===== FILTER - Native query =====
    @Query(value = "SELECT bd.* FROM booking_detail bd " +
            "JOIN booking b ON b.id = bd.booking_id " +
            "LEFT JOIN room r ON r.id = bd.room_id " +
            "JOIN room_type rt ON rt.id = bd.room_type_id " +
            "LEFT JOIN booking_f_b bfb ON bd.id = bfb.booking_detail_id " +
            "LEFT JOIN fwb f ON f.id = bfb.fwb_id " +
            "WHERE (bd.delete_at = false OR bd.delete_at IS NULL) " +
            "AND (CAST(:roomTypeId AS INTEGER) IS NULL OR rt.id = CAST(:roomTypeId AS INTEGER)) " +
            "AND (CAST(:status AS VARCHAR) IS NULL OR bd.status = CAST(:status AS VARCHAR)) " +
            "AND (CAST(:startDate AS DATE) IS NULL OR b.checkin_date >= CAST(:startDate AS DATE)) " +
            "AND (CAST(:endDate AS DATE) IS NULL OR b.checkin_date <= CAST(:endDate AS DATE)) " +
            "ORDER BY b.booking_date DESC", nativeQuery = true)
    List<BookingDetail> filterBookings(
            @Param("roomTypeId") Integer roomTypeId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Modifying
    @Transactional
    @Query("UPDATE BookingDetail bd SET bd.status = :status, bd.room.id = :roomId WHERE bd.id = :bookingDetailId")
    int approveWithRoom(@Param("bookingDetailId") Integer bookingDetailId,
                        @Param("status") String status,
                        @Param("roomId") Integer roomId);

    @Modifying
    @Transactional
    @Query("UPDATE BookingDetail bd SET bd.status = :status WHERE bd.id = :bookingDetailId")
    int updateBookingDetailStatus(@Param("bookingDetailId") Integer bookingDetailId, @Param("status") String status);

    @Query("""
    SELECT COUNT(DISTINCT b.room.id)
    FROM BookingDetail b
    """)
    long countDistinctBookedRooms();

    @Query("""
            SELECT COALESCE(SUM(bd.roomQuantity), 0)
            FROM BookingDetail bd
            JOIN bd.booking b
            WHERE bd.roomType.id = :roomTypeId
              AND b.checkinDate < :checkoutDate
              AND b.checkoutDate > :checkinDate
              AND UPPER(COALESCE(bd.status, 'PENDING')) IN ('PENDING', 'APPROVED', 'CONFIRMED', 'CHECKED_IN')
            """)
    Long sumReservedQuantityByRoomTypeAndDateRange(
            @Param("roomTypeId") Integer roomTypeId,
            @Param("checkinDate") LocalDate checkinDate,
            @Param("checkoutDate") LocalDate checkoutDate);

    @Query("""
            SELECT DISTINCT bd FROM BookingDetail bd
            JOIN FETCH bd.booking b
            LEFT JOIN FETCH bd.room r
            LEFT JOIN FETCH bd.roomType rt
            WHERE bd.room.id IN :roomIds
              AND UPPER(COALESCE(bd.status, 'PENDING')) IN ('PENDING','APPROVED','CONFIRMED','CHECKED_IN')
            """)
    List<BookingDetail> findActiveBookingsByRoomIds(@Param("roomIds") List<Integer> roomIds);

    @Query("""
            SELECT DISTINCT bd FROM BookingDetail bd
            JOIN FETCH bd.booking b
            LEFT JOIN FETCH bd.room r
            LEFT JOIN FETCH bd.roomType rt
            WHERE bd.room.id = :roomId
              AND UPPER(COALESCE(bd.status, 'PENDING')) IN ('PENDING','APPROVED','CONFIRMED','CHECKED_IN')
            """)
    List<BookingDetail> findByRoomIdWithBooking(@Param("roomId") Integer roomId);

    @Query("""
            SELECT DISTINCT bd FROM BookingDetail bd
            JOIN FETCH bd.booking b
            LEFT JOIN FETCH bd.room r
            LEFT JOIN FETCH bd.roomType rt
            WHERE UPPER(COALESCE(bd.status, 'PENDING')) IN ('PENDING','APPROVED','CONFIRMED','CHECKED_IN')
            """)
    List<BookingDetail> findOperationalBookings();

    @Query("""
            SELECT bd.roomType.id, COUNT(DISTINCT bd.room.id)
            FROM BookingDetail bd
            JOIN bd.booking b
            WHERE bd.roomType IS NOT NULL
              AND bd.room IS NOT NULL
              AND b.checkinDate < :checkoutDate
              AND b.checkoutDate > :checkinDate
              AND UPPER(COALESCE(bd.status, 'PENDING')) IN ('PENDING','APPROVED','CONFIRMED','CHECKED_IN')
            GROUP BY bd.roomType.id
            """)
    List<Object[]> countOccupiedRoomsByRoomType(@Param("checkinDate") LocalDate checkinDate,
                                                @Param("checkoutDate") LocalDate checkoutDate);

    @Query("""
            SELECT DISTINCT bd FROM BookingDetail bd
            JOIN FETCH bd.booking b
            LEFT JOIN FETCH bd.room r
            LEFT JOIN FETCH bd.roomType rt
            WHERE bd.roomType.id = :roomTypeId
              AND b.checkinDate < :checkoutDate
              AND b.checkoutDate > :checkinDate
              AND UPPER(COALESCE(bd.status, 'PENDING')) IN ('PENDING','APPROVED','CONFIRMED','CHECKED_IN')
            """)
    List<BookingDetail> findOverlappingBookingsByRoomType(@Param("roomTypeId") Integer roomTypeId,
                                                          @Param("checkinDate") LocalDate checkinDate,
                                                          @Param("checkoutDate") LocalDate checkoutDate);
}