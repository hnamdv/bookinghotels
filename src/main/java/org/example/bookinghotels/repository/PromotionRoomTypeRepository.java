package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.PromotionRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRoomTypeRepository extends JpaRepository<PromotionRoomType, Integer> {

    boolean existsByPromotion_IdAndRoomType_Id(Integer promotionId, Integer roomTypeId);

    List<PromotionRoomType> findByPromotion_Id(Integer promotionId);

    void deleteByPromotion_Id(Integer promotionId);
}