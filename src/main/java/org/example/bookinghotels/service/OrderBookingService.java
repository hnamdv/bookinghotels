package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.*;

import java.util.List;

public interface OrderBookingService {
    Booking processBooking(Booking booking, BookingDetail detail, List<BookingFB> orderedFoods, String paymentMethod);
    List<Invoices> getAllInvoices();
    List<FwB> getAllAvailableFoods();
}