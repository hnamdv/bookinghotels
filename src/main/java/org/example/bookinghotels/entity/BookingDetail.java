package org.example.bookinghotels.entity;

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

    // Booking chính
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    // Loại phòng
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    // Phòng cụ thể (nullable vì có thể assign sau)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = true)
    private Room room;

    // Giá phòng
    @Column(nullable = false)
    private Double price;

    // Giảm giá
    @Column(name = "discount_amount")
    private Double discountAmount = 0.0;

    // Số lượng phòng
    @Column(name = "room_quantity")
    private Integer roomQuantity = 1;

    // Số người lớn
    @Column(name = "adult_count")
    private Integer adultCount = 1;

    // Số trẻ em
    @Column(name = "child_count")
    private Integer childCount = 0;

    // Người đặt
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    // Trạng thái
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    // Food booking
    @OneToMany(mappedBy = "bookingDetail", fetch = FetchType.LAZY)
    private List<BookingFB> bookingFBs = new ArrayList<>();

    // Kiểm tra đã có phòng chưa
    public boolean hasRoom() {
        return room != null;
    }

    // Lấy số phòng
    public String getRoomNumber() {
        return room != null ? room.getRoomNumber() : "Null";
    }
}

