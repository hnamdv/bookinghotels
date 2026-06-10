package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.example.bookinghotels.repository.BookingRepository;
import org.example.bookinghotels.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private RoomRepository roomRepository;

    @GetMapping("/bookings")
    public String bookingManagement(Model model) {
        LocalDate today = LocalDate.now();

        // 1. Arrivals Today
        long arrivalsToday = bookingRepository.findAll().stream()
                .filter(b -> b.getCheckinDate().equals(today))
                .count();

        // 2. Departures Today
        long departuresToday = bookingRepository.findAll().stream()
                .filter(b -> b.getCheckoutDate().equals(today))
                .count();

        // 3. Occupancy
        long totalRooms = roomRepository.count();
        long occupiedRooms = countOccupiedRoomsOnDate(today);
        int occupancy = totalRooms == 0 ? 0 : (int) (occupiedRooms * 100 / totalRooms);

        // 4. Avg Daily Rate
        Double avgDailyRate = getAvgDailyRate();
        if (avgDailyRate == null) avgDailyRate = 0.0;

        // 5. Booking Details
        List<BookingDetail> bookingDetails = new ArrayList<>();
        try {
            bookingDetails = bookingDetailRepository.findAllWithDetails();
            bookingDetails.sort((a, b) -> b.getBooking().getBookingDate().compareTo(a.getBooking().getBookingDate()));
        } catch (Exception e) {
            System.err.println("Lỗi lấy dữ liệu: " + e.getMessage());
        }

        // 6. Recent logs
        List<Booking> recentLogs = bookingRepository.findAll();
        recentLogs.sort((a, b) -> b.getBookingDate().compareTo(a.getBookingDate()));
        recentLogs = recentLogs.size() > 5 ? recentLogs.subList(0, 5) : recentLogs;

        // 7. Forecast
        List<String> forecastDays = new ArrayList<>();
        List<Integer> forecastData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            forecastDays.add(date.format(formatter));
            long occupied = countOccupiedRoomsOnDate(date);
            int percentage = totalRooms == 0 ? 0 : (int) (occupied * 100 / totalRooms);
            forecastData.add(percentage);
        }

        model.addAttribute("arrivalsToday", arrivalsToday);
        model.addAttribute("departuresToday", departuresToday);
        model.addAttribute("occupancy", occupancy);
        model.addAttribute("avgDailyRate", String.format("%.0f", avgDailyRate));
        model.addAttribute("bookingDetails", bookingDetails);
        model.addAttribute("totalBookings", bookingDetails.size());
        model.addAttribute("recentLogs", recentLogs);
        model.addAttribute("forecastDays", forecastDays);
        model.addAttribute("forecastData", forecastData);  // ĐÃ SỬA: Xóa khoảng trắng

        return "html/admin-html/booking";
    }

    private long countOccupiedRoomsOnDate(LocalDate date) {
        try {
            List<BookingDetail> allDetails = bookingDetailRepository.findAllWithDetails();
            return allDetails.stream()
                    .filter(bd -> {
                        LocalDate checkin = bd.getBooking().getCheckinDate();
                        LocalDate checkout = bd.getBooking().getCheckoutDate();
                        return (date.isEqual(checkin) || date.isAfter(checkin)) && date.isBefore(checkout);
                    })
                    .map(bd -> bd.getRoom().getId())
                    .distinct()
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    private Double getAvgDailyRate() {
        try {
            List<BookingDetail> allDetails = bookingDetailRepository.findAllWithDetails();
            return allDetails.stream()
                    .mapToDouble(BookingDetail::getPrice)
                    .average()
                    .orElse(0.0);
        } catch (Exception e) {
            return 0.0;
        }
    }
}