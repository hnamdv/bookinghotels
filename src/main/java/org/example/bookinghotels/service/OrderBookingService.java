package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.*;
import java.time.LocalDate;
import java.util.List;

public interface OrderBookingService {
    Booking processBooking(Booking booking, BookingDetail detail, List<BookingFB> orderedFoods, String paymentMethod);
    List<Invoices> getAllInvoices();
    List<FwB> getAllAvailableFoods();

    // Bao//
    boolean isRoomAvailable(Integer roomId, LocalDate checkinDate, LocalDate checkoutDate);
    List<Integer> getAvailableRooms(List<Integer> allRoomIds, LocalDate checkinDate, LocalDate checkoutDate);
    void validateBooking(Integer roomId, LocalDate checkinDate, LocalDate checkoutDate);

    // --- THÊM HÀM ĐỂ FIX LỖI Ở BANKWEBHOOKCONTROLLER ---
    void updateStatusToPaid(String bookingIdStr);
    Booking getBookingById(String bookingIdStr);
    Invoices findInvoiceByBookingId(Long bookingId);

    // =====================================================
    // 🌟 THÊM HÀM NÀY ĐỂ XỬ LÝ HỦY GIỮ PHÒNG KHI BẤM HỦY QR
    // =====================================================
    void updateBookingStatus(Long bookingId, String status);
}