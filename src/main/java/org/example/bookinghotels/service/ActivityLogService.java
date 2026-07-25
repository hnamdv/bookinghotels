package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.ActivityLog;
import org.example.bookinghotels.entity.User;
import org.example.bookinghotels.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository repo;

    public void log(String action,
                    String module,
                    String description,
                    User user) {

        ActivityLog log = new ActivityLog();
        log.setAction(action);
        log.setTableName(module); // BOOKING / ROOM / USER
        log.setDescription(description);
        log.setUser(user);
        log.setCreatedAt(LocalDateTime.now());

        repo.save(log);
    }
}