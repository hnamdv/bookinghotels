package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.example.bookinghotels.repository.RoomRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.example.bookinghotels.service.RoomOperationService;
import org.example.bookinghotels.service.RoomInventoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/rooms")
public class AdminRoomController {
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final RoomOperationService roomOperationService;
    private final RoomInventoryService roomInventoryService;

    public AdminRoomController(RoomRepository roomRepository,
                               RoomTypeRepository roomTypeRepository,
                               BookingDetailRepository bookingDetailRepository,
                               RoomOperationService roomOperationService,
                               RoomInventoryService roomInventoryService) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.roomOperationService = roomOperationService;
        this.roomInventoryService = roomInventoryService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) Integer editId,
                        @RequestParam(required = false) Integer roomTypeId, Model model) {
        Room form = editId == null ? new Room() : roomRepository.findById(editId).orElse(new Room());
        model.addAttribute("roomForm", form);
        model.addAttribute("roomTypes", roomTypeRepository.findAll());
        var roomViews = roomOperationService.buildRoomViews(roomTypeId);
        model.addAttribute("roomViews", roomViews);
        model.addAttribute("selectedRoomTypeId", roomTypeId);
        model.addAttribute("totalRooms", roomRepository.count());
        model.addAttribute("occupiedCount", roomViews.stream()
                .filter(v -> List.of("OCCUPIED", "CHECKOUT_TODAY", "CHECKOUT_OVERDUE").contains(v.state())).count());
        model.addAttribute("availableCount", roomViews.stream()
                .filter(v -> v.state().startsWith("AVAILABLE")).count());
        return "html/admin-html/rooms";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Room submitted,
                       @RequestParam Integer roomTypeId,
                       RedirectAttributes ra) {
        try {
            var newType = roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("Loại phòng không tồn tại"));

            Room room;
            Integer oldTypeId = null;
            if (submitted.getId() != null) {
                room = roomRepository.findById(submitted.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại"));
                oldTypeId = room.getRoomType() == null ? null : room.getRoomType().getId();
            } else {
                room = new Room();
            }

            if (submitted.getRoomNumber() == null || submitted.getRoomNumber().isBlank()) {
                throw new IllegalArgumentException("Số phòng không được để trống");
            }
            String roomNumber = submitted.getRoomNumber().trim();
            roomRepository.findByRoomNumberIgnoreCase(roomNumber).ifPresent(existing -> {
                if (submitted.getId() == null || !existing.getId().equals(submitted.getId())) {
                    throw new IllegalArgumentException("Số phòng đã tồn tại");
                }
            });

            room.setRoomType(newType);
            room.setRoomNumber(roomNumber);
            room.setThumbnail(submitted.getThumbnail());
            room.setSlug(submitted.getSlug() == null || submitted.getSlug().isBlank()
                    ? "phong-" + roomNumber.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                    : submitted.getSlug().trim());
            roomRepository.saveAndFlush(room);

            roomInventoryService.syncActualRoomCount(newType);
            if (oldTypeId != null && !oldTypeId.equals(newType.getId())) {
                roomTypeRepository.findById(oldTypeId).ifPresent(roomInventoryService::syncActualRoomCount);
            }

            ra.addFlashAttribute("success", "Đã lưu phòng " + room.getRoomNumber()
                    + ". Tổng phòng của loại '" + newType.getNameType() + "' đã tự đồng bộ.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rooms";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            List<BookingDetail> history = bookingDetailRepository.findByRoomIdWithBooking(id);
            if (!history.isEmpty()) {
                throw new IllegalStateException("Phòng đã có lịch sử đặt, không thể xóa. Hãy giữ phòng để bảo toàn dữ liệu.");
            }
            Room room = roomRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại"));
            var type = room.getRoomType();
            roomRepository.delete(room);
            roomRepository.flush();
            roomInventoryService.syncActualRoomCount(type);
            ra.addFlashAttribute("success", "Đã xóa phòng và cập nhật tổng số phòng của loại phòng");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rooms";
    }

    @PostMapping("/booking/{detailId}/check-in")
    public String checkIn(@PathVariable Integer detailId,
                          @RequestParam(required = false) Integer roomTypeId,
                          RedirectAttributes ra) {
        return runOperation(() -> roomOperationService.checkIn(detailId), "Đã check-in", ra, roomTypeId);
    }

    @PostMapping("/booking/{detailId}/check-out")
    public String checkOut(@PathVariable Integer detailId,
                           @RequestParam(required = false) Integer roomTypeId,
                           RedirectAttributes ra) {
        return runOperation(() -> roomOperationService.checkOut(detailId, false), "Đã checkout", ra, roomTypeId);
    }

    @PostMapping("/booking/{detailId}/no-show")
    public String noShow(@PathVariable Integer detailId,
                         @RequestParam(required = false) Integer roomTypeId,
                         RedirectAttributes ra) {
        return runOperation(() -> roomOperationService.markNoShow(detailId), "Đã đánh dấu khách không đến", ra, roomTypeId);
    }

    @PostMapping("/booking/{detailId}/status")
    public String updateStatus(@PathVariable Integer detailId,
                               @RequestParam String status,
                               @RequestParam(required = false) Integer roomTypeId,
                               RedirectAttributes ra) {
        try {
            roomOperationService.updateStatus(detailId, status);
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái booking thành " + status);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return roomTypeId == null ? "redirect:/admin/rooms" : "redirect:/admin/rooms?roomTypeId=" + roomTypeId;
    }

    private String runOperation(Runnable operation, String success, RedirectAttributes ra, Integer roomTypeId) {
        try {
            operation.run();
            ra.addFlashAttribute("success", success);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return roomTypeId == null ? "redirect:/admin/rooms" : "redirect:/admin/rooms?roomTypeId=" + roomTypeId;
    }
}
