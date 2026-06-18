package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, Integer> {

    // đếm số phòng đang được booking (KHÔNG trùng phòng)
    @Query("""
    SELECT COUNT(DISTINCT b.room.id)
    FROM BookingDetail b
""")
    long countDistinctBookedRooms();
}