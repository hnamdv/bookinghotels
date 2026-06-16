package org.example.bookinghotels.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    @Column(unique = true)
    private String slug;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    private String thumbnail;
}