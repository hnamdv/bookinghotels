package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.*;

import java.time.LocalDate;
import java.util.List;

public interface OrderBookingService {
    Booking processBooking(Booking booking, BookingDetail detail, List<BookingFB> orderedFoods, String paymentMethod);
    List<Invoices> getAllInvoices();
    List<FwB> getAllAvailableFoods();
    //Bao//
    // Kiểm tra phòng có trống không
    boolean isRoomAvailable(Integer roomId, LocalDate checkinDate, LocalDate checkoutDate);
    // Lấy danh sách phòng trống (loại trừ phòng gối lịch)
    List<Integer> getAvailableRooms(List<Integer> allRoomIds, LocalDate checkinDate, LocalDate checkoutDate);
    // Validate booking (throw Exception nếu gối lịch)
    void validateBooking(Integer roomId, LocalDate checkinDate, LocalDate checkoutDate);
}