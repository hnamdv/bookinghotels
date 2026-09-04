package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Hotels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HotelsRepository extends JpaRepository<Hotels, Integer> {
    Optional<Hotels> findBySlug(String slug);
    @Query("SELECT h FROM Hotels h ORDER BY h.id ASC")
    List<Hotels> findAllHotels();
}