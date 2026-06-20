package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.RoomImg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RoomImgRepository extends JpaRepository<RoomImg, Integer> {
    List<RoomImg> findByRoomTypeId(Integer roomTypeId);
    boolean existsByRoomTypeIdAndImage(Integer roomTypeId, String image);
    @Transactional
    void deleteByRoomTypeId(Integer roomTypeId);
    @Transactional
    void deleteByImage(String image);
}
