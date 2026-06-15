package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.example.bookinghotels.repository.BookingRepository;
import org.example.bookinghotels.repository.RoomRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @GetMapping("/bookings")
    public String bookingManagement(
            Model model,
            @RequestParam(required = false) Integer roomTypeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate today = LocalDate.now();

        // Giá trị mặc định cho filter (dùng final để an toàn trong lambda)
        final LocalDate finalStartDate = (startDate != null) ? startDate : today.minusDays(30);
        final LocalDate finalEndDate = (endDate != null) ? endDate : today.plusDays(30);

        // Lấy toàn bộ booking details (có fetch join)
        List<BookingDetail> allDetails = bookingDetailRepository.findAllWithDetails();

        // ---- Lọc dữ liệu cho bảng (dùng stream) ----
        List<BookingDetail> filteredDetails = allDetails.stream()
                .filter(bd -> roomTypeId == null || roomTypeId <= 0 || (bd.getRoomType() != null && roomTypeId.equals(bd.getRoomType().getId())))
                .filter(bd -> {
                    if (status == null || status.isEmpty()) return true;
                    LocalDate ci = bd.getBooking().getCheckinDate();
                    LocalDate co = bd.getBooking().getCheckoutDate();
                    switch (status.toUpperCase()) {
                        case "CONFIRMED": return !co.isBefore(today) && !ci.isAfter(today);
                        case "CHECKED":   return co.isBefore(today);
                        case "PENDING":   return ci.isAfter(today);
                        default: return true;
                    }
                })
                .filter(bd -> !bd.getBooking().getCheckinDate().isBefore(finalStartDate))
                .filter(bd -> !bd.getBooking().getCheckinDate().isAfter(finalEndDate))
                .sorted((a, b) -> b.getBooking().getBookingDate().compareTo(a.getBooking().getBookingDate()))
                .collect(Collectors.toList());

        // ---- Thống kê (dùng toàn bộ dữ liệu không qua filter) ----
        long arrivalsToday = allDetails.stream()
                .filter(bd -> bd.getBooking().getCheckinDate().equals(today)).count();
        long departuresToday = allDetails.stream()
                .filter(bd -> bd.getBooking().getCheckoutDate().equals(today)).count();
        long totalRooms = roomRepository.count();
        long occupiedRooms = countOccupiedRoomsOnDate(today, allDetails);
        int occupancy = totalRooms == 0 ? 0 : (int) (occupiedRooms * 100 / totalRooms);
        double avgDailyRate = allDetails.stream()
                .mapToDouble(BookingDetail::getPrice).average().orElse(0.0);

        // ---- Recent logs (5 đơn mới nhất từ filtered) ----
        List<BookingDetail> recentLogs = filteredDetails.stream().limit(5).collect(Collectors.toList());

        // ---- Occupancy Forecast (7 ngày tới, dùng allDetails) ----
        List<String> forecastDays = new ArrayList<>();
        List<Integer> forecastData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            forecastDays.add(date.format(formatter));
            long occ = countOccupiedRoomsOnDate(date, allDetails);
            int per = totalRooms == 0 ? 0 : (int) (occ * 100 / totalRooms);
            forecastData.add(per);
        }

        // ---- Danh sách loại phòng cho dropdown filter ----
        List<RoomType> roomTypes = roomTypeRepository.findAll();

        // ---- Đưa dữ liệu vào model ----
        model.addAttribute("arrivalsToday", arrivalsToday);
        model.addAttribute("departuresToday", departuresToday);
        model.addAttribute("occupancy", occupancy);
        model.addAttribute("avgDailyRate", String.format("%.0f", avgDailyRate));
        model.addAttribute("bookingDetails", filteredDetails);
        model.addAttribute("totalBookings", filteredDetails.size());
        model.addAttribute("recentLogs", recentLogs);
        model.addAttribute("forecastDays", forecastDays);
        model.addAttribute("forecastData", forecastData);
        model.addAttribute("roomTypes", roomTypes);

        // Giữ lại giá trị filter để hiển thị trên form
        model.addAttribute("selectedRoomTypeId", roomTypeId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedStartDate", finalStartDate);
        model.addAttribute("selectedEndDate", finalEndDate);

        return "html/admin-html/booking";
    }

    // Hàm đếm số phòng có khách trong một ngày (dùng allDetails)
    private long countOccupiedRoomsOnDate(LocalDate date, List<BookingDetail> allDetails) {
        return allDetails.stream()
                .filter(bd -> {
                    LocalDate ci = bd.getBooking().getCheckinDate();
                    LocalDate co = bd.getBooking().getCheckoutDate();
                    return (date.isEqual(ci) || date.isAfter(ci)) && date.isBefore(co);
                })
                .map(bd -> bd.getRoom() != null ? bd.getRoom().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }
}