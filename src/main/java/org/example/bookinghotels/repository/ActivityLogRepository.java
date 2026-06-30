package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Integer> {
    boolean existsByActionAndTableNameAndDescriptionContaining(String action, String tableName, String marker);
    Optional<ActivityLog> findFirstByActionAndTableNameAndDescriptionContainingOrderByCreatedAtDesc(
            String action, String tableName, String marker);
}
