package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, Integer> {

    // Kiểm tra phòng đã được đặt trùng ngày chưa
    @Query("""
            SELECT COUNT(bd) > 0
            FROM BookingDetail bd
            JOIN bd.booking b
            WHERE bd.room.id = :roomId
              AND b.checkinDate < :checkoutDate
              AND b.checkoutDate > :checkinDate
            """)
    boolean existsOverlappingBooking(
            @Param("roomId") Integer roomId,
            @Param("checkinDate") LocalDate checkinDate,
            @Param("checkoutDate") LocalDate checkoutDate
    );

    // Lấy danh sách booking bị trùng phòng
    @Query("""
            SELECT bd
            FROM BookingDetail bd
            JOIN bd.booking b
            WHERE bd.room.id IN :roomIds
              AND b.checkinDate < :checkoutDate
              AND b.checkoutDate > :checkinDate
            """)
    List<BookingDetail> findOverlappingBookings(
            @Param("roomIds") List<Integer> roomIds,
            @Param("checkinDate") LocalDate checkinDate,
            @Param("checkoutDate") LocalDate checkoutDate
    );

    // Tìm theo booking ID
    @Query("""
            SELECT bd
            FROM BookingDetail bd
            WHERE bd.booking.id = :bookingId
            """)
    List<BookingDetail> findByBookingId(@Param("bookingId") Integer bookingId);

    // Load tất cả chi tiết booking + booking + room + roomtype + food
    @Query("""
            SELECT DISTINCT bd
            FROM BookingDetail bd
            JOIN FETCH bd.booking b
            LEFT JOIN FETCH bd.room r
            JOIN FETCH bd.roomType rt
            LEFT JOIN FETCH bd.bookingFBs bfb
            LEFT JOIN FETCH bfb.fwb f
            ORDER BY b.bookingDate DESC
            """)
    List<BookingDetail> findAllWithDetails();

    // Filter booking
    @Query(value = """
            SELECT bd.*
            FROM booking_detail bd
            JOIN booking b ON b.id = bd.booking_id
            LEFT JOIN room r ON r.id = bd.room_id
            JOIN room_type rt ON rt.id = bd.room_type_id
            LEFT JOIN booking_f_b bfb ON bd.id = bfb.booking_detail_id
            LEFT JOIN fwb f ON f.id = bfb.fwb_id
            WHERE (:roomTypeId IS NULL OR rt.id = :roomTypeId)
              AND (:status IS NULL OR bd.status = :status)
              AND (:startDate IS NULL OR b.checkin_date >= :startDate)
              AND (:endDate IS NULL OR b.checkin_date <= :endDate)
            ORDER BY b.booking_date DESC
            """, nativeQuery = true)
    List<BookingDetail> filterBookings(
            @Param("roomTypeId") Integer roomTypeId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Duyệt booking và gán phòng
    @Modifying
    @Transactional
    @Query("""
            UPDATE BookingDetail bd
            SET bd.status = :status,
                bd.room.id = :roomId
            WHERE bd.id = :bookingDetailId
            """)
    int approveWithRoom(
            @Param("bookingDetailId") Integer bookingDetailId,
            @Param("status") String status,
            @Param("roomId") Integer roomId
    );

    // Cập nhật trạng thái booking
    @Modifying
    @Transactional
    @Query("""
            UPDATE BookingDetail bd
            SET bd.status = :status
            WHERE bd.id = :bookingDetailId
            """)
    int updateBookingDetailStatus(
            @Param("bookingDetailId") Integer bookingDetailId,
            @Param("status") String status
    );

    // Đếm số phòng đã đặt
    @Query("""
            SELECT COUNT(DISTINCT bd.room.id)
            FROM BookingDetail bd
            """)
    long countDistinctBookedRooms();

    // Tổng số lượng phòng đã giữ trong khoảng ngày
    @Query("""
            SELECT COALESCE(SUM(bd.roomQuantity), 0)
            FROM BookingDetail bd
            JOIN bd.booking b
            WHERE bd.roomType.id = :roomTypeId
              AND b.checkinDate < :checkoutDate
              AND b.checkoutDate > :checkinDate
              AND UPPER(COALESCE(bd.status, 'PENDING'))
                  IN ('PENDING', 'APPROVED', 'CONFIRMED', 'CHECKED_IN')
            """)
    Long sumReservedQuantityByRoomTypeAndDateRange(
            @Param("roomTypeId") Integer roomTypeId,
            @Param("checkinDate") LocalDate checkinDate,
            @Param("checkoutDate") LocalDate checkoutDate
    );

    // FIX LỖI CHỖ NÀY
    @Query("""
            SELECT bd
            FROM BookingDetail bd
            JOIN FETCH bd.booking
            WHERE bd.room.id = :id
            """)
    List<BookingDetail> findByRoomIdWithBooking(@Param("id") Integer id);

    // Booking đang hoạt động
    @Query("""
            SELECT bd
            FROM BookingDetail bd
            WHERE bd.status IN ('APPROVED','CHECKED_IN')
            """)
    List<BookingDetail> findOperationalBookings();
}
