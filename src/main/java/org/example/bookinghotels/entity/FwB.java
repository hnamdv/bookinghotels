package org.example.bookinghotels.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fwb")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FwB {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Giữ nguyên
    private Integer id;

    @Column(name = "booking_fwb_id")
    private Integer bookingFwbId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String status;
}