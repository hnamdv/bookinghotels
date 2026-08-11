package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.FwB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FwbRepository extends JpaRepository<FwB, Integer> {
}