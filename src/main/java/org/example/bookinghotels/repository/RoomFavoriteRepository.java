package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.RoomFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomFavoriteRepository extends JpaRepository<RoomFavorite, Integer> {
    List<RoomFavorite> findByUserId(Integer userId);
    boolean existsByUserIdAndRoomType_Id(Integer userId, Integer roomTypeId);
    Optional<RoomFavorite> findByUserIdAndRoomType_Id(Integer userId, Integer roomTypeId);
    void deleteByUserIdAndRoomType_Id(Integer userId, Integer roomTypeId);
}
