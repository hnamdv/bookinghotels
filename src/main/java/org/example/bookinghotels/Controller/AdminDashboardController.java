package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.BookingFB;
import org.example.bookinghotels.entity.FwB;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.example.bookinghotels.repository.BookingFBRepository;
import org.example.bookinghotels.repository.FwbRepository;
import org.example.bookinghotels.repository.RoomRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private FwbRepository fwbRepository;

    @Autowired
    private BookingFBRepository bookingFBRepository;

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

        long arrivalsToday = allDetails.stream()
                .filter(bd -> bd.getBooking().getCheckinDate().equals(today)).count();
        long departuresToday = allDetails.stream()
                .filter(bd -> bd.getBooking().getCheckoutDate().equals(today)).count();
        long totalRooms = roomTypeRepository.count();
        long occupiedRooms = allDetails.stream()
                .filter(bd -> !bd.getBooking().getCheckinDate().isAfter(today) && bd.getBooking().getCheckoutDate().isAfter(today))
                .count();
        int occupancy = totalRooms == 0 ? 0 : (int) (occupiedRooms * 100 / totalRooms);
        double avgDailyRate = allDetails.stream()
                .mapToDouble(bd -> bd.getRoomType().getPrice())
                .average()
                .orElse(0.0);

        List<BookingDetail> recentLogs = filteredDetails.stream()
                .sorted(Comparator.comparing(bd -> bd.getBooking().getBookingDate(), Comparator.reverseOrder()))
                .limit(5)
                .collect(Collectors.toList());

        List<String> forecastDays = new ArrayList<>();
        List<Integer> forecastData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            forecastDays.add(date.format(formatter));
            long occ = allDetails.stream()
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
    // CÁC API AJAX XỬ LÝ THAO TÁC TRÊN GIAO DIỆN QUẢN LÝ ĐẶT PHÒNG
    // =========================================================

    // 1. Cập nhật trạng thái đã thanh toán
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

    // 2. Duyệt đơn đặt phòng & gán phòng
    @PutMapping("/api/booking-details/{id}/approve")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approveBooking(@PathVariable Integer id, @RequestParam(required = false) Integer roomId) {
        Map<String, Object> res = new HashMap<>();
        try {
            BookingDetail detail = bookingDetailRepository.findById(id).orElse(null);
            if (detail != null) {
                detail.setStatus("CONFIRMED");
                if (roomId != null) {
                    Room room = roomRepository.findById(roomId).orElse(null);
                    detail.setRoom(room);
                }
                bookingDetailRepository.save(detail);
                res.put("success", true);
                res.put("message", "Đã duyệt đơn và gán phòng thành công!");
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

    // 3. Hủy đơn đặt phòng
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

    // 4. Sửa thông tin trực tiếp (Tên, SĐT, số khách)
    @PutMapping("/api/booking-details/{id}/update-field")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateField(@PathVariable Integer id, @RequestParam String field, @RequestParam String value) {
        Map<String, Object> res = new HashMap<>();
        try {
            BookingDetail detail = bookingDetailRepository.findById(id).orElse(null);
            if (detail != null && detail.getBooking() != null) {
                Booking b = detail.getBooking();
                if ("name".equals(field)) b.setName(value);
                else if ("phone".equals(field)) b.setPhone(value);
                else if ("guests".equals(field)) {
                    String[] parts = value.split(",");
                    detail.setAdultCount(Integer.parseInt(parts[0]));
                    if (parts.length > 1) detail.setChildCount(Integer.parseInt(parts[1]));
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

    // 5. Lấy danh sách phòng trống để gán cho đơn
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
            List<Room> rooms = roomRepository.findByRoomTypeId(roomTypeId);
            res.put("success", true);
            res.put("rooms", rooms);
            res.put("totalRooms", rooms.size());
            res.put("availableCount", rooms.size());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // 6. Lấy danh sách F&B cho popup
    @GetMapping("/api/booking-details/{id}/foods")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getFoodsForDetail(@PathVariable Integer id) {
        try {
            List<FwB> allFwb = fwbRepository.findAll();
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
                item.put("price", fwb.getPrice());
                item.put("quantity", orderedMap.getOrDefault(fwb.getId(), 0));
                result.add(item);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    // 7. Lưu F&B phụ thu
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
                Integer fwbId = (Integer) map.get("fwbId");
                Integer qty = (Integer) map.get("quantity");
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
}