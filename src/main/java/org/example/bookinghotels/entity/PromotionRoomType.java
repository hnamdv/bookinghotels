package org.example.bookinghotels.entity;



import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "promotion_room_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;
}