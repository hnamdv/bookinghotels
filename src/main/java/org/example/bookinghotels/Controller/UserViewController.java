package org.example.bookinghotels.Controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserViewController {

    @PreAuthorize("hasAuthority('ROLE_USER')")  // BỎ COMMENT
    @GetMapping("/admin/users")
    public String showUserPage() {
        return "html/staff-html/user";
    }
}