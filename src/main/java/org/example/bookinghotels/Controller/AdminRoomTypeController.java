package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.*;
import org.example.bookinghotels.repository.*;
import org.example.bookinghotels.service.DatabaseSequenceService;
import org.example.bookinghotels.service.RoomTypeImageService;
import org.example.bookinghotels.service.RoomInventoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/admin/room-types")
@PreAuthorize("hasAuthority('ROLE_ROOM')")
public class AdminRoomTypeController {
    private final RoomTypeRepository roomTypeRepository;
    private final HotelsRepository hotelsRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionRoomTypeRepository promotionRoomTypeRepository;
    private final MediaRepository mediaRepository;
    private final RoomImgRepository roomImgRepository;
    private final DatabaseSequenceService sequenceService;
    private final RoomTypeImageService roomTypeImageService;
    private final RoomInventoryService roomInventoryService;
    private final FwbRepository fwbRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminRoomTypeController(RoomTypeRepository roomTypeRepository,
                                   HotelsRepository hotelsRepository,
                                   PromotionRepository promotionRepository,
                                   PromotionRoomTypeRepository promotionRoomTypeRepository,
                                   MediaRepository mediaRepository,
                                   RoomImgRepository roomImgRepository,
                                   DatabaseSequenceService sequenceService,
                                   RoomTypeImageService roomTypeImageService,
                                   RoomInventoryService roomInventoryService,
                                   FwbRepository fwbRepository) {
        this.roomTypeRepository = roomTypeRepository;
        this.hotelsRepository = hotelsRepository;
        this.promotionRepository = promotionRepository;
        this.promotionRoomTypeRepository = promotionRoomTypeRepository;
        this.mediaRepository = mediaRepository;
        this.roomImgRepository = roomImgRepository;
        this.sequenceService = sequenceService;
        this.roomTypeImageService = roomTypeImageService;
        this.roomInventoryService = roomInventoryService;
        this.fwbRepository = fwbRepository;
    }

    @GetMapping
    public String page(@RequestParam(required = false) Integer editId, Model model) {
        model.addAttribute("hotels", hotelsRepository.findAll());
        model.addAttribute("promotions", promotionRepository.findAll());
        model.addAttribute("mediaList", mediaRepository.findAll());
        model.addAttribute("roomAmenityOptions", getFreeAmenityOptions());
        model.addAttribute("roomTypes", roomTypeRepository.findAllWithImages());
        var promotionMappings = promotionRoomTypeRepository.findAll();
        model.addAttribute("promotionMappings", promotionMappings);
        Map<Integer, PromotionRoomType> promotionByRoomType = new HashMap<>();
        for (PromotionRoomType mapping : promotionMappings) {
            if (mapping.getRoomType() != null) promotionByRoomType.put(mapping.getRoomType().getId(), mapping);
        }
        model.addAttribute("promotionByRoomType", promotionByRoomType);
        if (editId != null) {
            roomTypeRepository.findDetailById(editId).ifPresent(rt -> {
                model.addAttribute("editRoomType", rt);
                model.addAttribute("selectedImageUrls", rt.getImages() == null ? List.of() : rt.getImages().stream().map(RoomImg::getImage).toList());
                model.addAttribute("selectedPromotionIds", promotionRoomTypeRepository.findByRoomTypeId(rt.getId()).stream().map(m -> m.getPromotion().getId()).toList());
                model.addAttribute("selectedAmenityIds", extractAmenityIds(rt.getBedOptions()));
            });
        }
        return "html/admin-html/room-types";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Integer id,
                       @RequestParam String nameType,
                       @RequestParam Double price,
                       @RequestParam Integer capacity,
                       @RequestParam(required = false) Integer hotelId,
                       @RequestParam(required = false) String bed,
                       @RequestParam(required = false) String bedOptions,
                       @RequestParam(required = false) List<Integer> roomAmenityIds,
                       @RequestParam(required = false) String description,
                       @RequestParam(required = false) Double area,
                       @RequestParam(defaultValue = "1") Integer totalRooms,
                       @RequestParam(defaultValue = "0") Double taxAndFee,
                       @RequestParam(defaultValue = "false") Boolean hasWifi,
                       @RequestParam(defaultValue = "false") Boolean hasBathtub,
                       @RequestParam(defaultValue = "false") Boolean hasTv,
                       @RequestParam(defaultValue = "false") Boolean hasBalcony,
                       @RequestParam(required = false) List<Integer> mediaIds,
                       @RequestParam(required = false) Integer promotionId,
                       RedirectAttributes ra) {
        try {
            if (nameType == null || nameType.isBlank()) throw new IllegalArgumentException("Tên loại phòng không được để trống.");
            if (price == null || price < 0) throw new IllegalArgumentException("Giá phòng không hợp lệ.");
            if (capacity == null || capacity < 1) throw new IllegalArgumentException("Sức chứa phải lớn hơn 0.");

            RoomType rt = id == null ? new RoomType() : roomTypeRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại phòng."));
            rt.setNameType(nameType.trim());
            rt.setPrice(price);
            rt.setCapacity(capacity);
            rt.setBed(blankToNull(bed));
            rt.setBedOptions(buildBedOptionsPayload(bedOptions, roomAmenityIds));
            rt.setDescription(blankToNull(description));
            rt.setArea(area);
            rt.setTotalRooms(totalRooms == null || totalRooms < 1 ? 1 : totalRooms);
            rt.setTaxAndFee(taxAndFee == null ? 0D : taxAndFee);
            rt.setHasWifi(Boolean.TRUE.equals(hasWifi));
            rt.setHasBathtub(Boolean.TRUE.equals(hasBathtub));
            rt.setHasTv(Boolean.TRUE.equals(hasTv));
            rt.setHasBalcony(Boolean.TRUE.equals(hasBalcony));
            if (hotelId != null) rt.setHotels(hotelsRepository.findById(hotelId)
                    .orElseThrow(() -> new IllegalArgumentException("Khách sạn không tồn tại.")));
            if (id == null) sequenceService.synchronize("room_type");
            rt = roomTypeRepository.saveAndFlush(rt);

            // Đồng bộ tuyệt đối ảnh theo checkbox: bỏ tick là gỡ ngay, không giữ bản ghi cũ và không nhân bản.
            roomTypeImageService.replaceImages(rt, mediaIds);

            int actualRoomCount = roomInventoryService.ensurePhysicalRooms(rt);
            rt.setTotalRooms(actualRoomCount);

            promotionRoomTypeRepository.deleteByRoomTypeId(rt.getId());
            if (promotionId != null) {
                Promotion promotion = promotionRepository.findById(promotionId)
                        .orElseThrow(() -> new IllegalArgumentException("Ưu đãi không tồn tại."));
                sequenceService.synchronize("promotion_room_type");
                PromotionRoomType mapping = new PromotionRoomType();
                mapping.setRoomType(rt);
                mapping.setPromotion(promotion);
                promotionRoomTypeRepository.saveAndFlush(mapping);
            }

            ra.addFlashAttribute("success", id == null ? "Đã tạo loại phòng." : "Đã cập nhật loại phòng.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", rootMessage(ex));
            if (id != null) return "redirect:/admin/room-types?editId=" + id;
        }
        return "redirect:/admin/room-types";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Không xóa loại phòng để bảo toàn dữ liệu đặt phòng. Hãy sửa thông tin hoặc ngừng sử dụng loại phòng này.");
        return "redirect:/admin/room-types";
    }


