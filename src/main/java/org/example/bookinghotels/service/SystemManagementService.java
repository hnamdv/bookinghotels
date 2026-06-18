package org.example.bookinghotels.service;

import org.example.bookinghotels.dto.OccupancyDTO;
import org.example.bookinghotels.dto.PromotionCheckResponse;
import org.example.bookinghotels.dto.RevenueDTO;
import org.example.bookinghotels.entity.ActivityLog;
import org.example.bookinghotels.entity.Promotion;
import org.example.bookinghotels.entity.RoomType;

import java.util.List;

public interface SystemManagementService {

    Promotion savePromotion(Promotion promotion);

    List<Promotion> getAllPromotions();

    Promotion getPromotionById(Integer id);

    Promotion updatePromotion(Integer id, Promotion promotion);

    PromotionCheckResponse checkPromotionCode(String code);

    void deletePromotion(Integer id);

    void applyPromotionToRoom(Integer promotionId, Integer roomTypeId);

    void logActivity(ActivityLog log);

    List<ActivityLog> getAllLogs();

    // Dashboard
    List<RevenueDTO> getRevenueByDay();

    List<RevenueDTO> getRevenueByMonth();

    List<RevenueDTO> getRevenueByYear();

    OccupancyDTO getOccupancy();

    List<RoomType> getAllRoomTypes();
}