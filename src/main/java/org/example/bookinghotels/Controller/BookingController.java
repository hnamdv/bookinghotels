package org.example.bookinghotels.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.BookingFB;
import org.example.bookinghotels.entity.Invoices;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.entity.FwB;
import org.example.bookinghotels.repository.FwbRepository;
import org.example.bookinghotels.repository.RoomRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.example.bookinghotels.service.OrderBookingService;
import org.example.bookinghotels.service.PromotionPricingService;
import org.example.bookinghotels.service.SiteBrandingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private static final String FALLBACK_IMAGE = "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80";

    @Autowired
    private OrderBookingService bookingService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private FwbRepository fwbRepository;

    @Autowired
    private SiteBrandingService brandingService;

    @Autowired
    private PromotionPricingService promotionPricingService;

    @GetMapping("/check")
    public String showCheckForm(
            @RequestParam(required = false) Integer roomTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkout,
            @RequestParam(required = false) Integer hotelId,
            Model model) {

        if (checkin == null) checkin = LocalDate.now();
        if (checkout == null || !checkout.isAfter(checkin)) checkout = checkin.plusDays(1);

        long totalAvailable = 0L;
        RoomType selectedForBranding = null;
        if (roomTypeId != null) {
            Optional<RoomType> roomTypeOpt = roomTypeRepository.findById(roomTypeId);
            if (roomTypeOpt.isPresent()) {
                RoomType selected = roomTypeOpt.get();
                selectedForBranding = selected;
                List<Room> roomsOfType = roomRepository.findByRoomTypeId(roomTypeId);
                List<Integer> roomIdsOfType = roomsOfType.stream().map(Room::getId).collect(Collectors.toList());
                List<Integer> availableRoomIds = roomIdsOfType.isEmpty()
                        ? List.of()
                        : bookingService.getAvailableRooms(roomIdsOfType, checkin, checkout);

                totalAvailable = availableRoomIds.size();

                PromotionPricingService.PriceQuote quote = promotionPricingService.quote(selected, checkin, checkout);
                double originalPrice = quote.originalNightlyPrice();
                double effectivePrice = quote.effectiveNightlyPrice();
                double discountAmount = Math.max(0D, originalPrice - effectivePrice);

                model.addAttribute("selectedRoomType", selected);
                model.addAttribute("selectedRoomTypeId", roomTypeId);
                model.addAttribute("selectedRoomImages", roomTypeImages(selected));
                model.addAttribute("selectedRoomHero", roomTypeImages(selected).get(0));
                model.addAttribute("totalPhysicalRooms", roomsOfType.size());
                model.addAttribute("availableRoomIds", availableRoomIds);

                // Ưu đãi dùng chung cùng service với Home, Detail và Payment.
                // Không tự tính riêng ở trang booking/check để tránh lệch giá giữa các trang.
                model.addAttribute("hasPromotion", quote.promoted());
                model.addAttribute("promotionName", quote.promotionName());
                model.addAttribute("discountPercent", Math.round(quote.discountPercent()));
                model.addAttribute("originalNightlyPrice", originalPrice);
                model.addAttribute("effectiveNightlyPrice", effectivePrice);
                model.addAttribute("discountPerNight", discountAmount);
                model.addAttribute("promotionEndAt", quote.promotionEndAt());
            }
        } else {
            // Không bắt khách chọn lại phòng vật lý. Nếu chưa có roomTypeId thì quay về trang home để chọn loại phòng trước.
            model.addAttribute("selectedRoomType", null);
        }

        model.addAttribute("checkin", checkin);
        model.addAttribute("checkout", checkout);
        model.addAttribute("stayNights", Math.max(1, ChronoUnit.DAYS.between(checkin, checkout)));
        model.addAttribute("totalAvailable", totalAvailable);
        model.addAttribute("today", LocalDate.now().toString());
        Integer activeHotelId = hotelId;
        if (activeHotelId == null && selectedForBranding != null && selectedForBranding.getHotels() != null) {
            activeHotelId = selectedForBranding.getHotels().getId();
        }
        applyBranding(model, selectedForBranding == null ? null : selectedForBranding.getHotels());
        model.addAttribute("hotelId", activeHotelId);
        model.addAttribute("bookingUrl", roomTypeId == null ? "/home#rooms-section" :
                "/booking/payment?roomTypeId=" + roomTypeId
                        + (activeHotelId == null ? "" : "&hotelId=" + activeHotelId)
                        + "&checkin=" + checkin + "&checkout=" + checkout);

        return "html/client-html/booking";
    }


    private void applyBranding(Model model, org.example.bookinghotels.entity.Hotels hotel) {
        String siteName = brandingService.get("site.name", "FEELHOME HOTEL");
        String siteLogo = brandingService.get("site.logo", brandingService.get("site.circleLogo", brandingService.get("site.headerLogo", "")));
        if (hotel != null && hotel.getId() != null) {
            String prefix = "hotel." + hotel.getId() + ".";
            if (hotel.getName() != null && !hotel.getName().isBlank()) siteName = hotel.getName();
            siteLogo = brandingService.get(prefix + "logo", hotel.getLogo() == null ? siteLogo : hotel.getLogo());
        }
        model.addAttribute("siteName", siteName);
        model.addAttribute("siteLogo", siteLogo);
    }

    private List<String> roomTypeImages(RoomType roomType) {
        if (roomType == null || roomType.getImages() == null) return List.of(FALLBACK_IMAGE);
        List<String> images = roomType.getImages().stream()
                .map(img -> img == null ? null : img.getImage())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(Collectors.toList());
        return images.isEmpty() ? List.of(FALLBACK_IMAGE) : images;
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
            model.addAttribute("siteName", brandingService.get("site.name", "FEELHOME HOTEL"));
            model.addAttribute("siteLogo", brandingService.get("site.logo", brandingService.get("site.circleLogo", brandingService.get("site.headerLogo", ""))));

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
            @RequestParam(required = false) Integer hotelId,
            Model model,
            RedirectAttributes redirectAttributes) {

        LocalDate start = LocalDate.parse(checkin);
        LocalDate end = LocalDate.parse(checkout);

        if (roomTypeId == null && roomId != null) {
            roomTypeId = roomRepository.findById(roomId)
                    .map(Room::getRoomType)
                    .filter(Objects::nonNull)
                    .map(RoomType::getId)
                    .orElse(null);
        }

        Room room = null;
        if (roomTypeId != null) {
            room = bookingService.findFirstAvailableRoomByType(roomTypeId, start, end);
            if (room != null) roomId = room.getId();
        } else if (roomId != null && bookingService.isRoomAvailable(roomId, start, end)) {
            room = roomRepository.findById(roomId).orElse(null);
            if (room != null && room.getRoomType() != null) roomTypeId = room.getRoomType().getId();
        }

        if (room == null || roomTypeId == null) {
            redirectAttributes.addFlashAttribute("error", "Loại phòng này đã hết phòng trong khoảng ngày đã chọn. Vui lòng đổi ngày hoặc chọn loại phòng khác.");
            String target = roomTypeId == null ? "?checkin=" + checkin + "&checkout=" + checkout : "?roomTypeId=" + roomTypeId + "&checkin=" + checkin + "&checkout=" + checkout;
            return "redirect:/booking/check" + target;
        }

        long availableCount = bookingService.countAvailableRoomsByType(roomTypeId, start, end);

        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0) days = 1;

        PromotionPricingService.PriceQuote quote = promotionPricingService.quote(room.getRoomType(), start, end);
        double originalPrice = quote.originalNightlyPrice();
        double discountPercent = quote.discountPercent();
        double discountedPrice = quote.effectiveNightlyPrice();
        double discountAmount = Math.max(0D, (originalPrice - discountedPrice) * days);
        double totalAmount = discountedPrice * days;

        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("roomId", roomId);
        bookingData.put("roomTypeId", roomTypeId);
        bookingData.put("availableCount", availableCount);
        bookingData.put("checkinDate", checkin);
        bookingData.put("checkoutDate", checkout);
        bookingData.put("days", days);
        bookingData.put("roomName", room.getRoomType().getNameType());
        bookingData.put("roomPrice", discountedPrice);
        bookingData.put("originalRoomPrice", originalPrice);
        bookingData.put("discountPercent", Math.round(discountPercent));
        bookingData.put("discountAmount", discountAmount);
        bookingData.put("hasPromotion", quote.promoted());
        bookingData.put("promotionName", quote.promotionName());
        bookingData.put("promotionEndAt", quote.promotionEndAt() == null ? null : quote.promotionEndAt().toString());
        bookingData.put("totalAmount", totalAmount);

        List<FwB> foodMenu = bookingService.getAllAvailableFoods();

        model.addAttribute("booking", bookingData);
        model.addAttribute("foodMenu", foodMenu);
        applyBranding(model, room.getRoomType() == null ? null : room.getRoomType().getHotels());

        return "html/client-html/payment";
    }

    @PostMapping("/confirm-payment")
    public String confirmPayment(
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) Integer roomTypeId,
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

            if (roomTypeId == null && roomId != null) {
                roomTypeId = roomRepository.findById(roomId)
                        .map(Room::getRoomType)
                        .filter(Objects::nonNull)
                        .map(RoomType::getId)
                        .orElse(null);
            }
            if (roomTypeId == null) {
                throw new IllegalArgumentException("Thiếu loại phòng cần đặt.");
            }

            RoomType targetRoomType = roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng"));
            Room availableRoom = bookingService.findFirstAvailableRoomByType(roomTypeId, start, end);
            if (availableRoom == null) {
                redirectAttributes.addFlashAttribute("error", "Loại phòng này đã hết phòng trong khoảng ngày đã chọn. Vui lòng đổi ngày hoặc chọn loại phòng khác.");
                return "redirect:/booking/check?roomTypeId=" + roomTypeId + "&checkin=" + checkinDate + "&checkout=" + checkoutDate;
            }

            Booking booking = new Booking();
            booking.setName(customerName);
            booking.setPhone(customerPhone);
            booking.setEmail(customerEmail);
            booking.setCheckinDate(start);
            booking.setCheckoutDate(end);

            BookingDetail detail = new BookingDetail();
            detail.setRoomType(targetRoomType);
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

            Booking savedBooking = bookingService.processBookingAutoAssign(
                    booking,
                    detail,
                    roomTypeId,
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
            String target = roomTypeId == null ? (roomId == null ? "" : "roomId=" + roomId) : "roomTypeId=" + roomTypeId;
            return "redirect:/booking/payment?" + target
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
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) Integer roomTypeId,
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

            if (roomTypeId == null && roomId != null) {
                roomTypeId = roomRepository.findById(roomId)
                        .map(Room::getRoomType)
                        .filter(Objects::nonNull)
                        .map(RoomType::getId)
                        .orElse(null);
            }
            if (roomTypeId == null) {
                response.put("success", false);
                response.put("message", "Thiếu loại phòng cần đặt.");
                return ResponseEntity.badRequest().body(response);
            }

            RoomType targetRoomType = roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng"));
            Room availableRoom = bookingService.findFirstAvailableRoomByType(roomTypeId, start, end);
            if (availableRoom == null) {
                response.put("success", false);
                response.put("message", "Loại phòng này đã hết phòng trong khoảng ngày đã chọn. Vui lòng đổi ngày hoặc chọn loại phòng khác.");
                return ResponseEntity.badRequest().body(response);
            }

            Booking booking = new Booking();
            booking.setName(customerName);
            booking.setPhone(customerPhone);
            booking.setEmail(customerEmail);
            booking.setCheckinDate(start);
            booking.setCheckoutDate(end);

            BookingDetail detail = new BookingDetail();
            detail.setRoomType(targetRoomType);
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

            Booking savedBooking = bookingService.processBookingAutoAssign(
                    booking,
                    detail,
                    roomTypeId,
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

    @PreAuthorize("isAuthenticated()")
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