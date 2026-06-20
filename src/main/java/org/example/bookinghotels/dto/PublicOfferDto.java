package org.example.bookinghotels.dto;

import org.example.bookinghotels.entity.Promotion;
import org.example.bookinghotels.entity.PromotionRoomType;
import org.example.bookinghotels.entity.RoomImg;
import org.example.bookinghotels.entity.RoomType;

import java.time.LocalDate;

public class PublicOfferDto {
    private final Integer mappingId;
    private final Integer roomTypeId;
    private final String roomName;
    private final String hotelName;
    private final String description;
    private final String image;
    private final Double originalPrice;
    private final Double discountPercent;
    private final Double discountedPrice;
    private final String promotionName;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Integer capacity;
    private final String bed;
    private final Double area;

    public PublicOfferDto(PromotionRoomType mapping) {
        Promotion promotion = mapping.getPromotion();
        RoomType roomType = mapping.getRoomType();
        this.mappingId = mapping.getId();
        this.roomTypeId = roomType == null ? null : roomType.getId();
        this.roomName = roomType == null ? null : roomType.getNameType();
        this.hotelName = roomType == null || roomType.getHotels() == null ? null : roomType.getHotels().getName();
        this.description = promotion == null ? null : promotion.getDescription();
        this.originalPrice = roomType == null ? null : roomType.getPrice();
        this.discountPercent = promotion == null || promotion.getDiscountPercent() == null ? 0D : promotion.getDiscountPercent();
        this.discountedPrice = originalPrice == null ? null : Math.max(0D, originalPrice * (100D - discountPercent) / 100D);
        this.promotionName = promotion == null ? null : promotion.getPromotionName();
        this.startDate = promotion == null ? null : promotion.getStartDate();
        this.endDate = promotion == null ? null : promotion.getEndDate();
        this.capacity = roomType == null ? null : roomType.getCapacity();
        this.bed = roomType == null ? null : roomType.getBed();
        this.area = roomType == null ? null : roomType.getArea();
        this.image = resolveImage(roomType);
    }

    private String resolveImage(RoomType roomType) {
        if (roomType == null || roomType.getImages() == null || roomType.getImages().isEmpty()) return null;
        RoomImg first = roomType.getImages().get(0);
        return first == null ? null : first.getImage();
    }

    public Integer getMappingId() { return mappingId; }
    public Integer getRoomTypeId() { return roomTypeId; }
    public String getRoomName() { return roomName; }
    public String getHotelName() { return hotelName; }
    public String getDescription() { return description; }
    public String getImage() { return image; }
    public Double getOriginalPrice() { return originalPrice; }
    public Double getDiscountPercent() { return discountPercent; }
    public Double getDiscountedPrice() { return discountedPrice; }
    public String getPromotionName() { return promotionName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getCapacity() { return capacity; }
    public String getBed() { return bed; }
    public Double getArea() { return area; }
}
