package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.BookingFB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingFBRepository extends JpaRepository<BookingFB, Integer> {

    // Tìm BookingFB theo BookingDetail ID
    @Query("SELECT bf FROM BookingFB bf WHERE bf.bookingDetail.id = :bookingDetailId")
    List<BookingFB> findByBookingDetailId(@Param("bookingDetailId") Integer bookingDetailId);
}