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
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = true)
    private Room room;

    @Column(nullable = false)
    private Double price;

    @Column(name = "discount_amount")
    private Double discountAmount = 0.0;

    @Column(name = "room_quantity")
    private Integer roomQuantity = 1;

    @Column(name = "adult_count")
    private Integer adultCount = 1;

    @Column(name = "child_count")
    private Integer childCount = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "delete_at")
    private Boolean deleteAt = false; // Thêm trường xóa mềm

    @OneToMany(mappedBy = "bookingDetail", fetch = FetchType.LAZY)
    private List<BookingFB> bookingFBs = new ArrayList<>();

    public boolean hasRoom() {
        return room != null;
    }

    public String getRoomNumber() {
        return room != null ? room.getRoomNumber() : "Null";
    }
}