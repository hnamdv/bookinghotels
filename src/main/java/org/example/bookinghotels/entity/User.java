package org.example.bookinghotels.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.example.bookinghotels.listener.ActivityLogListener;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "\"USER\"")
@EntityListeners(ActivityLogListener.class)
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
    @JsonProperty("deleteAt") // Đảm bảo tên này khớp với u.deleteAt trong JS
    private Boolean deleteAt;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

}