    private List<FwB> getFreeAmenityOptions() {
        return fwbRepository.findAll().stream()
                .filter(item -> item.getStatus() == null
                        || item.getStatus().isBlank()
                        || "ACTIVE".equalsIgnoreCase(item.getStatus())
                        || "SHOW".equalsIgnoreCase(item.getStatus()))
                .filter(item -> item.getPrice() <= 0D)
                .filter(item -> {
                    String category = item.getCategory() == null ? "" : item.getCategory().toLowerCase(Locale.ROOT);
                    return category.contains("tiện ích") || category.contains("tien ich") || category.contains("amenity");
                })
                .sorted(Comparator.comparing(FwB::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private String buildBedOptionsPayload(String bedOptionsText, List<Integer> amenityIds) {
        List<Map<String, Object>> amenities = new ArrayList<>();
        if (amenityIds != null) {
            for (Integer amenityId : new LinkedHashSet<>(amenityIds)) {
                if (amenityId == null) continue;
                fwbRepository.findById(amenityId).ifPresent(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", item.getId());
                    map.put("name", item.getName());
                    map.put("icon", item.getImage());
                    map.put("category", item.getCategory());
                    amenities.add(map);
                });
            }
        }

        String text = bedOptionsText == null ? "" : bedOptionsText.trim();
        if (text.isBlank() && amenities.isEmpty()) {
            return null;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        if (!text.isBlank()) {
            payload.put("text", text);
        }
        if (!amenities.isEmpty()) {
            payload.put("amenities", amenities);
            payload.put("amenityIds", amenities.stream().map(item -> item.get("id")).toList());
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return text.isBlank() ? null : text;
        }
    }

    private List<Integer> extractAmenityIds(String raw) {
        if (raw == null || raw.trim().isBlank()) {
            return List.of();
        }
        String value = raw.trim();
        if (!value.startsWith("{")) {
            return List.of();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
            Object ids = payload.get("amenityIds");
            if (ids instanceof Collection<?> collection) {
                List<Integer> result = new ArrayList<>();
                for (Object item : collection) {
                    if (item instanceof Number number) {
                        result.add(number.intValue());
                    } else if (item != null) {
                        try {
                            result.add(Integer.parseInt(item.toString()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                return result;
            }
            Object amenities = payload.get("amenities");
            if (amenities instanceof Collection<?> collection) {
                List<Integer> result = new ArrayList<>();
                for (Object item : collection) {
                    if (item instanceof Map<?, ?> map && map.get("id") != null) {
                        try {
                            result.add(Integer.parseInt(map.get("id").toString()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                return result;
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null) root = root.getCause();
        return root.getMessage() == null ? "Có lỗi xảy ra." : root.getMessage();
    }
}
