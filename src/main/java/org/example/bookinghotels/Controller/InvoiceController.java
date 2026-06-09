package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Invoices;
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

    @GetMapping("/payments")
    public String showInvoicesPage(
            // 🛠️ Đổi từ Long sang Integer cho đồng bộ hoàn toàn với Entity và Service
            @RequestParam(name = "id", required = false) Integer id,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            Model model) {

        // 1. Lấy danh sách hóa đơn theo từ khóa tìm kiếm và trạng thái lọc từ DB
        List<Invoices> invoicesList = invoiceService.searchInvoices(keyword, status);
        model.addAttribute("invoices", invoicesList);

        // 2. Xử lý hiển thị vùng "Chi tiết hóa đơn" và "Lịch sử ghi nhận" bên dưới
        if (id != null) {
            // Khách hàng chủ động bấm vào icon mắt để xem một hóa đơn cụ thể (Tham số truyền vào bây giờ là Integer)
            Invoices selected = invoiceService.findById(id);
            model.addAttribute("selectedInvoice", selected);
        } else {
            // Mặc định khi vừa vào trang, nếu danh sách có dữ liệu thì lấy phần tử đầu tiên hiển thị luôn cho đẹp giao diện
            if (invoicesList != null && !invoicesList.isEmpty()) {
                model.addAttribute("selectedInvoice", invoicesList.get(0));
            }
        }

        // 3. Giữ lại giá trị trên các ô input/select để không bị mất dữ liệu sau khi bấm nút "Lọc"
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        // Trả về file giao diện payments.html nằm trong thư mục templates
        return "payments";
    }
}