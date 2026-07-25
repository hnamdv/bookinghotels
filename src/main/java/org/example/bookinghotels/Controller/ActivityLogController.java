package org.example.bookinghotels.Controller;

import lombok.RequiredArgsConstructor;
import org.example.bookinghotels.service.SystemManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ActivityLogController {

    private final SystemManagementService systemManagementService;

    @GetMapping("/logs")
    public String logs(Model model) {

        var logs = systemManagementService.getAllLogs();

        System.out.println("SO LOG = " + logs.size());

        model.addAttribute("logs", logs);

        return "html/admin-html/logs";
    }

}