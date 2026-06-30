package org.example.bookinghotels.Controller;

import lombok.RequiredArgsConstructor;
import org.example.bookinghotels.dto.OccupancyDTO;
import org.example.bookinghotels.dto.RevenueDTO;
import org.example.bookinghotels.repository.InvoicesRepository;
import org.example.bookinghotels.service.SystemManagementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final SystemManagementService systemManagementService;
    private final InvoicesRepository invoicesRepository;

    @GetMapping("/revenue/day")
    public List<RevenueDTO> revenueDay() {
        return systemManagementService.getRevenueByDay();
    }

    @GetMapping("/revenue/month")
    public List<RevenueDTO> revenueMonth() {
        return systemManagementService.getRevenueByMonth();
    }

    @GetMapping("/revenue/year")
    public List<RevenueDTO> revenueYear() {
        return systemManagementService.getRevenueByYear();
    }

    @GetMapping("/occupancy")
    public OccupancyDTO occupancy() {
        return systemManagementService.getOccupancy();
    }

    @GetMapping("/invoice-count")
    public long invoiceCount() {
        return invoicesRepository.count();
    }

    @GetMapping("/invoice-all")
    public Object invoiceAll() {
        return invoicesRepository.findAll();
    }
}