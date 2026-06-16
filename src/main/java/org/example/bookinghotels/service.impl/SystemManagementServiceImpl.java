package org.example.bookinghotels.service.impl;


import org.example.bookinghotels.entity.ActivityLog;
import org.example.bookinghotels.entity.Promotion;
import org.example.bookinghotels.entity.PromotionRoomType;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.ActivityLogRepository;
import org.example.bookinghotels.repository.PromotionRepository;
import org.example.bookinghotels.repository.PromotionRoomTypeRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.example.bookinghotels.service.SystemManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SystemManagementServiceImpl implements SystemManagementService {

    @Autowired private PromotionRepository promotionRepository;
    @Autowired private PromotionRoomTypeRepository promotionRoomTypeRepository;
    @Autowired private ActivityLogRepository activityLogRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;

    @Override
    public Promotion savePromotion(Promotion promotion) {
        return promotionRepository.save(promotion);
    }

    @Override
    public void applyPromotionToRoom(Integer promotionId, Integer roomTypeId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại"));
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RuntimeException("Loại phòng không tồn tại"));

        PromotionRoomType mapping = new PromotionRoomType();
        mapping.setPromotion(promotion);
        mapping.setRoomType(roomType);
        promotionRoomTypeRepository.save(mapping);
    }

    @Override
    public void logActivity(ActivityLog log) {
        activityLogRepository.save(log);
    }

    @Override
    public List<ActivityLog> getAllLogs() {
        return activityLogRepository.findAll();
    }
}