package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.ActivityLog;
import org.example.bookinghotels.entity.User;
import org.example.bookinghotels.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Async // Chạy bất đồng bộ, cắt đứt hoàn toàn vòng lặp StackOverflowError
    public void log(String action,
                    String module,
                    String description,
                    User user) {
        try {
            ActivityLog log = new ActivityLog();
            log.setAction(action);
            log.setTableName(module);
            log.setDescription(description);
            log.setUser(user);
            log.setCreatedAt(LocalDateTime.now());

            activityLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("[ActivityLogService] Lỗi ghi log: " + e.getMessage());
        }
    }
}