package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Integer> {

    // THÊM DÒNG NÀY - Tìm BookingDetail theo bookingId
    List<BookingDetail> findByBookingId(Integer bookingId);
}