package org.example.bookinghotels.repository;
import org.example.bookinghotels.entity.RoomImg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomImgRepository extends JpaRepository<RoomImg, Integer> {
    // Tìm danh sách toàn bộ ảnh thuộc về một loại phòng cụ thể
    List<RoomImg> findByRoomTypeId(Integer roomTypeId);
}