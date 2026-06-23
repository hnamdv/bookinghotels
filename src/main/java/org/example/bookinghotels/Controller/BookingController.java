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
import java.time.temporal.ChronoUnit;
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
    // TRANG KIỂM TRA PHÒNG TRỐNG (GET) - Đã sửa hỗ trợ lấy dữ liệu từ Trang Chủ
    // =====================================================
    @GetMapping("/check")
    public String showCheckForm(
            @RequestParam(required = false) Integer roomTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkout,
            Model model) {

        // 1. Xử lý thiết lập ngày mặc định nếu từ ngoài trang chủ chưa truyền vào
        if (checkin == null) checkin = LocalDate.now();
        if (checkout == null) checkout = LocalDate.now().plusDays(1);

        // 2. Lấy toàn bộ danh sách ID phòng trong hệ thống
        List<Room> allRooms = roomRepository.findAll();
        List<Integer> roomIds = allRooms.stream().map(Room::getId).collect(Collectors.toList());

        // 3. Kiểm tra phòng trống bằng Service thông qua khoảng ngày
        List<Integer> availableRoomIds = bookingService.getAvailableRooms(roomIds, checkin, checkout);
        List<Room> availableRooms = roomRepository.findAllById(availableRoomIds);

        // 4. Nếu người dùng chọn đích danh Loại Phòng từ ngoài trang chủ, tiến hành lọc đúng loại đó
        if (roomTypeId != null) {
            availableRooms = availableRooms.stream()
                    .filter(r -> r.getRoomType() != null && r.getRoomType().getId().equals(roomTypeId))
                    .collect(Collectors.toList());
        }

        // Đẩy dữ liệu đồng bộ ra giao diện Thymeleaf
        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("availableRooms", availableRooms);
        model.addAttribute("today", LocalDate.now().toString());

        return "html/client-html/booking";
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
            if (roomIds == null || roomIds.isEmpty()) {
                roomIds = roomRepository.findAll()
                        .stream()
                        .map(Room::getId)
                        .collect(Collectors.toList());
            }

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

            List<Integer> availableRoomIds = bookingService.getAvailableRooms(roomIds, checkin, checkout);
            List<Room> availableRooms = roomRepository.findAllById(availableRoomIds);

            List<Integer> bookedRoomIds = new ArrayList<>(roomIds);
            bookedRoomIds.removeAll(availableRoomIds);
            List<Room> bookedRooms = roomRepository.findAllById(bookedRoomIds);

            model.addAttribute("checkin", checkin);
            model.addAttribute("checkout", checkout);
            model.addAttribute("availableRooms", availableRooms);
            model.addAttribute("bookedRooms", bookedRooms);
            model.addAttribute("totalAvailable", availableRooms.size());
            model.addAttribute("totalBooked", bookedRooms.size());

            return "html/client-html/booking";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/booking/check";
        }
    }

    // =====================================================
    // HIỂN THỊ TRANG THANH TOÁN (GET)
    // =====================================================
    @GetMapping("/payment")
    public String showPaymentPage(
            @RequestParam Integer roomId,
            @RequestParam String checkin,
            @RequestParam String checkout,
            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (!roomOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy phòng yêu cầu.");
            return "redirect:/booking/check";
        }

        Room room = roomOpt.get();
        LocalDate start = LocalDate.parse(checkin);
        LocalDate end = LocalDate.parse(checkout);
        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0) days = 1;

        double roomPrice = 0.0;
        String roomTypeName = "Phòng tiêu chuẩn";

        if (room.getRoomType() != null) {
            if (room.getRoomType().getPrice() != null) {
                roomPrice = room.getRoomType().getPrice(); // Đã đồng bộ động hoàn toàn từ database loại phòng
            }
            if (room.getRoomType().getNameType() != null) {
                roomTypeName = room.getRoomType().getNameType();
            }
        }

        double totalAmount = roomPrice * days;

        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("id", "BK" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        bookingData.put("checkinDate", checkin);
        bookingData.put("checkoutDate", checkout);
        bookingData.put("days", days);
        bookingData.put("roomName", roomTypeName + " (#" + room.getRoomNumber() + ")");
        bookingData.put("roomPrice", roomPrice);
        bookingData.put("totalAmount", totalAmount);

        model.addAttribute("booking", bookingData);
        return "html/client-html/payment";
    }

    @PostMapping("/confirm-payment")
    public String confirmPayment(
            @RequestParam String bookingId,
            @RequestParam String customerName,
            @RequestParam String customerPhone,
            @RequestParam String paymentMethod,
            RedirectAttributes redirectAttributes) {

        // Xử lý lưu Database qua bookingService tại đây...
        redirectAttributes.addFlashAttribute("success", "Đặt phòng thành công! Chúng tôi sẽ liên hệ sớm nhất.");
        return "redirect:/booking/check";
    }

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