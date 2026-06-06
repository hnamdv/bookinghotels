package org.example.bookinghotels.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "\"USER\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", length = 100, nullable = false, unique = true)
    private String username;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "email", length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "delete_at")
    private Boolean deleteAt = false;

    // Nhiều User thuộc về một Role
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;
}