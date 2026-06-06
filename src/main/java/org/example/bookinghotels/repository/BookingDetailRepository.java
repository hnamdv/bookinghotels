package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, Integer> {}