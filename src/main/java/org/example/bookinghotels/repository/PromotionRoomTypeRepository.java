package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.PromotionRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRoomTypeRepository extends JpaRepository<PromotionRoomType, Integer> {
}