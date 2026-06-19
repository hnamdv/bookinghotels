package org.example.bookinghotels.dto;

import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.entity.RoomImg;
import org.example.bookinghotels.entity.RoomType;

import java.util.ArrayList;
import java.util.List;

public class RoomTypeSummaryDto {
    private Integer id;
    private String nameType;
    private Double price;
    private Integer capacity;
    private String bed;
    private String description;
    private Boolean hasBathtub;
    private Boolean hasWifi;
    private Boolean hasTv;
    private Boolean hasBalcony;
    private Double area;
    private String bedOptions;
    private Integer totalRooms;
    private Double taxAndFee;
    private String hotelName;
    private String hotelAddress;
    private String thumbnail;
    private List<String> images;

    public RoomTypeSummaryDto() {
    }

    public RoomTypeSummaryDto(RoomType roomType) {
        this.id = roomType.getId();
        this.nameType = roomType.getNameType();
        this.price = roomType.getPrice();
        this.capacity = roomType.getCapacity();
        this.bed = roomType.getBed();
        this.description = roomType.getDescription();
        this.hasBathtub = Boolean.TRUE.equals(roomType.getHasBathtub());
        this.hasWifi = Boolean.TRUE.equals(roomType.getHasWifi());
        this.hasTv = Boolean.TRUE.equals(roomType.getHasTv());
        this.hasBalcony = Boolean.TRUE.equals(roomType.getHasBalcony());
        this.area = roomType.getArea();
        this.bedOptions = roomType.getBedOptions();
        this.totalRooms = roomType.getTotalRooms();
        this.taxAndFee = roomType.getTaxAndFee();

        Hotels hotel = roomType.getHotels();
        if (hotel != null) {
            this.hotelName = hotel.getName();
            this.hotelAddress = hotel.getAddress();
        }

        this.images = new ArrayList<>();
        if (roomType.getImages() != null) {
            for (RoomImg img : roomType.getImages()) {
                if (img != null && img.getImage() != null && !img.getImage().isBlank()) {
                    this.images.add(img.getImage());
                }
            }
        }
        this.thumbnail = this.images.isEmpty() ? "/img" : this.images.get(0);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNameType() { return nameType; }
    public void setNameType(String nameType) { this.nameType = nameType; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getBed() { return bed; }
    public void setBed(String bed) { this.bed = bed; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getHasBathtub() { return hasBathtub; }
    public void setHasBathtub(Boolean hasBathtub) { this.hasBathtub = hasBathtub; }
    public Boolean getHasWifi() { return hasWifi; }
    public void setHasWifi(Boolean hasWifi) { this.hasWifi = hasWifi; }
    public Boolean getHasTv() { return hasTv; }
    public void setHasTv(Boolean hasTv) { this.hasTv = hasTv; }
    public Boolean getHasBalcony() { return hasBalcony; }
    public void setHasBalcony(Boolean hasBalcony) { this.hasBalcony = hasBalcony; }
    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }
    public String getBedOptions() { return bedOptions; }
    public void setBedOptions(String bedOptions) { this.bedOptions = bedOptions; }
    public Integer getTotalRooms() { return totalRooms; }
    public void setTotalRooms(Integer totalRooms) { this.totalRooms = totalRooms; }
    public Double getTaxAndFee() { return taxAndFee; }
    public void setTaxAndFee(Double taxAndFee) { this.taxAndFee = taxAndFee; }
    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }
    public String getHotelAddress() { return hotelAddress; }
    public void setHotelAddress(String hotelAddress) { this.hotelAddress = hotelAddress; }
    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
}
