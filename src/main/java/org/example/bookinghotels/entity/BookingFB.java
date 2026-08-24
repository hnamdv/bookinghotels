package org.example.bookinghotels.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private BookingDetail bookingDetail;

    // ✅ THÊM @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fwb_id")
    @JsonIgnore
    private FwB fwb;

    private Integer quantity = 1;

    @Column(name = "price_at_order", nullable = false)
    private Double priceAtOrder;
}