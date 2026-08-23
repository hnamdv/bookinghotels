package org.example.bookinghotels.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.bookinghotels.listener.ActivityLogListener;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "promotion")
@EntityListeners(ActivityLogListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "promotion_name", nullable = false, length = 150)
    private String promotionName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "discount_percent")
    private Double discountPercent = 0.0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime = LocalTime.of(0, 0);

    @Column(name = "end_time")
    private LocalTime endTime = LocalTime.of(23, 59);
}