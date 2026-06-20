package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.BookingRepository;
import org.example.bookinghotels.repository.RoomRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.example.bookinghotels.service.OrderBookingService;
import org.example.bookinghotels.service.impl.OrderBookingServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
@Controller
@RequestMapping("/booking")
public class BookingController {

    @Autowired
    private OrderBookingService bookingService;
    @Autowired
    private OrderBookingServiceImpl bookingServiceimpl;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private BookingRepository bookingRepository;

    // Trang chủ booking
    @GetMapping
    public String showBookingPage(Model model) {
        model.addAttribute("booking", new Booking());
        return "html/client-html/booking";
    }

    // API lấy danh sách loại phòng trống (Dùng cho JS ở client-html/booking.html)
    @GetMapping("/api/check-room-types")
    @ResponseBody
    public List<RoomType> getAvailableRoomTypes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkin,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkout) {
        return bookingService.getAvailableRoomTypes(checkin, checkout);
    }
    // 1. Giữ nguyên POST để lưu dữ liệu
    @PostMapping("/save")
    public String saveBooking(@ModelAttribute Booking booking, @RequestParam Integer roomTypeId, Model model) {
        Booking savedBooking = bookingService.saveAndReturn(booking, roomTypeId);
        model.addAttribute("booking", savedBooking);

        // Sửa lại cho khớp với cấu trúc trong ảnh:
        return "html/client-html/payment";
    }

    // 2. Thêm GET để có thể load lại trang mà không lỗi
    @GetMapping("/payment")
    public String showPaymentPage(@RequestParam Integer bookingId, Model model) {
        // Gọi findById trực tiếp từ repo
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng với ID: " + bookingId));

        model.addAttribute("booking", booking);
        return "html/client-html/booking/payment";
    }

    @GetMapping("/success")
    public String successPage() {
        return "html/client-html/booking/success"; // File thông báo thành công
    }
    @Autowired
    private RoomTypeRepository roomTypeRepository;



    // API lấy tất cả loại phòng để hiển thị ngay khi vào trang
    @GetMapping("/api/all-room-types")
    @ResponseBody
    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }
}