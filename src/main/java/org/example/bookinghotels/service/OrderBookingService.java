package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.*;
import java.time.LocalDate;
import java.util.List;

public interface OrderBookingService {
    Booking processBooking(Booking booking, BookingDetail detail, List<BookingFB> orderedFoods, String paymentMethod);
    Booking processBookingAutoAssign(Booking booking, BookingDetail detail, Integer roomTypeId, List<BookingFB> orderedFoods, String paymentMethod);
    List<Invoices> getAllInvoices();
    List<FwB> getAllAvailableFoods();

    // Bao//
    boolean isRoomAvailable(Integer roomId, LocalDate checkinDate, LocalDate checkoutDate);
    List<Integer> getAvailableRooms(List<Integer> allRoomIds, LocalDate checkinDate, LocalDate checkoutDate);
    Room findFirstAvailableRoomByType(Integer roomTypeId, LocalDate checkinDate, LocalDate checkoutDate);
    long countAvailableRoomsByType(Integer roomTypeId, LocalDate checkinDate, LocalDate checkoutDate);
    void validateBooking(Integer roomId, LocalDate checkinDate, LocalDate checkoutDate);

    // --- CÁC HÀM XỬ LÝ THANH TOÁN & TRẠNG THÁI ---
    void updateStatusToPaid(String bookingIdStr);
    Booking getBookingById(String bookingIdStr);
    Invoices findInvoiceByBookingId(Long bookingId);

    // --- BỔ SUNG ĐỂ FIX LỖI Ở BOOKINGCONTROLLER ---
    void updateBookingStatus(Long bookingId, String status);
}