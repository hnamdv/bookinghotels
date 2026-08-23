package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityLogRepository
        extends JpaRepository<ActivityLog, Integer>, JpaSpecificationExecutor<ActivityLog> {

    boolean existsByActionAndTableNameAndDescriptionContaining(String action, String tableName, String marker);

    Optional<ActivityLog> findFirstByActionAndTableNameAndDescriptionContainingOrderByCreatedAtDesc(
            String action, String tableName, String marker);

    // Mới thêm: lấy toàn bộ log, mới nhất lên đầu, cho trang /admin/logs
    List<ActivityLog> findAllByOrderByCreatedAtDesc();
}