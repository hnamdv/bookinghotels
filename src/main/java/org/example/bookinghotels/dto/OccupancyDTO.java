package org.example.bookinghotels.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OccupancyDTO {

    private long totalRooms;
    private long bookedRooms;
    private double occupancyRate;
}