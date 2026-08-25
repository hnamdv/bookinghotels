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

<<<<<<< HEAD
        Page<ActivityLog> result = systemManagementService.searchLogs(
                keyword,
                action,
                module,
                fromDate,
                toDate,
                pageable
        );
=======
        System.out.println("SO LOG = " + logs.size());
>>>>>>> f39808f342c6a3f7c62a202f763f3e18dc5d14d9

        model.addAttribute("logs", logs);

        return "html/admin-html/logs";
    }

}