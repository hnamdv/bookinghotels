package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.BookingFB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BookingFBRepository extends JpaRepository<BookingFB, Integer> {

    List<BookingFB> findByBookingDetailId(Integer bookingDetailId);

    @Modifying
    @Transactional
    @Query("DELETE FROM BookingFB bfb WHERE bfb.bookingDetail.id = :bookingDetailId")
    void deleteByBookingDetailId(@Param("bookingDetailId") Integer bookingDetailId);
}