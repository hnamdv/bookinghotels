package org.example.bookinghotels.service;



import org.example.bookinghotels.entity.Role;
import org.example.bookinghotels.entity.User;
import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    User getUserById(Integer id);
    User createUser(User user);
    List<Role> getAllRoles();

}