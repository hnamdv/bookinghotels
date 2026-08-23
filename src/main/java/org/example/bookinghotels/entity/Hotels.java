package org.example.bookinghotels.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;  // ← THÊM IMPORT
import jakarta.persistence.*;
import lombok.*;
import org.example.bookinghotels.listener.ActivityLogListener;

import java.util.List;

@Entity
@Table(name = "hotels")
@EntityListeners(ActivityLogListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Hotels {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    private String phone;
    private String email;
    private String address;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String map;

    private String logo;

    @Column(unique = true)
    private String slug;

    private String thumbnail;

    // ✅ THÊM @JsonIgnore
    @OneToMany(mappedBy = "hotels", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<RoomType> roomTypes;
}