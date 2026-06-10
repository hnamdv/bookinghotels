package org.example.bookinghotels.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @ManyToMany(mappedBy = "roles")
    @JsonIgnore
    // Role không phải là chủ sở hữu, User mới là chủ
    private List<User> users;
}