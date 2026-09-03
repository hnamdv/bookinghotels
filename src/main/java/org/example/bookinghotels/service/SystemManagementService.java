package org.example.bookinghotels.service;

import org.example.bookinghotels.dto.OccupancyDTO;
import org.example.bookinghotels.dto.PromotionCheckResponse;
import org.example.bookinghotels.dto.RevenueDTO;
import org.example.bookinghotels.entity.ActivityLog;
import org.example.bookinghotels.entity.Promotion;
import org.example.bookinghotels.entity.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;

public interface SystemManagementService {
    // Mới thêm: dùng cho trang Activity Logs (filter + sort + phân trang)
    Page<ActivityLog> searchLogs(String keyword, String action, String module,
                                 String fromDate, String toDate, Pageable pageable);

    Promotion savePromotion(Promotion promotion);

    List<Promotion> getAllPromotions();

    Promotion getPromotionById(Integer id);

    Promotion updatePromotion(Integer id, Promotion promotion);

    PromotionCheckResponse checkPromotionCode(String code);

    PromotionCheckResponse checkPromotionCode(String code, Integer roomTypeId);

    void deletePromotion(Integer id);

    void applyPromotionToRoom(Integer promotionId, Integer roomTypeId);

    void logActivity(ActivityLog log);

    List<ActivityLog> getAllLogs();

    // ==== Dashboard ====

    List<RevenueDTO> getRevenueByDay();

    List<RevenueDTO> getRevenueByDay(LocalDate fromDate, LocalDate toDate);

    List<RevenueDTO> getRevenueByMonth();

    List<RevenueDTO> getRevenueByMonth(Integer month, Integer year);

    List<RevenueDTO> getRevenueByYear();

    OccupancyDTO getOccupancy();

    long getInvoiceCount(LocalDate fromDate, LocalDate toDate);

    // ====================

    List<RoomType> getAllRoomTypes();

    void updatePromotionRoomTypes(Integer id, List<Integer> roomTypeIds);

    Object getRoomTypeIdsByPromotion(Integer id);
}