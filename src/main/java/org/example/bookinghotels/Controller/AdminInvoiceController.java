package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminInvoiceController {

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @GetMapping("/invoice-list")
    public String viewInvoicesPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            Model model) {

        List<BookingDetail> allDetails = bookingDetailRepository.findAllWithDetails();

        List<BookingDetail> filteredInvoices = allDetails.stream()
                .filter(bd -> bd.getBooking() != null)
                .collect(Collectors.toList());

        if (status != null && !status.trim().isEmpty()) {
            filteredInvoices = filteredInvoices.stream()
                    .filter(bd -> status.equalsIgnoreCase(bd.getStatus()))
                    .collect(Collectors.toList());
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String finalKeyword = keyword.toLowerCase().trim();
            filteredInvoices = filteredInvoices.stream()
                    .filter(bd -> (bd.getBooking().getName() != null && bd.getBooking().getName().toLowerCase().contains(finalKeyword))
                            || (bd.getBooking().getPhone() != null && bd.getBooking().getPhone().contains(finalKeyword))
                            || (bd.getBooking().getEmail() != null && bd.getBooking().getEmail().toLowerCase().contains(finalKeyword)))
                    .collect(Collectors.toList());
        }

        model.addAttribute("invoices", filteredInvoices);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "html/admin-html/invoice-list";
    }
}