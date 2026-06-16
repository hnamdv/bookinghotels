package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Integer> {}