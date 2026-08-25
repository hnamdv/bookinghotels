package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Media;
import org.example.bookinghotels.repository.MediaRepository;
import org.example.bookinghotels.service.FwBService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping({"/staff/fwb", "/admin/addons", "/admin/services"})
@PreAuthorize("hasAuthority('ROLE_FWB')")
public class FwbManagementController {

    private final FwBService fwBService;
    private final MediaRepository mediaRepository;

    public FwbManagementController(FwBService fwBService, MediaRepository mediaRepository) {
        this.fwBService = fwBService;
        this.mediaRepository = mediaRepository;
    }

    @GetMapping({"/management", "/amenities"})
    public String management(@RequestParam(required = false) Integer editId, Model model) {
        fillCommon(model, editId);
        model.addAttribute("amenities", fwBService.getRoomAmenityOptions());
        model.addAttribute("chargeableServices", fwBService.getChargeableServices());
        model.addAttribute("allItems", fwBService.getAll());
        return "html/admin-html/fwb-management";
    }

    private void fillCommon(Model model, Integer editId) {
        model.addAttribute("mediaList", mediaRepository.findAll());
        if (editId != null) {
            fwBService.findById(editId).ifPresent(item -> model.addAttribute("editService", fwBService.toMap(item)));
        }
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Integer id,
                       @RequestParam String name,
                       @RequestParam(required = false) Double price,
                       @RequestParam(required = false) String unit,
                       @RequestParam(required = false) String image,
                       @RequestParam(required = false) Integer mediaId,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false, defaultValue = "services") String mode,
                       RedirectAttributes ra) {
        String resolvedImage = resolveImage(mediaId, image);
        Double resolvedPrice = price == null ? 0D : Math.max(0D, price);
        String resolvedCategory = normalizeCategory(category, resolvedPrice);
        String resolvedUnit = unit == null || unit.isBlank() ? (resolvedPrice <= 0D ? "mục" : "lượt") : unit.trim();

        fwBService.saveService(id, name, resolvedPrice, resolvedUnit, resolvedImage, resolvedCategory, status);
        ra.addFlashAttribute("success", resolvedPrice <= 0D
                ? "Đã lưu tiện ích miễn phí để tick vào loại phòng."
                : "Đã lưu dịch vụ/phụ thu có giá để khách chọn ở thanh toán/POS.");
        return "redirect:/staff/fwb/management";
    }

    @PostMapping("/{id}/hide")
    public String hide(@PathVariable Integer id,
                       @RequestParam(required = false, defaultValue = "services") String mode,
                       RedirectAttributes ra) {
        fwBService.hide(id);
        ra.addFlashAttribute("success", "Đã ẩn khỏi danh sách.");
        return "redirect:/staff/fwb/management";
    }

    private String resolveImage(Integer mediaId, String image) {
        if (mediaId != null) {
            return mediaRepository.findById(mediaId).map(Media::getFileUrl).orElse(image == null ? "" : image.trim());
        }
        return image == null ? "" : image.trim();
    }

    private String normalizeCategory(String category, Double price) {
        if (category != null && !category.isBlank()) return category.trim();
        return price == null || price <= 0D ? "Tiện ích phòng" : "Phụ thu / dịch vụ";
    }
}
