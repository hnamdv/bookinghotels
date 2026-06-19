package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.BookingDetailRepository;
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
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

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
}