package org.example.bookinghotels.service.impl;

import org.example.bookinghotels.dto.OccupancyDTO;
import org.example.bookinghotels.dto.PromotionCheckResponse;
import org.example.bookinghotels.entity.ActivityLog;
import org.example.bookinghotels.entity.Promotion;
import org.example.bookinghotels.entity.PromotionRoomType;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.*;
import org.example.bookinghotels.service.SystemManagementService;
import org.example.bookinghotels.specification.ActivityLogSpecification; // << MỚI
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;               // << MỚI
import org.springframework.data.domain.Pageable;             // << MỚI
import org.springframework.data.jpa.domain.Specification;    // << MỚI
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.example.bookinghotels.dto.RevenueDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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
        return checkPromotionCode(code, null);
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

    // ==== MỚI: dùng cho trang Activity Logs (filter + sort + phân trang) ====
    @Override
    public Page<ActivityLog> searchLogs(String keyword, String action, String module,
                                        String fromDate, String toDate, Pageable pageable) {
        Specification<ActivityLog> spec = ActivityLogSpecification.filter(keyword, action, module, fromDate, toDate);
        return activityLogRepository.findAll(spec, pageable);
    }
    // ==========================================================================

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
    public List<RevenueDTO> getRevenueByDay(LocalDate fromDate, LocalDate toDate) {
        return invoicesRepository.findAll()
                .stream()
                .filter(invoice -> invoice.getInvoiceDate() != null)
                .filter(invoice -> fromDate == null || !invoice.getInvoiceDate().toLocalDate().isBefore(fromDate))
                .filter(invoice -> toDate == null || !invoice.getInvoiceDate().toLocalDate().isAfter(toDate))
                .collect(java.util.stream.Collectors.groupingBy(
                        invoice -> invoice.getInvoiceDate().toLocalDate().toString(),
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.summingDouble(invoice -> invoice.getTotalAmount() == null ? 0.0 : invoice.getTotalAmount())
                ))
                .entrySet()
                .stream()
                .map(entry -> new RevenueDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public List<RevenueDTO> getRevenueByMonth(Integer month, Integer year) {
        return invoicesRepository.findAll()
                .stream()
                .filter(invoice -> invoice.getInvoiceDate() != null)
                .filter(invoice -> month == null || invoice.getInvoiceDate().getMonthValue() == month)
                .filter(invoice -> year == null || invoice.getInvoiceDate().getYear() == year)
                .collect(java.util.stream.Collectors.groupingBy(
                        invoice -> String.format("%04d-%02d", invoice.getInvoiceDate().getYear(), invoice.getInvoiceDate().getMonthValue()),
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.summingDouble(invoice -> invoice.getTotalAmount() == null ? 0.0 : invoice.getTotalAmount())
                ))
                .entrySet()
                .stream()
                .map(entry -> new RevenueDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public long getInvoiceCount(LocalDate fromDate, LocalDate toDate) {
        return invoicesRepository.findAll()
                .stream()
                .filter(invoice -> invoice.getInvoiceDate() != null)
                .filter(invoice -> fromDate == null || !invoice.getInvoiceDate().toLocalDate().isBefore(fromDate))
                .filter(invoice -> toDate == null || !invoice.getInvoiceDate().toLocalDate().isAfter(toDate))
                .count();
    }

    @Override
    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }

    @Override
    public void updatePromotionRoomTypes(Integer id, List<Integer> roomTypeIds) {
        Promotion promotion = getPromotionById(id);
        List<PromotionRoomType> existing = promotionRoomTypeRepository.findAll()
                .stream()
                .filter(item -> item.getPromotion() != null && item.getPromotion().getId() != null && item.getPromotion().getId().equals(id))
                .toList();
        promotionRoomTypeRepository.deleteAll(existing);
        if (roomTypeIds == null) return;
        for (Integer roomTypeId : roomTypeIds) {
            RoomType roomType = roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loại phòng không tồn tại"));
            PromotionRoomType mapping = new PromotionRoomType();
            mapping.setPromotion(promotion);
            mapping.setRoomType(roomType);
            promotionRoomTypeRepository.save(mapping);
        }
    }

    @Override
    public Object getRoomTypeIdsByPromotion(Integer id) {
        return promotionRoomTypeRepository.findAll()
                .stream()
                .filter(item -> item.getPromotion() != null && item.getPromotion().getId() != null && item.getPromotion().getId().equals(id))
                .filter(item -> item.getRoomType() != null && item.getRoomType().getId() != null)
                .map(item -> item.getRoomType().getId())
                .toList();
    }
}