package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.Role;
import org.example.bookinghotels.entity.User;
import java.util.List;

public interface UserService {
    // Lấy danh sách tất cả người dùng (chưa bị xóa)
    List<User> getAllUsers();

    // Tìm chi tiết một người dùng
    User getUserById(Integer id);

    // Thêm mới người dùng
    User createUser(User user);

    // Xóa mềm (Soft delete)
    void softDelete(Integer id);

    // Lấy danh sách tất cả các vai trò (roles)
    List<Role> getAllRoles();
    User updateUser(Integer id, User userDetails);
    void restoreUser(Integer id);
}