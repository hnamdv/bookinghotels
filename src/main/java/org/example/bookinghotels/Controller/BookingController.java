package org.example.bookinghotels.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.BookingFB;
import org.example.bookinghotels.entity.Invoices;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.entity.FwB;
import org.example.bookinghotels.repository.RoomRepository;
import org.example.bookinghotels.service.OrderBookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/check")
    public String showCheckForm(
            @RequestParam(required = false) Integer roomTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkout,
            Model model) {

        if (checkin == null) checkin = LocalDate.now();
        if (checkout == null) checkout = LocalDate.now().plusDays(1);

        List<Room> allRooms = roomRepository.findAll();
        List<Integer> roomIds = allRooms.stream().map(Room::getId).collect(Collectors.toList());

        List<Integer> availableRoomIds = bookingService.getAvailableRooms(roomIds, checkin, checkout);
        List<Room> availableRooms = roomRepository.findAllById(availableRoomIds);

        if (roomTypeId != null) {
            availableRooms = availableRooms.stream()
                    .filter(r -> r.getRoomType() != null && r.getRoomType().getId().equals(roomTypeId))
                    .collect(Collectors.toList());
        }

        Map<Integer, Room> uniqueRoomTypeMap = new LinkedHashMap<>();
        for (Room room : availableRooms) {
            if (room.getRoomType() != null) {
                uniqueRoomTypeMap.putIfAbsent(room.getRoomType().getId(), room);
            }
        }
        List<Room> uniqueAvailableRooms = new ArrayList<>(uniqueRoomTypeMap.values());

        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("availableRooms", uniqueAvailableRooms);
        model.addAttribute("today", LocalDate.now().toString());

        return "html/client-html/booking";
    }

    @PostMapping("/check")
    public String checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkin,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkout,
            @RequestParam(required = false) List<Integer> roomIds,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            if (roomIds == null || roomIds.isEmpty()) {
                roomIds = roomRepository.findAll().stream().map(Room::getId).collect(Collectors.toList());
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

            Map<Integer, Room> uniqueRoomTypeMap = new LinkedHashMap<>();
            for (Room room : availableRooms) {
                if (room.getRoomType() != null) {
                    uniqueRoomTypeMap.putIfAbsent(room.getRoomType().getId(), room);
                }
            }
            List<Room> uniqueAvailableRooms = new ArrayList<>(uniqueRoomTypeMap.values());

            model.addAttribute("checkin", checkin);
            model.addAttribute("checkout", checkout);
            model.addAttribute("availableRooms", uniqueAvailableRooms);
            model.addAttribute("totalAvailable", uniqueAvailableRooms.size());
            model.addAttribute("totalBooked", roomIds.size() - availableRooms.size());

            return "html/client-html/booking";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/booking/check";
        }
    }

    @GetMapping("/payment")
    public String showPaymentPage(
            @RequestParam Integer roomId,
            @RequestParam String checkin,
            @RequestParam String checkout,
            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy phòng yêu cầu.");
            return "redirect:/booking/check";
        }

        Room room = roomOpt.get();
        LocalDate start = LocalDate.parse(checkin);
        LocalDate end = LocalDate.parse(checkout);

        boolean isStillAvailable = bookingService.isRoomAvailable(roomId, start, end);
        if (!isStillAvailable) {
            redirectAttributes.addFlashAttribute("error", "Phòng này vừa có người đặt hoặc đang giữ giao dịch. Vui lòng chọn phòng khác!");
            return "redirect:/booking/check?checkin=" + checkin + "&checkout=" + checkout;
        }

        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0) days = 1;

        double originalPrice = room.getRoomType().getPrice();
        double discountPercent = 10.0;
        double discountedPrice = originalPrice * (1 - (discountPercent / 100));
        double totalAmount = discountedPrice * days;

        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("roomId", roomId);
        bookingData.put("checkinDate", checkin);
        bookingData.put("checkoutDate", checkout);
        bookingData.put("days", days);
        bookingData.put("roomName", room.getRoomType().getNameType());
        bookingData.put("roomPrice", discountedPrice);
        bookingData.put("totalAmount", totalAmount);

        List<FwB> foodMenu = bookingService.getAllAvailableFoods();

        model.addAttribute("booking", bookingData);
        model.addAttribute("foodMenu", foodMenu);

        return "html/client-html/payment";
    }

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
            @RequestParam(required = false) String selectedServicesJson,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            LocalDate start = LocalDate.parse(checkinDate);
            LocalDate end = LocalDate.parse(checkoutDate);

            boolean isStillAvailable = bookingService.isRoomAvailable(roomId, start, end);
            if (!isStillAvailable) {
                redirectAttributes.addFlashAttribute("error", "Rất tiếc, phòng đã bị giữ chỗ trong vài phút trước. Vui lòng chọn phòng khác.");
                return "redirect:/booking/check?checkin=" + checkinDate + "&checkout=" + checkoutDate;
            }

            Booking booking = new Booking();
            booking.setName(customerName);
            booking.setPhone(customerPhone);
            booking.setEmail(customerEmail);
            booking.setCheckinDate(start);
            booking.setCheckoutDate(end);

            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

            BookingDetail detail = new BookingDetail();
            detail.setRoom(room);
            detail.setRoomType(room.getRoomType());
            detail.setAdultCount(adultCount);
            detail.setChildCount(childCount);
            detail.setRoomQuantity(1);
            detail.setStatus("PENDING");

            List<BookingFB> orderedFoods = new ArrayList<>();
            if (selectedServicesJson != null && !selectedServicesJson.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> serviceList = mapper.readValue(selectedServicesJson, List.class);

                for (Map<String, Object> sItem : serviceList) {
                    Integer fwbId = (Integer) sItem.get("fwbId");
                    Integer qty = (Integer) sItem.get("quantity");

                    if (fwbId != null && qty != null && qty > 0) {
                        BookingFB itemFB = new BookingFB();
                        FwB fwbRef = new FwB();
                        fwbRef.setId(fwbId);
                        itemFB.setFwb(fwbRef);
                        itemFB.setQuantity(qty);
                        orderedFoods.add(itemFB);
                    }
                }
            }

            Booking savedBooking = bookingService.processBooking(
                    booking,
                    detail,
                    orderedFoods,
                    paymentMethod
            );

            Invoices invoice = bookingService.findInvoiceByBookingId(Long.valueOf(savedBooking.getId()));

            model.addAttribute("booking", savedBooking);
            model.addAttribute("invoice", invoice);

            return "html/client-html/qr-payment";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/booking/payment?roomId=" + roomId
                    + "&checkin=" + checkinDate
                    + "&checkout=" + checkoutDate;
        }
    }

    // =====================================================
    // ĐÃ SỬA DỨT ĐIỂM: API TRẢ VỀ ĐỒNG THỜI CẢ STATUS VÀ PAYMENTSTATUS ĐỂ PHỤC VỤ JAVASCRIPT POLLING
    // =====================================================
    @GetMapping("/api/invoice-status/{bookingId}")
    @ResponseBody
    public Map<String, String> checkInvoiceStatus(@PathVariable Long bookingId) {
        Map<String, String> response = new HashMap<>();
        try {
            Invoices invoice = bookingService.findInvoiceByBookingId(bookingId);
            response.put("status", invoice.getPaymentStatus());
            response.put("paymentStatus", invoice.getPaymentStatus());
        } catch (Exception e) {
            response.put("status", "UNKNOWN");
            response.put("paymentStatus", "UNKNOWN");
        }
        return response;
    }

    @PostMapping("/api/cancel-booking/{bookingId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelBooking(@PathVariable Long bookingId) {
        Map<String, Object> response = new HashMap<>();
        try {
            bookingService.updateBookingStatus(bookingId, "CANCELLED");
            response.put("success", true);
            response.put("message", "Đã hủy giữ phòng thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi hủy: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
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