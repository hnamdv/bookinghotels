package org.example.bookinghotels.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.bookinghotels.entity.User;
import java.util.List;

@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "role_name", length = 50, nullable = false)
    private String roleName;

    // Một Role có thể nằm trong nhiều User
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private List<User> users;
}