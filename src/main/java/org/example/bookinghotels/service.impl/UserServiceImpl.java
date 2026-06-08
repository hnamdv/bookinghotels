package org.example.bookinghotels.service.impl;


import org.example.bookinghotels.entity.Role;
import org.example.bookinghotels.entity.User;
import org.example.bookinghotels.repository.RoleRepository;
import org.example.bookinghotels.repository.UserRepository;
import org.example.bookinghotels.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class UserServiceImpl implements UserService {
    @Override
    public User updateUser(Integer id, User userDetails) {
        User user = getUserById(id);
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        // Cập nhật các trường khác nếu cần
        return userRepository.save(user);
    }
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    @Override
    public List<User> getAllUsers() {
        // Chỉ lấy user chưa bị xóa (deleteAt = false)
        return userRepository.findAll().stream()
                .filter(u -> !u.getDeleteAt())
                .collect(Collectors.toList());
    }

    @Override
    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
    }

    @Override
    public User createUser(User user) {
        // Lưu ý: Trước khi save, hãy đảm bảo password đã được băm (hash)
        return userRepository.save(user);
    }

    @Override
    public void softDelete(Integer id) {
        User user = getUserById(id);
        user.setDeleteAt(true); // Đánh dấu xóa mềm
        userRepository.save(user);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}