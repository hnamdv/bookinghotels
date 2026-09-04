package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.BookingFB;
import org.example.bookinghotels.entity.FwB;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.example.bookinghotels.repository.BookingFBRepository;
import org.example.bookinghotels.repository.BookingRepository;
import org.example.bookinghotels.repository.FwbRepository;
import org.example.bookinghotels.repository.InvoicesRepository;
import org.example.bookinghotels.repository.RoomRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.example.bookinghotels.entity.Invoices;
import org.example.bookinghotels.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private FwbRepository fwbRepository;

    @Autowired
    private BookingFBRepository bookingFBRepository;

    @Autowired
    private InvoicesRepository invoicesRepository;

    @Autowired
    private EmailService emailService;

    @GetMapping("/bookings")
    public String bookingManagement(Model model,
                                    @RequestParam(required = false) Integer roomTypeId,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate today = LocalDate.now();

        if (status != null && status.isBlank()) {
            status = null;
        }

        List<BookingDetail> filteredDetails;
        if (roomTypeId != null || status != null || startDate != null || endDate != null) {
            filteredDetails = bookingDetailRepository.filterBookings(roomTypeId, status, startDate, endDate);
        } else {
            filteredDetails = bookingDetailRepository.findAllWithDetails();
        }

        List<BookingDetail> allDetails = bookingDetailRepository.findAllWithDetails();

        // ===== THỐNG KÊ =====
        long arrivalsToday = allDetails.stream()
                .filter(bd -> bd.getBooking() != null && bd.getBooking().getCheckinDate() != null)
                .filter(bd -> bd.getBooking().getCheckinDate().equals(today))
                .count();

        long departuresToday = allDetails.stream()
                .filter(bd -> bd.getBooking() != null && bd.getBooking().getCheckoutDate() != null)
                .filter(bd -> bd.getBooking().getCheckoutDate().equals(today))
                .count();

        long totalRooms = roomRepository.count();

        long occupiedRooms = allDetails.stream()
                .filter(bd -> bd.getStatus() != null && !"CANCELLED".equals(bd.getStatus()) && !"CHECKED_OUT".equals(bd.getStatus()))
                .filter(bd -> bd.getBooking() != null && bd.getBooking().getCheckinDate() != null && bd.getBooking().getCheckoutDate() != null)
                .filter(bd -> !bd.getBooking().getCheckinDate().isAfter(today) && bd.getBooking().getCheckoutDate().isAfter(today))
                .count();

        int occupancy = totalRooms == 0 ? 0 : (int) (occupiedRooms * 100 / totalRooms);

        double avgDailyRate = allDetails.stream()
                .filter(bd -> bd.getRoomType() != null && bd.getRoomType().getPrice() != null)
                .mapToDouble(bd -> bd.getRoomType().getPrice())
                .average()
                .orElse(0.0);

        List<BookingDetail> recentLogs = filteredDetails.stream()
                .filter(bd -> bd.getBooking() != null && bd.getBooking().getBookingDate() != null)
                .sorted(Comparator.comparing(bd -> bd.getBooking().getBookingDate(), Comparator.reverseOrder()))
                .limit(5)
                .collect(Collectors.toList());

        // ===== DỰ BÁO LẤP ĐẦY 7 NGÀY =====
        List<String> forecastDays = new ArrayList<>();
        List<Integer> forecastData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            forecastDays.add(date.format(formatter));

            long occ = allDetails.stream()
                    .filter(bd -> bd.getStatus() != null && !"CANCELLED".equals(bd.getStatus()) && !"CHECKED_OUT".equals(bd.getStatus()))
                    .filter(bd -> bd.getBooking() != null && bd.getBooking().getCheckinDate() != null && bd.getBooking().getCheckoutDate() != null)
                    .filter(bd -> !bd.getBooking().getCheckinDate().isAfter(date) && bd.getBooking().getCheckoutDate().isAfter(date))
                    .count();

            int per = totalRooms == 0 ? 0 : (int) (occ * 100 / totalRooms);
            forecastData.add(per);
        }

        List<RoomType> roomTypes = roomTypeRepository.findAll();

        model.addAttribute("bookingDetails", filteredDetails);
        model.addAttribute("totalBookings", filteredDetails.size());
        model.addAttribute("arrivalsToday", arrivalsToday);
        model.addAttribute("departuresToday", departuresToday);
        model.addAttribute("occupancy", occupancy);
        model.addAttribute("avgDailyRate", String.format("%.0f", avgDailyRate));
        model.addAttribute("recentLogs", recentLogs);
        model.addAttribute("forecastDays", forecastDays);
        model.addAttribute("forecastData", forecastData);
        model.addAttribute("roomTypes", roomTypes);
        model.addAttribute("selectedRoomTypeId", roomTypeId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedStartDate", startDate);
        model.addAttribute("selectedEndDate", endDate);

        return "html/admin-html/booking";
    }

    // =========================================================
    // API TẠO BOOKING TẠI QUẦY (WALK-IN)
    // =========================================================
    @PostMapping("/walk-in")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createWalkInBooking(@RequestBody Map<String, Object> data) {
        Map<String, Object> res = new HashMap<>();

        try {
            System.out.println("=== WALK-IN DATA: " + data + " ===");

            Integer roomTypeId = getInteger(data.get("roomTypeId"));
            String customerName = (String) data.get("customerName");
            String customerPhone = (String) data.get("customerPhone");
            String customerEmail = data.get("customerEmail") != null ? data.get("customerEmail").toString() : "";
            Integer adultCount = getInteger(data.get("adultCount"));
            Integer childCount = getInteger(data.get("childCount"));
            String checkinStr = (String) data.get("checkinDate");
            String checkoutStr = (String) data.get("checkoutDate");
            String paymentMethod = data.get("paymentMethod") != null ? data.get("paymentMethod").toString() : "TIEN_MAT";

            // Validate
            if (roomTypeId == null) {
                res.put("success", false);
                res.put("message", "Vui lòng chọn loại phòng");
                return ResponseEntity.badRequest().body(res);
            }
            if (customerName == null || customerName.isBlank()) {
                res.put("success", false);
                res.put("message", "Vui lòng nhập tên khách hàng");
                return ResponseEntity.badRequest().body(res);
            }
            if (customerPhone == null || customerPhone.isBlank()) {
                res.put("success", false);
                res.put("message", "Vui lòng nhập số điện thoại");
                return ResponseEntity.badRequest().body(res);
            }
            if (checkinStr == null || checkoutStr == null) {
                res.put("success", false);
                res.put("message", "Vui lòng chọn ngày nhận và trả phòng");
                return ResponseEntity.badRequest().body(res);
            }

            LocalDate checkinDate = LocalDate.parse(checkinStr);
            LocalDate checkoutDate = LocalDate.parse(checkoutStr);

            if (!checkoutDate.isAfter(checkinDate)) {
                res.put("success", false);
                res.put("message", "Ngày trả phòng phải sau ngày nhận phòng");
                return ResponseEntity.badRequest().body(res);
            }

            RoomType roomType = roomTypeRepository.findById(roomTypeId).orElse(null);
            if (roomType == null) {
                res.put("success", false);
                res.put("message", "Loại phòng không tồn tại");
                return ResponseEntity.badRequest().body(res);
            }

            // Tạo Booking
            Booking booking = new Booking();
            booking.setName(customerName);
            booking.setPhone(customerPhone);
            booking.setEmail(customerEmail);
            booking.setCheckinDate(checkinDate);
            booking.setCheckoutDate(checkoutDate);
            booking.setBookingDate(LocalDateTime.now());
            booking.setDeleteAt(false);

            Booking savedBooking = bookingRepository.save(booking);
            System.out.println("✅ Saved booking ID: " + savedBooking.getId());

            // Tạo BookingDetail
            BookingDetail detail = new BookingDetail();
            detail.setBooking(savedBooking);
            detail.setRoomType(roomType);
            detail.setAdultCount(adultCount != null ? adultCount : 1);
            detail.setChildCount(childCount != null ? childCount : 0);
            detail.setRoomQuantity(1);
            detail.setPrice(roomType.getPrice() != null ? roomType.getPrice() : 0);
            detail.setDiscountAmount(0.0);
            detail.setStatus("PENDING");
            detail.setDeleteAt(false);

            // Gán phòng nếu có
            List<Room> availableRooms = roomRepository.findAvailableRooms(roomTypeId, checkinDate, checkoutDate);
            if (availableRooms != null && !availableRooms.isEmpty()) {
                detail.setRoom(availableRooms.get(0));
            }

            BookingDetail savedDetail = bookingDetailRepository.save(detail);
            System.out.println("✅ Saved booking detail ID: " + savedDetail.getId());

            // Tính tổng tiền
            double totalAmount = roomType.getPrice() != null ? roomType.getPrice() : 0;

            // Tạo Invoice - KHÔNG setUser vì user null
            try {
                Invoices invoice = new Invoices();
                invoice.setBooking(savedBooking);
                invoice.setTotalAmount(totalAmount);
                invoice.setPaymentMethod(paymentMethod);
                invoice.setPaymentStatus("PENDING");
                // ❌ KHÔNG SET USER
                invoicesRepository.save(invoice);
                System.out.println("✅ Saved invoice");
            } catch (Exception e) {
                System.err.println("⚠️ Không tạo được invoice: " + e.getMessage());
            }

            res.put("success", true);
            res.put("message", "Tạo đơn đặt phòng thành công!");
            res.put("bookingId", savedBooking.getId());
            res.put("bookingDetailId", savedDetail.getId());
            res.put("totalAmount", totalAmount);
            res.put("assignedRoom", detail.getRoom() != null ? detail.getRoom().getRoomNumber() : null);

            return ResponseEntity.ok(res);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ LỖI WALK-IN: " + e.getMessage());
            res.put("success", false);
            res.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // =========================================================
    // CÁC API AJAX XỬ LÝ THAO TÁC TRÊN GIAO DIỆN QUẢN LÝ ĐẶT PHÒNG
    // =========================================================

    // API lấy thông tin chi tiết booking
    @GetMapping("/api/booking-details/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBookingDetail(@PathVariable Integer id) {
        Map<String, Object> res = new HashMap<>();
        try {
            BookingDetail detail = bookingDetailRepository.findById(id).orElse(null);
            if (detail == null) {
                res.put("success", false);
                res.put("message", "Không tìm thấy booking detail");
                return ResponseEntity.badRequest().body(res);
            }

            res.put("success", true);
            res.put("id", detail.getId());
            res.put("status", detail.getStatus());
            res.put("bookingId", detail.getBooking() != null ? detail.getBooking().getId() : null);
            res.put("customerName", detail.getBooking() != null ? detail.getBooking().getName() : null);
            res.put("customerPhone", detail.getBooking() != null ? detail.getBooking().getPhone() : null);
            res.put("customerEmail", detail.getBooking() != null ? detail.getBooking().getEmail() : null);

            if (detail.getRoom() != null) {
                res.put("roomId", detail.getRoom().getId());
                res.put("roomNumber", detail.getRoom().getRoomNumber());
            } else {
                res.put("roomId", null);
                res.put("roomNumber", null);
            }

            if (detail.getRoomType() != null) {
                res.put("roomTypeId", detail.getRoomType().getId());
                res.put("roomTypeName", detail.getRoomType().getNameType());
            }

            if (detail.getBooking() != null) {
                res.put("checkinDate", detail.getBooking().getCheckinDate() != null ? detail.getBooking().getCheckinDate().toString() : null);
                res.put("checkoutDate", detail.getBooking().getCheckoutDate() != null ? detail.getBooking().getCheckoutDate().toString() : null);
            }

            res.put("adultCount", detail.getAdultCount());
            res.put("childCount", detail.getChildCount());
            res.put("price", detail.getPrice());

            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // API cập nhật trạng thái đã thanh toán
    @PutMapping("/api/booking-details/{id}/mark-paid")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markPaid(@PathVariable Integer id) {
        Map<String, Object> res = new HashMap<>();
        try {
            BookingDetail detail = bookingDetailRepository.findById(id).orElse(null);
            if (detail != null) {
                detail.setStatus("PAID");
                bookingDetailRepository.save(detail);
                res.put("success", true);
                res.put("message", "Đã cập nhật trạng thái thanh toán thành công!");
                emailService.sendStatusChangeNotification(detail, "PAID");
                return ResponseEntity.ok(res);
            }
            res.put("success", false);
            res.put("message", "Không tìm thấy chi tiết đặt phòng");
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // API duyệt đơn đặt phòng & gán phòng
    @PutMapping("/api/booking-details/{id}/approve")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approveBooking(@PathVariable Integer id, @RequestParam(required = false) Integer roomId) {
        Map<String, Object> res = new HashMap<>();
        try {
            BookingDetail detail = bookingDetailRepository.findById(id).orElse(null);
            if (detail != null) {
                detail.setStatus("APPROVED");
                if (roomId != null) {
                    Room room = roomRepository.findById(roomId).orElse(null);
                    detail.setRoom(room);
                }
                bookingDetailRepository.save(detail);
                res.put("success", true);
                res.put("message", "Đã duyệt đơn và gán phòng thành công!");
                emailService.sendStatusChangeNotification(detail, "APPROVED");
                return ResponseEntity.ok(res);
            }
            res.put("success", false);
            res.put("message", "Không tìm thấy đơn đặt phòng");
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // API hủy đơn đặt phòng
    @PutMapping("/api/booking-details/{id}/reject")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectBooking(@PathVariable Integer id) {
        Map<String, Object> res = new HashMap<>();
        try {
            BookingDetail detail = bookingDetailRepository.findById(id).orElse(null);
            if (detail != null) {
                detail.setStatus("CANCELLED");
                bookingDetailRepository.save(detail);
                res.put("success", true);
                res.put("message", "Đã hủy đơn thành công!");
                emailService.sendStatusChangeNotification(detail, "CANCELLED");
                return ResponseEntity.ok(res);
            }
            res.put("success", false);
            res.put("message", "Không tìm thấy đơn đặt phòng");
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // API sửa thông tin trực tiếp
    @PutMapping("/api/booking-details/{id}/update-field")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateField(@PathVariable Integer id, @RequestParam String field, @RequestParam String value) {
        Map<String, Object> res = new HashMap<>();
        try {
            BookingDetail detail = bookingDetailRepository.findById(id).orElse(null);
            if (detail != null && detail.getBooking() != null) {
                Booking b = detail.getBooking();
                if ("name".equals(field)) {
                    b.setName(value);
                } else if ("phone".equals(field)) {
                    b.setPhone(value);
                } else if ("guests".equals(field)) {
                    String[] parts = value.split(",");
                    detail.setAdultCount(Integer.parseInt(parts[0]));
                    if (parts.length > 1) {
                        detail.setChildCount(Integer.parseInt(parts[1]));
                    }
                }
                bookingDetailRepository.save(detail);
                res.put("success", true);
                res.put("message", "Cập nhật thành công!");
                return ResponseEntity.ok(res);
            }
            res.put("success", false);
            res.put("message", "Không tìm thấy thông tin");
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // API lấy danh sách phòng trống
    @GetMapping("/api/booking-details/{id}/available-rooms")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAvailableRoomsForDetail(@PathVariable Integer id) {
        Map<String, Object> res = new HashMap<>();
        try {
            BookingDetail detail = bookingDetailRepository.findById(id).orElse(null);
            if (detail == null || detail.getRoomType() == null) {
                res.put("success", false);
                res.put("rooms", Collections.emptyList());
                return ResponseEntity.ok(res);
            }
            Integer roomTypeId = detail.getRoomType().getId();
            List<Room> rooms = roomRepository.findAvailableRooms(
                    roomTypeId,
                    detail.getBooking().getCheckinDate(),
                    detail.getBooking().getCheckoutDate()
            );

            List<Map<String, Object>> roomList = rooms.stream().map(r -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", r.getId());
                map.put("roomNumber", r.getRoomNumber());
                map.put("slug", r.getSlug());
                map.put("thumbnail", r.getThumbnail());
                return map;
            }).toList();

            res.put("success", true);
            res.put("rooms", roomList);
            res.put("totalRooms", roomList.size());
            res.put("availableCount", roomList.size());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // API lấy danh sách F&B
    @GetMapping("/api/booking-details/{id}/foods")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFoodsForDetail(@PathVariable Integer id) {
        Map<String, Object> res = new HashMap<>();
        try {
            List<FwB> allFwb = fwbRepository.findAll().stream()
                    .filter(f -> f != null)
                    .filter(f -> f.getStatus() == null || f.getStatus().isBlank() || "ACTIVE".equalsIgnoreCase(f.getStatus()))
                    .filter(f -> f.getPrice() > 0D)
                    .toList();

            List<BookingFB> ordered = bookingFBRepository.findByBookingDetailId(id);
            Map<Integer, Integer> orderedMap = new HashMap<>();
            for (BookingFB fb : ordered) {
                if (fb.getFwb() != null) {
                    orderedMap.put(fb.getFwb().getId(), fb.getQuantity());
                }
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (FwB fwb : allFwb) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", fwb.getId());
                item.put("name", fwb.getName() != null ? fwb.getName() : fwb.getDescription());
                item.put("description", fwb.getDescription());
                item.put("price", fwb.getPrice());
                item.put("quantity", orderedMap.getOrDefault(fwb.getId(), 0));
                result.add(item);
            }

            res.put("success", true);
            res.put("foods", result);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("foods", Collections.emptyList());
            return ResponseEntity.ok(res);
        }
    }

    // API lưu F&B phụ thu
    @PostMapping("/api/booking-details/{id}/foods")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveFoodsForDetail(@PathVariable Integer id, @RequestBody List<Map<String, Object>> items) {
        Map<String, Object> res = new HashMap<>();

        try {
            BookingDetail detail = bookingDetailRepository.findById(id).orElse(null);
            if (detail == null) {
                res.put("success", false);
                res.put("message", "Không tìm thấy chi tiết đặt phòng");
                return ResponseEntity.badRequest().body(res);
            }
            List<BookingFB> existing = bookingFBRepository.findByBookingDetailId(id);
            bookingFBRepository.deleteAll(existing);
            for (Map<String, Object> map : items) {
                Integer fwbId = null;
                Integer qty = null;
                Object fwbIdObj = map.get("fwbId");
                Object qtyObj = map.get("quantity");
                if (fwbIdObj instanceof Number) {
                    fwbId = ((Number) fwbIdObj).intValue();
                } else if (fwbIdObj instanceof String) {
                    try { fwbId = Integer.parseInt((String) fwbIdObj); } catch (NumberFormatException e) {}
                }
                if (qtyObj instanceof Number) {
                    qty = ((Number) qtyObj).intValue();
                } else if (qtyObj instanceof String) {
                    try { qty = Integer.parseInt((String) qtyObj); } catch (NumberFormatException e) {}
                }
                if (fwbId != null && qty != null && qty > 0) {
                    FwB fwb = fwbRepository.findById(fwbId).orElse(null);
                    if (fwb != null) {
                        BookingFB fb = new BookingFB();
                        fb.setBookingDetail(detail);
                        fb.setFwb(fwb);
                        fb.setQuantity(qty);
                        fb.setPriceAtOrder(fwb.getPrice());
                        bookingFBRepository.save(fb);
                    }
                }
            }
            res.put("success", true);
            res.put("message", "Đã cập nhật phụ thu thành công!");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // API check-out
    @PutMapping("/api/booking-details/{id}/check-out")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkOutBooking(@PathVariable Integer id) {
        Map<String, Object> res = new HashMap<>();
        try {
            BookingDetail detail = bookingDetailRepository.findById(id).orElse(null);
            if (detail != null) {
                detail.setStatus("CHECKED_OUT");
                bookingDetailRepository.save(detail);
                res.put("success", true);
                res.put("message", "Đã trả phòng thành công!");
                emailService.sendStatusChangeNotification(detail, "CHECKED_OUT");
                return ResponseEntity.ok(res);
            }
            res.put("success", false);
            res.put("message", "Không tìm thấy đơn đặt phòng");
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }
    // API check-in
    @PutMapping("/api/booking-details/{id}/check-in")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkInBooking(@PathVariable Integer id) {
        Map<String, Object> res = new HashMap<>();
        try {
            BookingDetail detail = bookingDetailRepository.findById(id).orElse(null);
            if (detail != null) {
                detail.setStatus("CHECKED_IN");
                bookingDetailRepository.save(detail);
                res.put("success", true);
                res.put("message", "Đã nhận phòng!");
                emailService.sendStatusChangeNotification(detail, "CHECKED_IN");
                return ResponseEntity.ok(res);
            }
            res.put("success", false);
            res.put("message", "Không tìm thấy đơn đặt phòng");
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // Helper method
    private Integer getInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}