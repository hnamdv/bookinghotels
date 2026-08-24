package org.example.bookinghotels.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;  // ← THÊM IMPORT
import jakarta.persistence.*;
import lombok.*;
import org.example.bookinghotels.listener.ActivityLogListener;

@Entity
@Table(name = "room_img")
@EntityListeners(ActivityLogListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomImg {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ✅ THÊM @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_type_id")
    @JsonIgnore
    private RoomType roomType;

    @Column(nullable = false)
    private String image;
}