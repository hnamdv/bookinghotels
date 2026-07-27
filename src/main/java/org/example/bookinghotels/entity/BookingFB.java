package org.example.bookinghotels.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.bookinghotels.listener.ActivityLogListener;

@Entity
@Table(name = "booking_f_b")
@EntityListeners(ActivityLogListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingFB {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_detail_id")
    private BookingDetail bookingDetail;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fwb_id")
    private FwB fwb;

    private Integer quantity = 1;

    // Chuyển sang kiểu nguyên thủy double để đảm bảo an toàn, không bao giờ bị null
    @Column(name = "price_at_order", nullable = false)
    private double priceAtOrder;
}