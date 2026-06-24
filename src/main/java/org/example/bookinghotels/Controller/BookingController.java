package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.Invoices;
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
    // TRANG KIỂM TRA PHÒNG TRỐNG (GET)
    // =====================================================
    @GetMapping("/check")
    public String showCheckForm(
            @RequestParam(required = false) Integer roomTypeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkin,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkout,

            Model model) {

        if (checkin == null) checkin = LocalDate.now();
        if (checkout == null) checkout = LocalDate.now().plusDays(1);

        List<Room> allRooms = roomRepository.findAll();

        List<Integer> roomIds = allRooms.stream()
                .map(Room::getId)
                .collect(Collectors.toList());

        List<Integer> availableRoomIds =
                bookingService.getAvailableRooms(roomIds, checkin, checkout);

        List<Room> availableRooms =
                roomRepository.findAllById(availableRoomIds);

        if (roomTypeId != null) {
            availableRooms = availableRooms.stream()
                    .filter(r -> r.getRoomType() != null
                            && r.getRoomType().getId().equals(roomTypeId))
                    .collect(Collectors.toList());
        }

        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("availableRooms", availableRooms);
        model.addAttribute("today", LocalDate.now().toString());

        return "html/client-html/booking";
    }

    // =====================================================
    // XỬ LÝ CHECK PHÒNG
    // =====================================================
    @PostMapping("/check")
    public String checkAvailability(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkin,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkout,

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
                redirectAttributes.addFlashAttribute(
                        "error",
                        "Vui lòng chọn ngày check-in và check-out"
                );
                return "redirect:/booking/check";
            }

            if (!checkin.isBefore(checkout)) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "Ngày check-in phải trước ngày check-out"
                );
                return "redirect:/booking/check";
            }

            if (checkin.isBefore(LocalDate.now())) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "Không thể đặt phòng trong quá khứ"
                );
                return "redirect:/booking/check";
            }

            List<Integer> availableRoomIds =
                    bookingService.getAvailableRooms(
                            roomIds,
                            checkin,
                            checkout
                    );

            List<Room> availableRooms =
                    roomRepository.findAllById(availableRoomIds);

            List<Integer> bookedRoomIds = new ArrayList<>(roomIds);
            bookedRoomIds.removeAll(availableRoomIds);

            List<Room> bookedRooms =
                    roomRepository.findAllById(bookedRoomIds);

            model.addAttribute("checkin", checkin);
            model.addAttribute("checkout", checkout);
            model.addAttribute("availableRooms", availableRooms);
            model.addAttribute("bookedRooms", bookedRooms);
            model.addAttribute("totalAvailable", availableRooms.size());
            model.addAttribute("totalBooked", bookedRooms.size());

            return "html/client-html/booking";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Lỗi: " + e.getMessage()
            );
            return "redirect:/booking/check";
        }
    }

    // =====================================================
    // TRANG PAYMENT
    // =====================================================
    @GetMapping("/payment")
    public String showPaymentPage(
            @RequestParam Integer roomId,
            @RequestParam String checkin,
            @RequestParam String checkout,
            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<Room> roomOpt = roomRepository.findById(roomId);

        if (roomOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không tìm thấy phòng yêu cầu."
            );
            return "redirect:/booking/check";
        }

        Room room = roomOpt.get();

        LocalDate start = LocalDate.parse(checkin);
        LocalDate end = LocalDate.parse(checkout);

        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0) days = 1;

        double roomPrice = room.getRoomType().getPrice();
        double totalAmount = roomPrice * days;

        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("roomId", roomId);
        bookingData.put("checkinDate", checkin);
        bookingData.put("checkoutDate", checkout);
        bookingData.put("days", days);
        bookingData.put(
                "roomName",
                room.getRoomType().getNameType()
                        + " (#" + room.getRoomNumber() + ")"
        );
        bookingData.put("roomPrice", roomPrice);
        bookingData.put("totalAmount", totalAmount);

        model.addAttribute("booking", bookingData);

        return "html/client-html/payment";
    }

    // =====================================================
    // XÁC NHẬN THANH TOÁN -> NHẢY TRANG QR
    // =====================================================
    @PostMapping("/confirm-payment")
    public String confirmPayment(
            @RequestParam Integer roomId,
            @RequestParam String customerName,
            @RequestParam String customerPhone,
            @RequestParam String customerEmail,
            @RequestParam Integer adultCount,
            @RequestParam Integer childCount,
            @RequestParam String checkinDate,
            @RequestParam String checkoutDate,
            @RequestParam String paymentMethod,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {

            Booking booking = new Booking();
            booking.setName(customerName);
            booking.setPhone(customerPhone);
            booking.setEmail(customerEmail);
            booking.setCheckinDate(LocalDate.parse(checkinDate));
            booking.setCheckoutDate(LocalDate.parse(checkoutDate));

            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() ->
                            new RuntimeException("Không tìm thấy phòng"));

            BookingDetail detail = new BookingDetail();
            detail.setRoom(room);
            detail.setRoomType(room.getRoomType());
            detail.setAdultCount(adultCount);
            detail.setChildCount(childCount);
            detail.setRoomQuantity(1);
            detail.setStatus("PENDING");

            Booking savedBooking = bookingService.processBooking(
                    booking,
                    detail,
                    new ArrayList<>(),
                    paymentMethod
            );

            Invoices invoice = bookingService.findInvoiceByBookingId(
                    Long.valueOf(savedBooking.getId())
            );

            model.addAttribute("booking", savedBooking);
            model.addAttribute("invoice", invoice);

            return "html/client-html/qr-payment";

        } catch (Exception e) {
            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Lỗi: " + e.getMessage()
            );

            return "redirect:/booking/payment?roomId=" + roomId
                    + "&checkin=" + checkinDate
                    + "&checkout=" + checkoutDate;
        }
    }

    // =====================================================
    // AJAX CHECK ROOM
    // =====================================================
    @GetMapping("/api/check-room")
    @ResponseBody
    public Map<String, Object> checkRoomAjax(
            @RequestParam Integer roomId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkin,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkout) {

        boolean available =
                bookingService.isRoomAvailable(
                        roomId,
                        checkin,
                        checkout
                );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomId", roomId);
        result.put("available", available);
        result.put(
                "message",
                available ? "✅ Phòng trống" : "❌ Phòng đã được đặt"
        );

        return result;
    }
}