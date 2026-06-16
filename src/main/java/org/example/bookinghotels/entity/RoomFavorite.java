package org.example.bookinghotels.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "room_favorite", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "room_type_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomFavorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;
}
