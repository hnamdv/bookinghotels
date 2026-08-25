package org.example.bookinghotels.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.bookinghotels.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @GetMapping("/api/auth/logout")
    public String logout(HttpSession session) {
        // 1. Xóa toàn bộ dữ liệu trong session
        session.invalidate();

        // 2. Clear thông tin bảo mật của Spring Security
        SecurityContextHolder.clearContext();

        // 3. Chuyển hướng về trang đăng nhập
        return "redirect:/login"; // Hoặc đường dẫn file html trang đăng nhập của bạn
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        // Log để xem dữ liệu có đến được controller không
        System.out.println("DEBUG: Request received: " + request);

        String loginId = request.get("loginId");
        String password = request.get("password");

        // Kiểm tra dữ liệu bị null
        if (loginId == null || loginId.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", "Vui lòng nhập Username hoặc Email!"));
        }

        try {
            String result = authService.login(loginId, password);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đăng nhập thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    @GetMapping("/login")
    public String loginPage() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        // Nếu ĐÃ đăng nhập (không phải null và không phải anonymous) -> Đẩy thẳng vào trang quản trị / trang chính
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/admin"; // Trang trung tâm, menu sẽ hiển thị theo quyền thực tế
        }

        // Nếu CHƯA đăng nhập -> Cho hiển thị giao diện login.html bình thường
        return "login";
    }
}