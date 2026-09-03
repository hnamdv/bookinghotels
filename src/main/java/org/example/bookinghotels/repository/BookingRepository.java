package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    // Hỗ trợ tìm kiếm linh hoạt bằng: Số điện thoại, Mã ID đơn hàng, hoặc Email của khách
    // Đổi kiểu trả về thành List<Booking> để chứa nhiều đơn nếu khách đặt nhiều lần bằng 1 SĐT/Email
    @Query("SELECT b FROM Booking b WHERE b.phone = :keyword OR CAST(b.id AS string) = :keyword OR b.email = :keyword")
    List<Booking> findByKeyword(@Param("keyword") String keyword);
    // Load tất cả booking (chưa xóa) kèm detail và room type
    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.bookingDetails bd " +
            "LEFT JOIN FETCH bd.roomType rt " +
            "WHERE b.deleteAt = false")
    List<Booking> findAllWithDetails();

    // Filter theo status, ngày, roomType (chưa xóa)
    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.bookingDetails bd " +
            "LEFT JOIN FETCH bd.roomType rt " +
            "WHERE b.deleteAt = false " +
            "AND (:roomTypeId IS NULL OR rt.id = :roomTypeId) " +
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

    // ==== Dùng cho trang trash ====
    List<Booking> findAllByDeleteAtTrue();
}