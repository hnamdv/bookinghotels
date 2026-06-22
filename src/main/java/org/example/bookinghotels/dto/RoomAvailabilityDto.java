package org.example.bookinghotels.dto;

import java.time.LocalDate;

public class RoomAvailabilityDto {
    private final Integer roomTypeId;
    private final Integer totalRooms;
    private final Long bookedRooms;
    private final Integer availableRooms;
    private final boolean available;
    private final LocalDate checkin;
    private final LocalDate checkout;

    public RoomAvailabilityDto(Integer roomTypeId,
                               Integer totalRooms,
                               Long bookedRooms,
                               Integer availableRooms,
                               LocalDate checkin,
                               LocalDate checkout) {
        this.roomTypeId = roomTypeId;
        this.totalRooms = totalRooms;
        this.bookedRooms = bookedRooms;
        this.availableRooms = availableRooms;
        this.available = availableRooms != null && availableRooms > 0;
        this.checkin = checkin;
        this.checkout = checkout;
    }

    public Integer getRoomTypeId() { return roomTypeId; }
    public Integer getTotalRooms() { return totalRooms; }
    public Long getBookedRooms() { return bookedRooms; }
    public Integer getAvailableRooms() { return availableRooms; }
    public boolean isAvailable() { return available; }
    public LocalDate getCheckin() { return checkin; }
    public LocalDate getCheckout() { return checkout; }
}
