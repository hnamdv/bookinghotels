package org.example.bookinghotels.Controller;

import org.example.bookinghotels.dto.PublicOfferDto;
import org.example.bookinghotels.entity.Promotion;
import org.example.bookinghotels.entity.PromotionRoomType;
import org.example.bookinghotels.repository.PromotionRoomTypeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/public/offers")
public class PublicOfferController {
    private final PromotionRoomTypeRepository repository;

    public PublicOfferController(PromotionRoomTypeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<PublicOfferDto>> getActiveOffers() {
        LocalDate today = LocalDate.now();
        List<PublicOfferDto> offers = repository.findAll().stream()
                .filter(this::isUsable)
                .filter(mapping -> isActive(mapping.getPromotion(), today))
                .sorted(Comparator
                        .comparing((PromotionRoomType mapping) -> mapping.getPromotion().getDiscountPercent(), Comparator.reverseOrder())
                        .thenComparing(PromotionRoomType::getId, Comparator.reverseOrder()))
                .map(PublicOfferDto::new)
                .toList();
        return ResponseEntity.ok(offers);
    }

    private boolean isUsable(PromotionRoomType mapping) {
        return mapping != null
                && mapping.getPromotion() != null
                && mapping.getRoomType() != null
                && mapping.getPromotion().getDiscountPercent() != null
                && mapping.getPromotion().getDiscountPercent() > 0;
    }

    private boolean isActive(Promotion promotion, LocalDate today) {
        return (promotion.getStartDate() == null || !promotion.getStartDate().isAfter(today))
                && (promotion.getEndDate() == null || !promotion.getEndDate().isBefore(today));
    }
}
