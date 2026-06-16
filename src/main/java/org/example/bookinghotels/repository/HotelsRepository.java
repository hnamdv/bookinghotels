package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Hotels;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HotelsRepository extends JpaRepository<Hotels, Integer> {
    Optional<Hotels> findBySlug(String slug);
}