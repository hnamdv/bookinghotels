package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Integer> {

    Optional<Room> findByRoomNumberIgnoreCase(String roomNumber);

    boolean existsByRoomNumberIgnoreCase(String roomNumber);

    @Query("SELECT r FROM Room r WHERE r.roomType.id = :roomTypeId ORDER BY r.roomNumber")
    List<Room> findByRoomTypeId(@Param("roomTypeId") Integer roomTypeId);

    // ===== TÌM PHÒNG TRỐNG THEO LOẠI PHÒNG VÀ KHOẢNG THỜI GIAN =====
    // Đã bỏ điều kiện r.status = 'APPROVED' vì entity Room không có field status
    // Thêm điều kiện kiểm tra deleteAt của Room
    @Query("""
        SELECT r FROM Room r
        WHERE r.roomType.id = :roomTypeId
          AND (r.deleteAt = false OR r.deleteAt IS NULL)
          AND r.id NOT IN (
              SELECT bd.room.id FROM BookingDetail bd
              JOIN bd.booking b
              WHERE bd.room.id IS NOT NULL
                AND (bd.deleteAt = false OR bd.deleteAt IS NULL)
                AND bd.status NOT IN ('CANCELLED', 'CHECKED_OUT')
                AND b.checkinDate < :checkoutDate
                AND b.checkoutDate > :checkinDate
          )
        ORDER BY r.roomNumber
        """)
    List<Room> findAvailableRooms(
            @Param("roomTypeId") Integer roomTypeId,
            @Param("checkinDate") LocalDate checkinDate,
            @Param("checkoutDate") LocalDate checkoutDate);

    // ===== ĐẾM SỐ PHÒNG THEO LOẠI (CHỈ ĐẾM PHÒNG CHƯA XÓA) =====
    @Query("""
        SELECT r.roomType.id, COUNT(r.id) 
        FROM Room r 
        WHERE r.roomType IS NOT NULL 
          AND (r.deleteAt = false OR r.deleteAt IS NULL)
        GROUP BY r.roomType.id
        """)
    List<Object[]> countRoomsGroupByRoomType();

    // ===== TÌM TẤT CẢ PHÒNG CÒN HOẠT ĐỘNG =====
    @Query("""
        SELECT r FROM Room r
        WHERE (r.deleteAt = false OR r.deleteAt IS NULL)
        ORDER BY r.roomNumber
        """)
    List<Room> findAllActive();

    // ===== TÌM PHÒNG THEO ID VÀ CHƯA XÓA =====
    @Query("""
        SELECT r FROM Room r
        WHERE r.id = :id
          AND (r.deleteAt = false OR r.deleteAt IS NULL)
        """)
    Optional<Room> findActiveById(@Param("id") Integer id);

    // ===== TÌM PHÒNG THEO SLUG VÀ CHƯA XÓA =====
    @Query("""
        SELECT r FROM Room r
        WHERE r.slug = :slug
          AND (r.deleteAt = false OR r.deleteAt IS NULL)
        """)
    Optional<Room> findActiveBySlug(@Param("slug") String slug);

    // ===== TÌM PHÒNG THEO SỐ PHÒNG VÀ CHƯA XÓA =====
    @Query("""
        SELECT r FROM Room r
        WHERE r.roomNumber = :roomNumber
          AND (r.deleteAt = false OR r.deleteAt IS NULL)
        """)
    Optional<Room> findActiveByRoomNumber(@Param("roomNumber") String roomNumber);
}