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

    @GetMapping("/invoices")
    public String viewInvoicesPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        // 1. Lấy toàn bộ danh sách đặt phòng có kèm chi tiết giống hệt trang Dashboard
        List<BookingDetail> allDetails = bookingDetailRepository.findAllWithDetails();

        // 2. Tạo danh sách hóa đơn từ dữ liệu đặt phòng
        List<BookingDetail> paidInvoices = allDetails.stream()
                .filter(bd -> bd.getBooking() != null)
                .collect(Collectors.toList());

        // 3. Xử lý tìm kiếm theo từ khóa (Đã sửa đổi tên hàm getter cho khớp Entity Booking)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String finalKeyword = keyword.toLowerCase().trim();
            paidInvoices = paidInvoices.stream()
                    .filter(bd -> (bd.getBooking().getName() != null && bd.getBooking().getName().toLowerCase().contains(finalKeyword))
                            || (bd.getBooking().getPhone() != null && bd.getBooking().getPhone().contains(finalKeyword))
                            || (bd.getBooking().getEmail() != null && bd.getBooking().getEmail().toLowerCase().contains(finalKeyword)))
                    .collect(Collectors.toList());
        }

        // Đẩy dữ liệu ra ngoài giao diện
        model.addAttribute("invoices", paidInvoices);
        model.addAttribute("keyword", keyword);

        return "html/admin-html/invoice-list";
    }
}