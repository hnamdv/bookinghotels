package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Integer> {
    Optional<Room> findByRoomNumberIgnoreCase(String roomNumber);
    boolean existsByRoomNumberIgnoreCase(String roomNumber);

    @Query("SELECT r FROM Room r WHERE r.roomType.id = :roomTypeId ORDER BY r.roomNumber")
    List<Room> findByRoomTypeId(@Param("roomTypeId") Integer roomTypeId);
    //bao//
    // Tìm phòng trống theo room_type và khoảng thời gian
    @Query("SELECT r FROM Room r " +
            "WHERE r.roomType.id = :roomTypeId " +
            "AND r.id NOT IN (" +
            "   SELECT bd.room.id FROM BookingDetail bd " +
            "   JOIN bd.booking b " +
            "   WHERE bd.room IS NOT NULL " +
            "   AND bd.status IN ('PENDING', 'CONFIRMED') " +
            "   AND b.checkinDate < :checkoutDate " +
            "   AND b.checkoutDate > :checkinDate" +
            ")")
    List<Room> findAvailableRooms(@Param("roomTypeId") Integer roomTypeId,
                                  @Param("checkinDate") LocalDate checkinDate,
                                  @Param("checkoutDate") LocalDate checkoutDate);

    // Đếm số phòng đã đặt theo loại phòng trong khoảng thời gian
    @Query("SELECT COUNT(bd) FROM BookingDetail bd " +
            "JOIN bd.booking b " +
            "WHERE bd.roomType.id = :roomTypeId " +
            "AND bd.room IS NOT NULL " +
            "AND bd.status IN ('PENDING', 'CONFIRMED') " +
            "AND b.checkinDate < :checkoutDate " +
            "AND b.checkoutDate > :checkinDate")
    long countBookedRoomsByType(@Param("roomTypeId") Integer roomTypeId,
                                @Param("checkinDate") LocalDate checkinDate,
                                @Param("checkoutDate") LocalDate checkoutDate);

    // Đếm tổng số phòng của loại phòng
    @Query("SELECT COUNT(r) FROM Room r WHERE r.roomType.id = :roomTypeId")
    long countTotalRoomsByType(@Param("roomTypeId") Integer roomTypeId);
    // Đếm số phòng còn trống theo loại phòng trong khoảng thời gian
    @Query("SELECT COUNT(r) FROM Room r " +
            "WHERE r.roomType.id = :roomTypeId " +
            "AND r.id NOT IN (" +
            "   SELECT bd.room.id FROM BookingDetail bd " +
            "   JOIN bd.booking b " +
            "   WHERE bd.room IS NOT NULL " +
            "   AND bd.status IN ('PENDING', 'CONFIRMED') " +
            "   AND b.checkinDate < :checkoutDate " +
            "   AND b.checkoutDate > :checkinDate" +
            ")")
    long countAvailableRoomsByType(@Param("roomTypeId") Integer roomTypeId,
                                   @Param("checkinDate") LocalDate checkinDate,
                                   @Param("checkoutDate") LocalDate checkoutDate);

}
