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

    @GetMapping("/roomdetail/{id}")
    public String roomDetailPath() {
        return "html/client-html/roomdetail";
    }

    @GetMapping("/roomdetail")
    public String roomDetailQuery(@org.springframework.web.bind.annotation.RequestParam(required = false) Integer id) {
        return id == null ? "redirect:/home" : "redirect:/roomdetail/" + id;
    }

    @GetMapping({"/room-detail", "/room-detail.html"})
    public String oldRoomDetailAlias(@org.springframework.web.bind.annotation.RequestParam(required = false) Integer id) {
        return id == null ? "redirect:/home" : "redirect:/roomdetail/" + id;
    }

    @GetMapping({"/", "/layout", "/layout.html"})
    public String homeAliases() {
        return "redirect:/home";
    }
}
