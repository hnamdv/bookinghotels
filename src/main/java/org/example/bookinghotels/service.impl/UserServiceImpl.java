package org.example.bookinghotels.service.impl;

import org.example.bookinghotels.entity.Role;
import org.example.bookinghotels.entity.User;
import org.example.bookinghotels.repository.RoleRepository;
import org.example.bookinghotels.repository.UserRepository;
import org.example.bookinghotels.service.ActivityLogService; // <-- Thêm service log
import org.example.bookinghotels.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ActivityLogService activityLogService; // <-- Inject ActivityLogService

    @Override
    public User createUser(User user) {
        // 1. Luôn luôn băm mật khẩu khi tạo mới
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        User savedUser = userRepository.save(user);

        // Ghi log tạo mới user an toàn
        activityLogService.log("CREATE", "USER", "Tạo mới user: " + savedUser.getUsername(), savedUser);

        return savedUser;
    }

    @Override
    public User updateUser(Integer id, User userDetails) {
        User user = getUserById(id);
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());

        // Xử lý nhiều quyền
        if (userDetails.getRoles() != null) {
            user.setRoles(userDetails.getRoles());
        }

        // 2. Chỉ băm và cập nhật mật khẩu nếu có mật khẩu mới gửi lên
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        User updatedUser = userRepository.save(user);

        // Ghi log cập nhật user an toàn
        activityLogService.log("UPDATE", "USER", "Cập nhật user: " + updatedUser.getUsername(), updatedUser);

        return updatedUser;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
    }

    @Override
    public void softDelete(Integer id) {
        User user = getUserById(id);
        user.setDeleteAt(true);
        userRepository.save(user);

        // Ghi log xóa mềm an toàn
        activityLogService.log("DELETE", "USER", "Soft delete user: " + user.getUsername(), user);
    }

    @Override
    public void restoreUser(Integer id) {
        User user = getUserById(id);
        user.setDeleteAt(false);
        userRepository.save(user);

        // Ghi log khôi phục user an toàn
        activityLogService.log("RESTORE", "USER", "Khôi phục user: " + user.getUsername(), user);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}