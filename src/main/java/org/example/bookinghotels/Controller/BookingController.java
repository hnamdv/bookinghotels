package org.example.bookinghotels.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.BookingFB;
import org.example.bookinghotels.entity.Invoices;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.entity.FwB;
import org.example.bookinghotels.repository.FwbRepository;
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

    @Autowired
    private FwbRepository fwbRepository;

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

        List<Map<String, Object>> roomCards = new ArrayList<>();
        for (Room room : allRooms) {
            boolean isAvailable = availableRoomIds.contains(room.getId());

            if (roomTypeId != null && (room.getRoomType() == null || !room.getRoomType().getId().equals(roomTypeId))) {
                continue;
            }

            Map<String, Object> card = new HashMap<>();
            card.put("room", room);
            card.put("available", isAvailable);
            card.put("label", isAvailable ? "CÒN TRỐNG" : "ĐÃ CÓ KHÁCH");

            double price = (room.getRoomType() != null) ? room.getRoomType().getPrice() : 0.0;
            card.put("effectivePrice", price);
            card.put("promoted", false);
            card.put("availableAgain", null);

            roomCards.add(card);
        }

        long totalAvailable = availableRoomIds.size();

        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("roomCards", roomCards);
        model.addAttribute("totalAvailable", totalAvailable);
        model.addAttribute("selectedRoomTypeId", roomTypeId);
        if (roomTypeId != null && !roomCards.isEmpty()) {
            model.addAttribute("selectedRoomType", ((Room)roomCards.get(0).get("room")).getRoomType());
        }
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

            List<Room> allRooms = roomRepository.findAll();
            List<Integer> availableRoomIds = bookingService.getAvailableRooms(roomIds, checkin, checkout);

            List<Map<String, Object>> roomCards = new ArrayList<>();
            for (Room room : allRooms) {
                boolean isAvailable = availableRoomIds.contains(room.getId());
                Map<String, Object> card = new HashMap<>();
                card.put("room", room);
                card.put("available", isAvailable);
                card.put("label", isAvailable ? "CÒN TRỐNG" : "ĐÃ CÓ KHÁCH");
                double price = (room.getRoomType() != null) ? room.getRoomType().getPrice() : 0.0;
                card.put("effectivePrice", price);
                card.put("promoted", false);
                card.put("availableAgain", null);
                roomCards.add(card);
            }

            model.addAttribute("checkin", checkin);
            model.addAttribute("checkout", checkout);
            model.addAttribute("roomCards", roomCards);
            model.addAttribute("totalAvailable", (long) availableRoomIds.size());
            model.addAttribute("totalBooked", roomIds.size() - availableRoomIds.size());

            return "html/client-html/booking";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/booking/check";
        }
    }

    @GetMapping("/payment")
    public String showPaymentPage(
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) Integer roomTypeId,
            @RequestParam String checkin,
            @RequestParam String checkout,
            Model model,
            RedirectAttributes redirectAttributes) {

        LocalDate start = LocalDate.parse(checkin);
        LocalDate end = LocalDate.parse(checkout);

        if (roomId == null && roomTypeId != null) {
            List<Room> allRooms = roomRepository.findAll().stream()
                    .filter(r -> r.getRoomType() != null && r.getRoomType().getId().equals(roomTypeId))
                    .collect(Collectors.toList());
            List<Integer> roomIds = allRooms.stream().map(Room::getId).collect(Collectors.toList());
            List<Integer> availableRoomIds = bookingService.getAvailableRooms(roomIds, start, end);

            if (!availableRoomIds.isEmpty()) {
                roomId = availableRoomIds.get(0);
            }
        }

        if (roomId == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy phòng trống phù hợp.");
            return "redirect:/booking/check?checkin=" + checkin + "&checkout=" + checkout;
        }

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy phòng yêu cầu.");
            return "redirect:/booking/check";
        }

        Room room = roomOpt.get();

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
                        FwB fwbRef = fwbRepository.findById(fwbId).orElse(null);
                        if (fwbRef != null) {
                            BookingFB itemFB = new BookingFB();
                            itemFB.setFwb(fwbRef);
                            itemFB.setQuantity(qty);
                            itemFB.setPriceAtOrder(fwbRef.getPrice()); // Đảm bảo gán giá tại thời điểm đặt tránh null
                            orderedFoods.add(itemFB);
                        }
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

    @GetMapping("/api/invoice-status/{bookingId}")
    @ResponseBody
    public Map<String, String> checkInvoiceStatus(@PathVariable Long bookingId) {
        Map<String, String> response = new HashMap<>();
        try {
            Invoices invoice = bookingService.findInvoiceByBookingId(bookingId);
            String status = (invoice != null && invoice.getPaymentStatus() != null) ? invoice.getPaymentStatus() : "UNKNOWN";
            response.put("status", status);
            response.put("paymentStatus", status);
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
            response.put("message", "Đã hủy giữ phòng và cập nhật trạng thái hóa đơn thành CANCELLED thành công.");
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

    @GetMapping("/api/available-rooms")
    @ResponseBody
    public List<Map<String, Object>> getAvailableRoomsJson(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkin,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkout) {

        List<Room> allRooms = roomRepository.findAll();
        List<Integer> roomIds = allRooms.stream().map(Room::getId).collect(Collectors.toList());
        List<Integer> availableRoomIds = bookingService.getAvailableRooms(roomIds, checkin, checkout);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Integer id : availableRoomIds) {
            Room room = roomRepository.findById(id).orElse(null);
            if (room != null) {
                Map<String, Object> roomMap = new HashMap<>();
                roomMap.put("id", room.getId());
                roomMap.put("name", "Phòng " + room.getRoomNumber() + " - " + room.getRoomType().getNameType());
                result.add(roomMap);
            }
        }
        return result;
    }

    @PostMapping("/confirm-payment-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmPaymentAjax(
            @RequestParam Integer roomId,
            @RequestParam String customerName,
            @RequestParam String customerPhone,
            @RequestParam String customerEmail,
            @RequestParam Integer adultCount,
            @RequestParam Integer childCount,
            @RequestParam String checkinDate,
            @RequestParam String checkoutDate,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String selectedServicesJson
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate start = LocalDate.parse(checkinDate);
            LocalDate end = LocalDate.parse(checkoutDate);

            boolean isStillAvailable = bookingService.isRoomAvailable(roomId, start, end);
            if (!isStillAvailable) {
                response.put("success", false);
                response.put("message", "Phòng đã bị giữ chỗ trong vài phút trước. Vui lòng chọn phòng khác.");
                return ResponseEntity.badRequest().body(response);
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
            detail.setStatus("CONFIRMED");

            List<BookingFB> orderedFoods = new ArrayList<>();
            if (selectedServicesJson != null && !selectedServicesJson.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> serviceList = mapper.readValue(selectedServicesJson, List.class);

                for (Map<String, Object> sItem : serviceList) {
                    Integer fwbId = (Integer) sItem.get("fwbId");
                    Integer qty = (Integer) sItem.get("quantity");

                    if (fwbId != null && qty != null && qty > 0) {
                        FwB fwbRef = fwbRepository.findById(fwbId).orElse(null);
                        if (fwbRef != null) {
                            BookingFB itemFB = new BookingFB();
                            itemFB.setFwb(fwbRef);
                            itemFB.setQuantity(qty);
                            itemFB.setPriceAtOrder(fwbRef.getPrice()); // Đảm bảo gán giá tại thời điểm đặt tránh null
                            orderedFoods.add(itemFB);
                        }
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

            double finalTotal = (invoice != null && invoice.getTotalAmount() != null) ? invoice.getTotalAmount() : 0.0;

            response.put("success", true);
            response.put("customerName", customerName);
            response.put("totalAmount", String.format("%,.0f VND", finalTotal));
            response.put("bookingId", savedBooking.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/admin/walk-in")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createWalkInBooking(
            @RequestParam Integer roomId,
            @RequestParam String customerName,
            @RequestParam String customerPhone,
            @RequestParam(required = false) String customerEmail,
            @RequestParam Integer adultCount,
            @RequestParam Integer childCount,
            @RequestParam String checkinDate,
            @RequestParam String checkoutDate,
            @RequestParam String paymentMethod
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate start = LocalDate.parse(checkinDate);
            LocalDate end = LocalDate.parse(checkoutDate);

            boolean isStillAvailable = bookingService.isRoomAvailable(roomId, start, end);
            if (!isStillAvailable) {
                response.put("success", false);
                response.put("message", "Phòng này hiện không trống trong khoảng thời gian đã chọn!");
                return ResponseEntity.badRequest().body(response);
            }

            Booking booking = new Booking();
            booking.setName(customerName);
            booking.setPhone(customerPhone);
            booking.setEmail(customerEmail != null && !customerEmail.trim().isEmpty() ? customerEmail : "walkin@hotel.com");
            booking.setCheckinDate(start);
            booking.setCheckoutDate(end);

            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin phòng"));

            BookingDetail detail = new BookingDetail();
            detail.setRoom(room);
            detail.setRoomType(room.getRoomType());
            detail.setAdultCount(adultCount);
            detail.setChildCount(childCount);
            detail.setRoomQuantity(1);
            detail.setStatus("CONFIRMED");

            Booking savedBooking = bookingService.processBooking(
                    booking,
                    detail,
                    new ArrayList<>(),
                    paymentMethod
            );

            Invoices invoice = bookingService.findInvoiceByBookingId(Long.valueOf(savedBooking.getId()));
            double totalAmount = (invoice != null && invoice.getTotalAmount() != null) ? invoice.getTotalAmount() : 0.0;

            response.put("success", true);
            response.put("message", "Lập đơn đặt phòng tại quầy thành công!");
            response.put("bookingId", savedBooking.getId());
            response.put("totalAmount", totalAmount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}