package org.example.bookinghotels.Controller;

import lombok.RequiredArgsConstructor;
import org.example.bookinghotels.repository.InvoicesRepository;
import org.example.bookinghotels.service.SystemManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class DashboardController {

    private final SystemManagementService systemManagementService;
    private final InvoicesRepository invoicesRepository;

    @GetMapping("/thongke")
    public String dashboard(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model) {

        LocalDate from = (fromDate != null && !fromDate.isBlank()) ? LocalDate.parse(fromDate) : null;
        LocalDate to = (toDate != null && !toDate.isBlank()) ? LocalDate.parse(toDate) : null;

        boolean hasDayFilter = from != null || to != null;
        boolean hasMonthFilter = month != null || year != null;

        model.addAttribute("occupancy", systemManagementService.getOccupancy());

        model.addAttribute(
                "revenueDay",
                hasDayFilter
                        ? systemManagementService.getRevenueByDay(from, to)
                        : systemManagementService.getRevenueByDay()
        );

        model.addAttribute(
                "revenueMonth",
                hasMonthFilter
                        ? systemManagementService.getRevenueByMonth(month, year)
                        : systemManagementService.getRevenueByMonth()
        );

        model.addAttribute("revenueYear", systemManagementService.getRevenueByYear());

        model.addAttribute(
                "invoiceCount",
                hasDayFilter
                        ? systemManagementService.getInvoiceCount(from, to)
                        : invoicesRepository.count()
        );

        return "html/staff-html/thongke";
    }
}