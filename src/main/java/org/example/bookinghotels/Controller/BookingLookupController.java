package org.example.bookinghotels.Controller;

import jakarta.servlet.http.HttpSession;
import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Random;

@Controller
public class BookingLookupController {

    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping("/booking/lookup")
    public String lookupBooking(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String captchaInput,
            HttpSession session,
            Model model) {

        // 1. Tạo câu hỏi Captcha mới nếu chưa có trong Session
        Integer num1 = (Integer) session.getAttribute("captchaNum1");
        Integer num2 = (Integer) session.getAttribute("captchaNum2");

        if (num1 == null || num2 == null) {
            Random random = new Random();
            num1 = random.nextInt(9) + 1; // Số từ 1 đến 9
            num2 = random.nextInt(9) + 1;
            session.setAttribute("captchaNum1", num1);
            session.setAttribute("captchaNum2", num2);
        }

        model.addAttribute("captchaQuestion", num1 + " + " + num2 + " = ?");

        // 2. Nếu người dùng submit form tìm kiếm
        if (keyword != null && !keyword.isBlank()) {
            String cleanKeyword = keyword.trim();
            model.addAttribute("keyword", cleanKeyword);

            // Kiểm tra Captcha
            if (captchaInput == null || captchaInput.isBlank()) {
                model.addAttribute("error", "Vui lòng nhập mã xác nhận Captcha!");
                return "html/client-html/order-lookup";
            }

            try {
                int userAnswer = Integer.parseInt(captchaInput.trim());
                int correctAnswer = num1 + num2;

                if (userAnswer != correctAnswer) {
                    model.addAttribute("error", "Mã xác nhận Captcha không chính xác. Vui lòng thử lại!");
                    // Reset lại captcha mới sau khi nhập sai
                    session.removeAttribute("captchaNum1");
                    session.removeAttribute("captchaNum2");
                    return "html/client-html/order-lookup";
                }
            } catch (NumberFormatException e) {
                model.addAttribute("error", "Mã xác nhận Captcha phải là một con số!");
                return "html/client-html/order-lookup";
            }

            // Nếu Captcha đúng -> Xóa session cũ để đổi câu hỏi cho lần sau
            session.removeAttribute("captchaNum1");
            session.removeAttribute("captchaNum2");

            // Tiến hành tìm kiếm trong Database
            List<Booking> bookings = bookingRepository.findByKeyword(cleanKeyword);

            if (bookings != null && !bookings.isEmpty()) {
                model.addAttribute("bookingList", bookings);
            } else {
                model.addAttribute("error", "Không tìm thấy thông tin đặt phòng nào với từ khóa: " + cleanKeyword);
            }
        }

        return "html/client-html/order-lookup";
    }
}