package org.example.bookinghotels.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;  // ← THÊM IMPORT NÀY
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    private Boolean gender;

    @Column(name = "booking_date")
    private LocalDateTime bookingDate = LocalDateTime.now();

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    @Column(name = "checkout_date", nullable = false)
    private LocalDate checkoutDate;

    @Column(name = "actual_checkin")
    private LocalDateTime actualCheckin;

    @Column(name = "actual_checkout")
    private LocalDateTime actualCheckout;

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY)
    @JsonIgnore  // ← THÊM DÒNG NÀY
    private List<BookingDetail> bookingDetails = new ArrayList<>();
}