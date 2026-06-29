package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.FwB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FwbRepository extends JpaRepository<FwB, Integer> {
    List<FwB> findByStatus(String status);
}