package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    Optional<Promotion> findByPromotionNameIgnoreCase(String promotionName);
}