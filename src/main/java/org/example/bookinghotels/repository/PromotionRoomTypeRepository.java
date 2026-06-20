package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.PromotionRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PromotionRoomTypeRepository extends JpaRepository<PromotionRoomType, Integer> {
    boolean existsByRoomTypeIdAndPromotionId(Integer roomTypeId, Integer promotionId);
    List<PromotionRoomType> findByRoomTypeId(Integer roomTypeId);
    @Transactional
    void deleteByRoomTypeId(Integer roomTypeId);
}
