package org.example.bookinghotels.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {

    // Khi người dùng truy cập /admin, nó sẽ load trang dashboard
    @GetMapping("/admin")
    public String showDashboard() {
        return "dashboard"; // Trỏ đến file dashboard.html bạn vừa tạo
    }
}