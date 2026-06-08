package org.example.bookinghotels.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller // Dùng @Controller (không phải @RestController)
public class LoginViewController {
        @GetMapping("/login")
        public String showLoginPage() {
            return "Login"; // Trả về đúng tên file Login.html
        }
    }
