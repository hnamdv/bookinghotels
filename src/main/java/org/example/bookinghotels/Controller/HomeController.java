package org.example.bookinghotels.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home() {
        return "layout.html";
    }

    @GetMapping("/home/layout")
    public String layout() {
        return "layout.html";
    }
}