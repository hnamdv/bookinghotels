package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.RoomImg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RoomImgRepository extends JpaRepository<RoomImg, Integer> {
    List<RoomImg> findByRoomTypeId(Integer roomTypeId);
    boolean existsByRoomTypeIdAndImage(Integer roomTypeId, String image);
    @Transactional
    void deleteByRoomTypeId(Integer roomTypeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("delete from RoomImg ri where ri.roomType.id = :roomTypeId")
    int deleteAllByRoomTypeIdBulk(@Param("roomTypeId") Integer roomTypeId);
    @Transactional
    void deleteByImage(String image);
}
