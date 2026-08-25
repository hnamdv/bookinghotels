package org.example.bookinghotels.Controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class HomeAdminController {

    @GetMapping("/admin")
    public String home(Authentication authentication) {
        // Nếu tài khoản chỉ có đúng 1 quyền chức năng, đưa thẳng tới đúng module đó.
        // Tài khoản có nhiều quyền giữ trang trung tâm để chọn module từ sidebar.
        if (authentication != null && authentication.isAuthenticated()) {
            Set<String> roles = authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.toSet());

            if (roles.size() == 1) {
                String role = roles.iterator().next();
                return switch (role) {
                    case "ROLE_USER" -> "redirect:/admin/users";
                    case "ROLE_BOOKING" -> "redirect:/admin/bookings";
                    case "ROLE_ROOM" -> "redirect:/admin/rooms";
                    case "ROLE_HOTEL" -> "redirect:/admin/hotels";
                    case "ROLE_PROMOTION" -> "redirect:/staff/promotions";
                    case "ROLE_FWB" -> "redirect:/staff/fwb/management";
                    case "ROLE_IMG" -> "redirect:/admin/media";
                    default -> "homeadmin";
                };
            }
        }
        return "homeadmin";
    }
}
