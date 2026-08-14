package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.Invoices;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.example.bookinghotels.repository.BookingRepository;
import org.example.bookinghotels.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @GetMapping("/admin/invoice-list-custom")
    public String showInvoicesPage(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            Model model) {

        List<BookingDetail> invoices;

        boolean hasKeyword = (keyword != null && !keyword.trim().isEmpty());
        boolean hasStatus = (status != null && !status.trim().isEmpty());

        if (hasKeyword || hasStatus) {
            String kw = hasKeyword ? "%" + keyword.trim() + "%" : "%";
            String st = hasStatus ? status : "";
            invoices = bookingDetailRepository.searchBookingDetails(kw, st);
        } else {
            invoices = bookingDetailRepository.findAllWithDetails();
        }

        model.addAttribute("invoices", invoices);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "html/admin-html/invoice-list";
    }

    @PostMapping("/admin/invoice/delete/{id}")
    public String deleteInvoice(@PathVariable Integer id) {
        // Thay vì deleteById (xóa cứng), ta thực hiện đánh dấu xóa mềm
        bookingDetailRepository.findById(id).ifPresent(detail -> {
            detail.setDeleteAt(true);
            bookingDetailRepository.save(detail);
        });
        return "redirect:/admin/invoice-list-custom";
    }

    @GetMapping("/invoice/qr")
    public String showQR(@RequestParam Integer bookingId, Model model) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

        Invoices invoice = invoiceService.findByBookingId(bookingId);
        model.addAttribute("booking", booking);
        model.addAttribute("invoice", invoice);
        return "html/client-html/qr-payment";
    }
}