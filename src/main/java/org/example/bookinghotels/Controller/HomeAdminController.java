package org.example.bookinghotels.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeAdminController {

    // Đổi từ "/" sang "/admin" để tránh trùng lặp
    @GetMapping("/admin")
    public String home() {
        return "homeadmin"; // Giữ nguyên để trả về file homeadmin.html
    }
}