package org.example.bookinghotels.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String home() {
        return "forward:/client-home.html";
    }

    @GetMapping("/offers")
    public String offers() {
        return "forward:/offers.html";
    }

    @GetMapping({"/", "/layout", "/layout.html"})
    public String homeAliases() {
        return "redirect:/home";
    }
}
