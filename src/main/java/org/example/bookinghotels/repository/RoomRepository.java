package org.example.bookinghotels.repository;
import org.example.bookinghotels.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Integer> {
    //Bao start//
    long count();

    // Lấy tất cả phòng
    List<Room> findAll();
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
    //Bao end//
}
public interface RoomRepository extends JpaRepository<Room, Integer> {}