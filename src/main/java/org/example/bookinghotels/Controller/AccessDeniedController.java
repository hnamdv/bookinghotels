package org.example.bookinghotels.Controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccessDeniedController {

    @GetMapping("/access-denied")
    public String accessDenied(HttpSession session, Model model) {
        Object savedBackUrl = session.getAttribute("ACCESS_DENIED_BACK_URL");
        session.removeAttribute("ACCESS_DENIED_BACK_URL");

        String backUrl = savedBackUrl instanceof String ? (String) savedBackUrl : "/admin";
        if (!backUrl.startsWith("/") || backUrl.startsWith("//") || backUrl.startsWith("/access-denied")) {
            backUrl = "/admin";
        }

        model.addAttribute("backUrl", backUrl);
        return "access-denied";
    }
}
