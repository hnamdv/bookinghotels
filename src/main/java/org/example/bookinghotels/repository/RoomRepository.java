package org.example.bookinghotels.repository;
import org.example.bookinghotels.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Integer> {
    long count();

    // Lấy tất cả phòng
    List<Room> findAll();
}