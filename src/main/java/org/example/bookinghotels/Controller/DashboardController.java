package org.example.bookinghotels.Controller;

import lombok.RequiredArgsConstructor;
import org.example.bookinghotels.repository.InvoicesRepository;
import org.example.bookinghotels.service.SystemManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final SystemManagementService systemManagementService;
    private final InvoicesRepository invoicesRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        model.addAttribute("occupancy", systemManagementService.getOccupancy());

        model.addAttribute("revenueDay", systemManagementService.getRevenueByDay());
        model.addAttribute("revenueMonth", systemManagementService.getRevenueByMonth());

        model.addAttribute("invoiceCount", invoicesRepository.count());

        return "html/staff-html/dashboard";
    }
}