package org.example.bookinghotels.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "room_img")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomImg {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    @Column(nullable = false)
    private String image;
}