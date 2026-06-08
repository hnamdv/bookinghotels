package org.example.bookinghotels.repository;
import org.example.bookinghotels.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Integer> {

}