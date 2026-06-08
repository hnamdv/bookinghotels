package org.example.bookinghotels.service;
import org.example.bookinghotels.entity.User;
import org.example.bookinghotels.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    private final Map<String, Integer> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPT = 5;

    public String login(String email, String rawPassword) {
        // 1. Tìm user bằng Email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        // 2. Kiểm tra tài khoản đã bị khóa chưa
        if (user.getDeleteAt() != null && user.getDeleteAt()) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
        }

        // 3. Kiểm tra mật khẩu (So sánh thô - không băm)
        if (!user.getPassword().equals(rawPassword)) {
            // Nếu sai, tăng đếm số lần sai
            int attempts = loginAttempts.getOrDefault(email, 0) + 1;
            loginAttempts.put(email, attempts);

            if (attempts >= MAX_ATTEMPT) {
                user.setDeleteAt(true); // Khóa tài khoản
                userRepository.save(user);
                throw new RuntimeException("Tài khoản đã bị khóa do nhập sai 5 lần!");
            }
            throw new RuntimeException("Sai mật khẩu! Còn " + (MAX_ATTEMPT - attempts) + " lần thử.");
        }

        // 4. Nếu đăng nhập thành công
        loginAttempts.remove(email); // Reset số lần đếm
        return "SUCCESS_TOKEN_123";  // Sau này thay bằng logic JWT của bạn
    }
}