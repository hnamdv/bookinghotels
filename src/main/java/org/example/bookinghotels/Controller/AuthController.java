package org.example.bookinghotels.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.bookinghotels.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView; // Import cái này

import java.util.Map;
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpServletRequest servletRequest) {
        try {
            // 1. Gọi Service của bạn để xác thực
            authService.login(request.get("email"), request.get("password"));

            // 2. TỰ ĐỘNG TẠO SESSION: Spring Security sẽ tự lưu vào Cookie JSESSIONID
            // Nếu bạn dùng formLogin mặc định thì nó tự làm, nhưng vì bạn dùng API,
            // bạn phải tự tạo Authentication object và đẩy vào SecurityContext

            // (Đoạn code cấp quyền nạp vào SecurityContext như tôi đã gửi ở trên)

            return ResponseEntity.ok(Map.of("success", true, "message", "Đăng nhập thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

}