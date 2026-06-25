package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.Promotion;
import org.example.bookinghotels.entity.PromotionRoomType;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.PromotionRoomTypeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;

@Service
public class PromotionPricingService {

    private final PromotionRoomTypeRepository promotionRoomTypeRepository;

    public PromotionPricingService(PromotionRoomTypeRepository promotionRoomTypeRepository) {
        this.promotionRoomTypeRepository = promotionRoomTypeRepository;
    }

    public PriceQuote quote(RoomType roomType, LocalDate checkin, LocalDate checkout) {
        if (roomType == null || roomType.getId() == null) {
            return PriceQuote.noPromotion(0D);
        }

        double original = roomType.getPrice() == null ? 0D : roomType.getPrice();
        LocalDateTime now = LocalDateTime.now();

        Promotion promotion = promotionRoomTypeRepository.findByRoomTypeId(roomType.getId()).stream()
                .map(PromotionRoomType::getPromotion)
                .filter(p -> p != null && isActiveNow(p, now) && coversStay(p, checkin, checkout))
                .max(Comparator.comparingDouble(p -> p.getDiscountPercent() == null ? 0D : p.getDiscountPercent()))
                .orElse(null);

        if (promotion == null) {
            return PriceQuote.noPromotion(original);
        }

        double percent = Math.max(0D, Math.min(100D,
                promotion.getDiscountPercent() == null ? 0D : promotion.getDiscountPercent()));
        double discounted = Math.max(0D, original * (100D - percent) / 100D);
        LocalDateTime endAt = LocalDateTime.of(
                promotion.getEndDate() == null ? LocalDate.MAX : promotion.getEndDate(),
                promotion.getEndTime() == null ? LocalTime.of(23, 59, 59) : promotion.getEndTime()
        );

        return new PriceQuote(
                original,
                discounted,
                original - discounted,
                percent,
                promotion.getPromotionName(),
                endAt,
                true
        );
    }

    private boolean isActiveNow(Promotion promotion, LocalDateTime now) {
        LocalDate startDate = promotion.getStartDate() == null ? LocalDate.MIN : promotion.getStartDate();
        LocalDate endDate = promotion.getEndDate() == null ? LocalDate.MAX : promotion.getEndDate();
        LocalTime startTime = promotion.getStartTime() == null ? LocalTime.MIN : promotion.getStartTime();
        LocalTime endTime = promotion.getEndTime() == null ? LocalTime.of(23, 59, 59) : promotion.getEndTime();
        LocalDateTime start = LocalDateTime.of(startDate, startTime);
        LocalDateTime end = LocalDateTime.of(endDate, endTime);
        return !now.isBefore(start) && !now.isAfter(end);
    }

    private boolean coversStay(Promotion promotion, LocalDate checkin, LocalDate checkout) {
        if (checkin == null || checkout == null) return true;
        LocalDate start = promotion.getStartDate() == null ? LocalDate.MIN : promotion.getStartDate();
        LocalDate end = promotion.getEndDate() == null ? LocalDate.MAX : promotion.getEndDate();
        LocalDate lastNight = checkout.minusDays(1);
        return !checkin.isBefore(start) && !lastNight.isAfter(end);
    }

    public record PriceQuote(
            double originalNightlyPrice,
            double effectiveNightlyPrice,
            double discountPerNight,
            double discountPercent,
            String promotionName,
            LocalDateTime promotionEndAt,
            boolean promoted
    ) {
        public static PriceQuote noPromotion(double original) {
            return new PriceQuote(original, original, 0D, 0D, null, null, false);
        }
    }
}
