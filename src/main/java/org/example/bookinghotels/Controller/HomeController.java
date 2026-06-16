package org.example.bookinghotels.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home() {
        return "redirect:/layout.html";
    }

    @GetMapping("/home/layout")
    public String layout() {
        return "redirect:/layout.html";
    }
}