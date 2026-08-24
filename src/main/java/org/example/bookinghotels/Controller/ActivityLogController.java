package org.example.bookinghotels.Controller;

import lombok.RequiredArgsConstructor;
import org.example.bookinghotels.entity.ActivityLog;
import org.example.bookinghotels.service.SystemManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ActivityLogController {

    private final SystemManagementService systemManagementService;
    private static final int PAGE_SIZE = 20;

    @GetMapping("/logs")
    public String logs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Sort sortObj = Sort.by(Sort.Direction.fromString(dir), sort);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, sortObj);

        Page<ActivityLog> result = systemManagementService.searchLogs(
                keyword,
                action,
                module,
                fromDate,
                toDate,
                pageable
        );

        model.addAttribute("logs", result.getContent());
        model.addAttribute("currentPage", result.getNumber());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("sortField", sort);
        model.addAttribute("sortDir", dir);

        return "html/admin-html/logs";
    }
}