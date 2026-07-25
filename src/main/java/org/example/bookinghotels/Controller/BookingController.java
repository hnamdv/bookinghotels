package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.Invoices;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.RoomRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.example.bookinghotels.service.OrderBookingService;
import org.example.bookinghotels.service.PromotionPricingService;
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

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private PromotionPricingService promotionPricingService;

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
        if (checkout == null || !checkout.isAfter(checkin)) checkout = checkin.plusDays(1);

        List<Room> allRooms = roomTypeId == null ? roomRepository.findAll() : roomRepository.findByRoomTypeId(roomTypeId);

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

        List<Room> displayRooms = allRooms;
        List<Map<String, Object>> roomCards = buildRoomCards(displayRooms, availableRoomIds, checkin, checkout);
        RoomType selectedRoomType = roomTypeId == null ? null : roomTypeRepository.findById(roomTypeId).orElse(null);

        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("selectedRoomTypeId", roomTypeId);
        model.addAttribute("selectedRoomType", selectedRoomType);
        model.addAttribute("availableRooms", availableRooms);
        model.addAttribute("roomCards", roomCards);
        model.addAttribute("totalAvailable", availableRooms.size());
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

            List<Room> displayRooms = roomRepository.findAllById(roomIds);
            model.addAttribute("checkin", checkin);
            model.addAttribute("checkout", checkout);
            model.addAttribute("availableRooms", availableRooms);
            model.addAttribute("bookedRooms", bookedRooms);
            model.addAttribute("roomCards", buildRoomCards(displayRooms, availableRoomIds, checkin, checkout));
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
            @RequestParam(required = false) Integer roomTypeId,
            @RequestParam(required = false) Integer roomId,
            @RequestParam String checkin,
            @RequestParam String checkout,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            LocalDate start = LocalDate.parse(checkin);
            LocalDate end = LocalDate.parse(checkout);
            if (!end.isAfter(start)) end = start.plusDays(1);

            RoomType roomType;
            if (roomTypeId != null) {
                roomType = roomTypeRepository.findById(roomTypeId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng yêu cầu."));
            } else if (roomId != null) {
                Room room = roomRepository.findById(roomId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng yêu cầu."));
                roomType = room.getRoomType();
                roomTypeId = roomType.getId();
            } else {
                throw new RuntimeException("Thiếu loại phòng cần đặt.");
            }

            if (roomRepository.findAvailableRooms(roomTypeId, start, end).isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Loại phòng này đã hết phòng trong khoảng ngày đã chọn.");
                return "redirect:/booking/check?roomTypeId=" + roomTypeId
                        + "&checkin=" + start
                        + "&checkout=" + end;
            }

            long days = ChronoUnit.DAYS.between(start, end);
            if (days <= 0) days = 1;

            PromotionPricingService.PriceQuote quote = promotionPricingService.quote(roomType, start, end);
            double roomPrice = quote.effectiveNightlyPrice();
            double originalRoomPrice = quote.originalNightlyPrice();
            double taxAndFee = roomType.getTaxAndFee() == null ? 0D : roomType.getTaxAndFee();
            double totalAmount = roomPrice * days + taxAndFee;

            Map<String, Object> bookingData = new HashMap<>();
            bookingData.put("roomTypeId", roomTypeId);
            bookingData.put("checkinDate", start.toString());
            bookingData.put("checkoutDate", end.toString());
            bookingData.put("days", days);
            bookingData.put("roomName", roomType.getNameType());
            bookingData.put("roomPrice", roomPrice);
            bookingData.put("originalRoomPrice", originalRoomPrice);
            bookingData.put("taxAndFee", taxAndFee);
            bookingData.put("discountAmount", Math.max(0D, (originalRoomPrice - roomPrice) * days));
            bookingData.put("discountPercent", quote.discountPercent());
            bookingData.put("promotionName", quote.promotionName());
            bookingData.put("promotionEndAt", quote.promotionEndAt());
            bookingData.put("promoted", quote.promoted());
            bookingData.put("totalAmount", totalAmount);

            model.addAttribute("booking", bookingData);
            return "html/client-html/payment";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/booking/check";
        }
    }

    // =====================================================
    // XÁC NHẬN THANH TOÁN -> NHẢY TRANG QR
    // =====================================================
    @PostMapping("/confirm-payment")
    public String confirmPayment(
            @RequestParam Integer roomTypeId,
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

            BookingDetail detail = new BookingDetail();
            detail.setAdultCount(adultCount);
            detail.setChildCount(childCount);
            detail.setRoomQuantity(1);
            detail.setStatus("PENDING");

            Booking savedBooking = bookingService.processBookingAutoAssign(
                    booking,
                    detail,
                    roomTypeId,
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

            return "redirect:/booking/payment?roomTypeId=" + roomTypeId
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

    private List<Map<String, Object>> buildRoomCards(List<Room> rooms,
                                                      List<Integer> availableRoomIds,
                                                      LocalDate checkin,
                                                      LocalDate checkout) {
        Set<Integer> available = new HashSet<>(availableRoomIds);
        List<Integer> ids = rooms.stream().map(Room::getId).toList();
        Map<Integer, BookingDetail> overlapByRoom = new HashMap<>();
        if (!ids.isEmpty()) {
            for (BookingDetail bd : bookingDetailRepository.findOverlappingBookings(ids, checkin, checkout)) {
                if (bd.getRoom() != null) overlapByRoom.putIfAbsent(bd.getRoom().getId(), bd);
            }
        }
        List<Map<String, Object>> cards = new ArrayList<>();
        for (Room room : rooms) {
            Map<String, Object> card = new LinkedHashMap<>();
            card.put("room", room);
            boolean isAvailable = available.contains(room.getId());
            card.put("available", isAvailable);
            BookingDetail occupied = overlapByRoom.get(room.getId());
            card.put("bookingDetail", occupied);
            if (room.getRoomType() != null) {
                PromotionPricingService.PriceQuote quote = promotionPricingService.quote(room.getRoomType(), checkin, checkout);
                card.put("promoted", quote.promoted());
                card.put("originalPrice", quote.originalNightlyPrice());
                card.put("effectivePrice", quote.effectiveNightlyPrice());
                card.put("discountPercent", quote.discountPercent());
                card.put("promotionName", quote.promotionName());
                card.put("promotionEndAt", quote.promotionEndAt());
            } else {
                card.put("promoted", false);
                card.put("originalPrice", 0D);
                card.put("effectivePrice", 0D);
            }
            if (isAvailable) {
                card.put("label", "CÒN TRỐNG");
                card.put("availableAgain", null);
            } else if (occupied != null) {
                String status = occupied.getStatus() == null ? "" : occupied.getStatus().toUpperCase();
                card.put("label", "CHECKED_IN".equals(status) ? "ĐANG CÓ KHÁCH" : "ĐÃ ĐƯỢC ĐẶT");
                card.put("availableAgain", occupied.getBooking().getCheckoutDate());
            } else {
                card.put("label", "KHÔNG KHẢ DỤNG");
                card.put("availableAgain", null);
            }
            cards.add(card);
        }
        return cards;
    }
}
