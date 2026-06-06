package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.ActivityLog;
import org.example.bookinghotels.entity.Promotion;

import java.util.List;

public interface SystemManagementService {
    Promotion savePromotion(Promotion promotion);
    void applyPromotionToRoom(Integer promotionId, Integer roomTypeId);
    void logActivity(ActivityLog log);
    List<ActivityLog> getAllLogs();
}