package org.example.bookinghotels.Controller;

import org.example.bookinghotels.dto.PublicOfferDto;
import org.example.bookinghotels.entity.Promotion;
import org.example.bookinghotels.entity.PromotionRoomType;
import org.example.bookinghotels.repository.PromotionRoomTypeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    public ResponseEntity<List<PublicOfferDto>> getActiveOffers(
            @RequestParam(required = false) LocalDate checkin,
            @RequestParam(required = false) LocalDate checkout) {
        LocalDate today = LocalDate.now();
        LocalDate stayStart = checkin == null ? today : checkin;
        LocalDate stayEndExclusive = checkout == null ? stayStart.plusDays(1) : checkout;

        List<PublicOfferDto> offers = repository.findAll().stream()
                .filter(this::isUsable)
                .filter(mapping -> isActiveForStay(mapping.getPromotion(), stayStart, stayEndExclusive))
                .sorted(Comparator
                        .comparing((PromotionRoomType mapping) -> mapping.getPromotion().getDiscountPercent(), Comparator.reverseOrder())
                        .thenComparing(PromotionRoomType::getId, Comparator.reverseOrder()))
                .map(PublicOfferDto::new)
                .toList();
        return ResponseEntity.ok(offers);
    }

    private boolean isUsable(PromotionRoomType mapping) {
        if (mapping == null || mapping.getPromotion() == null || mapping.getRoomType() == null) return false;
        Promotion promotion = mapping.getPromotion();
        if (promotion.getDiscountPercent() == null || promotion.getDiscountPercent() <= 0) return false;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startsAt = promotion.getStartDate() == null ? null
                : LocalDateTime.of(promotion.getStartDate(), promotion.getStartTime() == null ? LocalTime.MIDNIGHT : promotion.getStartTime());
        LocalDateTime endsAt = promotion.getEndDate() == null ? null
                : LocalDateTime.of(promotion.getEndDate(), promotion.getEndTime() == null ? LocalTime.of(23, 59) : promotion.getEndTime());
        return (startsAt == null || !now.isBefore(startsAt)) && (endsAt == null || !now.isAfter(endsAt));
    }

    private boolean isActiveForStay(Promotion promotion, LocalDate checkin, LocalDate checkoutExclusive) {
        if (checkoutExclusive == null || !checkoutExclusive.isAfter(checkin)) return false;
        LocalDate lastNight = checkoutExclusive.minusDays(1);
        boolean startsInTime = promotion.getStartDate() == null || !promotion.getStartDate().isAfter(checkin);
        boolean coversLastNight = promotion.getEndDate() == null || !promotion.getEndDate().isBefore(lastNight);
        return startsInTime && coversLastNight;
    }
}
