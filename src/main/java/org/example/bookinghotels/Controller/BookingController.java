package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.repository.RoomRepository;
import org.example.bookinghotels.service.OrderBookingService;
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
    private RoomRepository roomRepository;

    // =====================================================
    // TRANG KIỂM TRA PHÒNG TRỐNG
    // =====================================================
    @GetMapping("/check")
    public String showCheckForm(Model model) {
        // Lấy tất cả phòng để hiển thị
        List<Room> allRooms = roomRepository.findAll();
        model.addAttribute("rooms", allRooms);
        model.addAttribute("today", LocalDate.now().toString());
        return "booking/check-availability"; // templates/booking/check-availability.html
    }

    // =====================================================
    // XỬ LÝ KIỂM TRA PHÒNG TRỐNG (POST)
    // =====================================================
    @PostMapping("/check")
    public String checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkin,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkout,
            @RequestParam(required = false) List<Integer> roomIds,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Nếu không chọn phòng cụ thể thì lấy tất cả
            if (roomIds == null || roomIds.isEmpty()) {
                roomIds = roomRepository.findAll()
                        .stream()
                        .map(Room::getId)
                        .collect(Collectors.toList());
            }

            // Validate ngày
            if (checkin == null || checkout == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ngày check-in và check-out");
                return "redirect:/booking/check";
            }

            if (!checkin.isBefore(checkout)) {
                redirectAttributes.addFlashAttribute("error", "Ngày check-in phải trước ngày check-out");
                return "redirect:/booking/check";
            }

            if (checkin.isBefore(LocalDate.now())) {
                redirectAttributes.addFlashAttribute("error", "Không thể đặt phòng trong quá khứ");
                return "redirect:/booking/check";
            }

            // Lấy danh sách phòng trống
            List<Integer> availableRoomIds = bookingService.getAvailableRooms(roomIds, checkin, checkout);

            // Lấy thông tin chi tiết phòng trống
            List<Room> availableRooms = roomRepository.findAllById(availableRoomIds);

            // Lấy phòng đã đặt
            List<Integer> bookedRoomIds = new ArrayList<>(roomIds);
            bookedRoomIds.removeAll(availableRoomIds);
            List<Room> bookedRooms = roomRepository.findAllById(bookedRoomIds);

            model.addAttribute("checkin", checkin);
            model.addAttribute("checkout", checkout);
            model.addAttribute("availableRooms", availableRooms);
            model.addAttribute("bookedRooms", bookedRooms);
            model.addAttribute("totalAvailable", availableRooms.size());
            model.addAttribute("totalBooked", bookedRooms.size());

            return "booking/check-result"; // templates/booking/check-result.html

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/booking/check";
        }
    }

    // =====================================================
    // API KIỂM TRA NHANH BẰNG AJAX (cho HTML + JS)
    // =====================================================
    @GetMapping("/api/check-room")
    @ResponseBody
    public Map<String, Object> checkRoomAjax(
            @RequestParam Integer roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkin,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkout) {

        boolean available = bookingService.isRoomAvailable(roomId, checkin, checkout);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomId", roomId);
        result.put("available", available);
        result.put("message", available ? "✅ Phòng trống" : "❌ Phòng đã được đặt");
        return result;
    }
}