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

    @Query("SELECT r FROM Room r " +
            "WHERE r.roomType.id = :roomTypeId " +
            "AND r.id NOT IN (" +
            "   SELECT bd.room.id FROM BookingDetail bd " +
            "   JOIN bd.booking b " +
            "   WHERE bd.room IS NOT NULL " +
            "   AND (UPPER(COALESCE(bd.status, 'PENDING')) = 'CHECKED_IN' " +
            "        OR (UPPER(COALESCE(bd.status, 'PENDING')) IN ('PENDING','APPROVED','CONFIRMED') " +
            "            AND b.checkinDate < :checkoutDate AND b.checkoutDate > :checkinDate))" +
            ") ORDER BY r.roomNumber")
    List<Room> findAvailableRooms(@Param("roomTypeId") Integer roomTypeId,
                                  @Param("checkinDate") LocalDate checkinDate,
                                  @Param("checkoutDate") LocalDate checkoutDate);
}
