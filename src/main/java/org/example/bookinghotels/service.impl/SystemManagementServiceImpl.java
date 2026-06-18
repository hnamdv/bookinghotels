package org.example.bookinghotels.service.impl;

import org.example.bookinghotels.dto.PromotionCheckResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.util.List;
import org.example.bookinghotels.dto.RevenueDTO;
import org.example.bookinghotels.dto.OccupancyDTO;
import org.example.bookinghotels.repository.InvoicesRepository;
import org.example.bookinghotels.repository.RoomRepository;
import org.example.bookinghotels.repository.BookingDetailRepository;
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

        boolean exists = promotionRoomTypeRepository
                .existsByPromotion_IdAndRoomType_Id(promotionId, roomTypeId);

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khuyến mãi này đã áp dụng cho loại phòng này rồi"
            );
        }

        PromotionRoomType mapping = new PromotionRoomType();
        mapping.setPromotion(promotion);
        mapping.setRoomType(roomType);

        promotionRoomTypeRepository.save(mapping);
    }
    @Override
    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }
    @Override
    public List<Integer> getRoomTypeIdsByPromotion(Integer promotionId) {
        return promotionRoomTypeRepository.findByPromotion_Id(promotionId)
                .stream()
                .map(item -> item.getRoomType().getId())
                .collect(Collectors.toList());
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

        return promotionRepository.save(oldPromotion);
    }
    @Override
    @Transactional
    public void updatePromotionRoomTypes(Integer promotionId, List<Integer> roomTypeIds) {
        Promotion promotion = getPromotionById(promotionId);

        promotionRoomTypeRepository.deleteByPromotion_Id(promotionId);

        if (roomTypeIds == null || roomTypeIds.isEmpty()) {
            return;
        }

        for (Integer roomTypeId : roomTypeIds) {
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
    }
    @Override
    @Transactional
    public void deletePromotion(Integer id) {
        Promotion promotion = getPromotionById(id);

        promotionRoomTypeRepository.deleteByPromotion_Id(id);

        promotionRepository.delete(promotion);
    }

    @Override
    public PromotionCheckResponse checkPromotionCode(String code, Integer roomTypeId) {
        if (code == null || code.trim().isEmpty()) {
            return new PromotionCheckResponse(
                    false,
                    "Vui lòng nhập mã giảm giá",
                    null,
                    null,
                    null
            );
        }

        if (roomTypeId == null) {
            return new PromotionCheckResponse(
                    false,
                    "Vui lòng chọn loại phòng",
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

        boolean canApply = promotionRoomTypeRepository
                .existsByPromotion_IdAndRoomType_Id(promotion.getId(), roomTypeId);

        if (!canApply) {
            return new PromotionCheckResponse(
                    false,
                    "Mã giảm giá không áp dụng cho loại phòng này",
                    promotion.getId(),
                    promotion.getPromotionName(),
                    null
            );
        }

        LocalDate today = LocalDate.now();

        if (promotion.getStartDate() != null && today.isBefore(promotion.getStartDate())) {
            return new PromotionCheckResponse(
                    false,
                    "Mã giảm giá chưa đến thời gian sử dụng",
                    promotion.getId(),
                    promotion.getPromotionName(),
                    null
            );
        }

        if (promotion.getEndDate() != null && today.isAfter(promotion.getEndDate())) {
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

        if (promotion.getStartDate() != null
                && promotion.getEndDate().isBefore(promotion.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày kết thúc không được nhỏ hơn ngày bắt đầu"
            );
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
    }
