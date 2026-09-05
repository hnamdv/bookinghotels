package org.example.bookinghotels.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Phòng yêu thích được lưu phía server.
 * ownerKey dùng USER:<username> khi đã đăng nhập, hoặc SESSION:<JSESSIONID> cho khách public.
 * Nhờ vậy Favorites không còn phụ thuộc localStorage/JavaScript.
 */
@Entity
@Table(
        name = "favorite_room",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorite_owner_room_type",
                columnNames = {"owner_key", "room_type_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "owner_key", nullable = false, length = 220)
    private String ownerKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
