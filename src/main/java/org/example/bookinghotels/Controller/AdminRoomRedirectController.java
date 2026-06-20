package org.example.bookinghotels.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminRoomRedirectController {
    @GetMapping("/admin/rooms")
    public String redirect() {
        return "redirect:/admin/room-types";
    }
}
