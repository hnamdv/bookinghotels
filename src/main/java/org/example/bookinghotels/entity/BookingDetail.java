package org.example.bookinghotels.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booking_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id")
    @JsonBackReference("booking-detail-ref")
    private Booking booking;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_type_id")
    @JsonIgnore  // ✅ THÊM VÀO
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = true)
    @JsonIgnore  // ✅ THÊM VÀO
    private Room room;

    private Double price;
    private Double discountAmount = 0.0;
    private Integer roomQuantity = 1;
    private Integer adultCount = 1;
    private Integer childCount = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @JsonIgnore  // ✅ THÊM VÀO
    private User user;

    private String status = "PENDING";
    private Boolean deleteAt = false;

    @OneToMany(mappedBy = "bookingDetail", fetch = FetchType.LAZY)
    @JsonIgnore  // ✅ THÊM VÀO
    private List<BookingFB> bookingFBs = new ArrayList<>();
}