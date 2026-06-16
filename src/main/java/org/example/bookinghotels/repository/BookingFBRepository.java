package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.BookingFB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingFBRepository extends JpaRepository<BookingFB, Integer> {

    // Tìm tất cả món đã gọi theo bookingDetailId
    List<BookingFB> findByBookingDetailId(Integer bookingDetailId);

    // Tìm theo bookingId (thông qua bookingDetail)
    List<BookingFB> findByBookingDetail_Booking_Id(Integer bookingId);
}