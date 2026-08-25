package org.example.bookinghotels.service.impl;

import org.example.bookinghotels.entity.Role;
import org.example.bookinghotels.entity.User;
import org.example.bookinghotels.repository.RoleRepository;
import org.example.bookinghotels.repository.UserRepository;
import org.example.bookinghotels.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // Thêm import này
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;// 1. Inject PasswordEncoder

    @Override
    public User createUser(User user) {
        // Luôn băm mật khẩu khi tạo mới.
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        user.setRoles(resolveManagedRoles(user.getRoles()));
        return userRepository.save(user);
    }

    @Override
    public User updateUser(Integer id, User userDetails) {
        User user = getUserById(id);
        user.setUsername(userDetails.getUsername());
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());

        // Chỉ nhận các Role thật có trong DB; tránh lưu object Role rời hoặc roleName giả.
        if (userDetails.getRoles() != null) {
            user.setRoles(resolveManagedRoles(userDetails.getRoles()));
        }

        // 3. Chỉ băm và cập nhật mật khẩu nếu có mật khẩu mới gửi lên
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        return userRepository.save(user);
    }

    private Set<Role> resolveManagedRoles(Set<Role> requestedRoles) {
        Set<Role> managedRoles = new LinkedHashSet<>();
        if (requestedRoles == null) {
            return managedRoles;
        }
        for (Role requested : requestedRoles) {
            if (requested == null) continue;
            Role managed = null;
            if (requested.getId() != null) {
                managed = roleRepository.findById(requested.getId())
                        .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + requested.getId()));
            } else if (requested.getRoleName() != null && !requested.getRoleName().isBlank()) {
                managed = roleRepository.findByRoleName(requested.getRoleName().trim())
                        .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + requested.getRoleName()));
            }
            if (managed != null) managedRoles.add(managed);
        }
        return managedRoles;
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
    }

    @Override
    public void restoreUser(Integer id) {
        User user = getUserById(id);
        user.setDeleteAt(false);
        userRepository.save(user);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}