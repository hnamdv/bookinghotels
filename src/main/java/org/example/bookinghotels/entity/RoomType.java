package org.example.bookinghotels.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "room_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hotels_id")
    private Hotels hotels;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "name_type", nullable = false, length = 150)
    private String nameType;

    private String bed;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "has_bathtub")
    private Boolean hasBathtub = false;

    @Column(name = "has_wifi")
    private Boolean hasWifi = false;

    @Column(name = "has_tv")
    private Boolean hasTv = false;

    @Column(name = "has_balcony")
    private Boolean hasBalcony = false;

    private Double area;

    @Column(name = "bed_options", columnDefinition = "TEXT")
    private String bedOptions;

    @Column(name = "total_rooms")
    private Integer totalRooms = 1;

    @Column(name = "tax_and_fee")
    private Double taxAndFee = 0.0;

    @OneToMany(mappedBy = "roomType", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Room> rooms;

    @OneToMany(mappedBy = "roomType", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<RoomImg> images;
    // Mở file RoomType.java lên nha Hải
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hotel_id")
    @JsonIgnore // Thêm thằng này vào, nó sẽ chặn không cho in ngược về khách sạn, hết sạch lặp và gạch đỏ!
    private Hotels hotels;
}