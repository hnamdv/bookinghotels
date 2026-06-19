package org.example.bookinghotels.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String welcome() {
        return "Chào mừng đến với hệ thống đặt phòng FeelHome!";
    }

}