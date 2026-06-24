package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.BookingFB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;  // ← Thêm import này

@Repository
public interface BookingFBRepository extends JpaRepository<BookingFB, Integer> {

    // Thêm method này
    List<BookingFB> findByBookingDetailId(Integer bookingDetailId);
}