package org.example.bookinghotels.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class UserViewController {

    @GetMapping("/admin/users")
    public String showUserPage() {
        // Trỏ đúng vào thư mục con: html -> staff-html -> user
        return "html/staff-html/user";
    }
}