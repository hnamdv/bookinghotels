package org.example.bookinghotels.service.impl;

import org.example.bookinghotels.dto.OccupancyDTO;
import org.example.bookinghotels.dto.PromotionCheckResponse;
import org.example.bookinghotels.entity.ActivityLog;
import org.example.bookinghotels.entity.Promotion;
import org.example.bookinghotels.entity.PromotionRoomType;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.*;
import org.example.bookinghotels.service.SystemManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.example.bookinghotels.dto.RevenueDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
@Service
public class SystemManagementServiceImpl implements SystemManagementService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private PromotionRoomTypeRepository promotionRoomTypeRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private InvoicesRepository invoicesRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingDetailRepository bookingDetailRepository;
    @Override
    public Promotion savePromotion(Promotion promotion) {
        validatePromotion(promotion);
        return promotionRepository.save(promotion);
    }

    @Override
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    @Override
    public Promotion getPromotionById(Integer id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy chương trình khuyến mãi"
                ));
    }

    @Override
    public Promotion updatePromotion(Integer id, Promotion promotion) {
        validatePromotion(promotion);

        Promotion oldPromotion = getPromotionById(id);

        oldPromotion.setPromotionName(promotion.getPromotionName());
        oldPromotion.setDescription(promotion.getDescription());
        oldPromotion.setDiscountPercent(promotion.getDiscountPercent());
        oldPromotion.setStartDate(promotion.getStartDate());
        oldPromotion.setEndDate(promotion.getEndDate());
        oldPromotion.setStartTime(promotion.getStartTime());
        oldPromotion.setEndTime(promotion.getEndTime());

        return promotionRepository.save(oldPromotion);
    }

    @Override
    public void deletePromotion(Integer id) {
        Promotion promotion = getPromotionById(id);
        promotionRepository.delete(promotion);
    }

    @Override
    public PromotionCheckResponse checkPromotionCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return new PromotionCheckResponse(
                    false,
                    "Vui lòng nhập mã giảm giá",
                    null,
                    null,
                    null
            );
        }

        String cleanCode = code.trim();

        Promotion promotion = promotionRepository
                .findByPromotionNameIgnoreCase(cleanCode)
                .orElse(null);

        if (promotion == null) {
            return new PromotionCheckResponse(
                    false,
                    "Mã giảm giá không tồn tại",
                    null,
                    cleanCode,
                    null
            );
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startsAt = promotion.getStartDate() == null ? null
                : LocalDateTime.of(promotion.getStartDate(), promotion.getStartTime() == null ? LocalTime.MIDNIGHT : promotion.getStartTime());
        LocalDateTime endsAt = promotion.getEndDate() == null ? null
                : LocalDateTime.of(promotion.getEndDate(), promotion.getEndTime() == null ? LocalTime.of(23, 59) : promotion.getEndTime());

        if (startsAt != null && now.isBefore(startsAt)) {
            return new PromotionCheckResponse(
                    false,
                    "Mã giảm giá chưa đến thời gian sử dụng",
                    promotion.getId(),
                    promotion.getPromotionName(),
                    null
            );
        }

        if (endsAt != null && now.isAfter(endsAt)) {
            return new PromotionCheckResponse(
                    false,
                    "Mã giảm giá đã hết hạn",
                    promotion.getId(),
                    promotion.getPromotionName(),
                    null
            );
        }

        if (promotion.getDiscountPercent() == null
                || promotion.getDiscountPercent() <= 0
                || promotion.getDiscountPercent() > 100) {
            return new PromotionCheckResponse(
                    false,
                    "Phần trăm giảm giá không hợp lệ",
                    promotion.getId(),
                    promotion.getPromotionName(),
                    null
            );
        }

        return new PromotionCheckResponse(
                true,
                "Mã giảm giá hợp lệ",
                promotion.getId(),
                promotion.getPromotionName(),
                promotion.getDiscountPercent()
        );
    }

    @Override
    public void applyPromotionToRoom(Integer promotionId, Integer roomTypeId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khuyến mãi không tồn tại"
                ));

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Loại phòng không tồn tại"
                ));

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

    private void validatePromotion(Promotion promotion) {
        if (promotion.getPromotionName() == null || promotion.getPromotionName().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên chương trình khuyến mãi không được để trống"
            );
        }

        if (promotion.getDiscountPercent() == null
                || promotion.getDiscountPercent() < 0
                || promotion.getDiscountPercent() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "% giảm phải từ 0 đến 100"
            );
        }

        if (promotion.getEndDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hạn dùng không được để trống"
            );
        }

        LocalTime startTime = promotion.getStartTime() == null ? LocalTime.MIDNIGHT : promotion.getStartTime();
        LocalTime endTime = promotion.getEndTime() == null ? LocalTime.of(23, 59) : promotion.getEndTime();
        promotion.setStartTime(startTime);
        promotion.setEndTime(endTime);

        if (promotion.getStartDate() != null) {
            LocalDateTime startsAt = LocalDateTime.of(promotion.getStartDate(), startTime);
            LocalDateTime endsAt = LocalDateTime.of(promotion.getEndDate(), endTime);
            if (!endsAt.isAfter(startsAt)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Thời gian kết thúc phải sau thời gian bắt đầu"
                );
            }
        }
    }
    @Override
    public List<RevenueDTO> getRevenueByDay() {

        return invoicesRepository.getRevenueByDay()
                .stream()
                .map(item -> new RevenueDTO(
                        item[0].toString(),
                        ((Number) item[1]).doubleValue()
                ))
                .toList();
    }

    @Override
    public List<RevenueDTO> getRevenueByMonth() {

        return invoicesRepository.getRevenueByMonth()
                .stream()
                .map(item -> new RevenueDTO(
                        item[0].toString(),
                        ((Number) item[1]).doubleValue()
                ))
                .toList();
    }

    @Override
    public List<RevenueDTO> getRevenueByYear() {

        return invoicesRepository.getRevenueByYear()
                .stream()
                .map(item -> new RevenueDTO(
                        item[0].toString(),
                        ((Number) item[1]).doubleValue()
                ))
                .toList();
    }

    @Override
    public OccupancyDTO getOccupancy() {

        long totalRooms = roomRepository.count();

        long bookedRooms = bookingDetailRepository.countDistinctBookedRooms();

        double occupancyRate = 0;

        if (totalRooms > 0) {
            occupancyRate = (bookedRooms * 100.0) / totalRooms;
        }

        return new OccupancyDTO(
                totalRooms,
                bookedRooms,
                Math.round(occupancyRate * 100.0) / 100.0
        );
    }
    @Override
    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }
}