package org.example.bookinghotels.Controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class UserViewController {
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @GetMapping("/admin/users")
    public String showUserPage() {
        // Trỏ đúng vào thư mục con: html -> staff-html -> user
        return "html/staff-html/user";
    }
}