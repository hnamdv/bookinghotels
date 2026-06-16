package org.example.bookinghotels.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "promotion")
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
}