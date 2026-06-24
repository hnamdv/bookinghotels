package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.Invoices;
import org.example.bookinghotels.repository.BookingRepository;
import org.example.bookinghotels.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private BookingRepository bookingRepository;

    // =====================================================
    // DANH SÁCH HÓA ĐƠN (GIỮ NGUYÊN CHỨC NĂNG CŨ)
    // =====================================================
    @GetMapping("/payments")
    public String showInvoicesPage(
            @RequestParam(name = "id", required = false) Integer id,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            Model model) {

        // 1. Lấy danh sách hóa đơn
        List<Invoices> invoicesList = invoiceService.searchInvoices(keyword, status);
        model.addAttribute("invoices", invoicesList);

        // 2. Hiển thị chi tiết hóa đơn
        if (id != null) {
            Invoices selected = invoiceService.findById(id);
            model.addAttribute("selectedInvoice", selected);
        } else {
            if (invoicesList != null && !invoicesList.isEmpty()) {
                model.addAttribute("selectedInvoice", invoicesList.get(0));
            }
        }

        // 3. Giữ filter
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "payments";
    }

    // =====================================================
    // TRANG QR THANH TOÁN MỚI
    // =====================================================
    @GetMapping("/invoice/qr")
    public String showQR(
            @RequestParam Integer bookingId,
            Model model) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

        // lấy invoice theo booking id
        Invoices invoice = invoiceService.findByBookingId(bookingId);

        model.addAttribute("booking", booking);
        model.addAttribute("invoice", invoice);

        return "html/client-html/qr-payment";
    }
}
