package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Media;
import org.example.bookinghotels.repository.MediaRepository;
import org.example.bookinghotels.service.FwBService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping({"/staff/fwb", "/admin/addons", "/admin/services"})
public class FwbManagementController {

    private final FwBService fwBService;
    private final MediaRepository mediaRepository;

    public FwbManagementController(FwBService fwBService, MediaRepository mediaRepository) {
        this.fwBService = fwBService;
        this.mediaRepository = mediaRepository;
    }

    @GetMapping("/management")
    public String management(@RequestParam(required = false) Integer editId, Model model) {
        fillCommon(model, editId);
        model.addAttribute("mode", "services");
        model.addAttribute("services", fwBService.getChargeableServices());
        return "html/admin-html/fwb-management";
    }

    @GetMapping("/amenities")
    public String amenities(@RequestParam(required = false) Integer editId, Model model) {
        fillCommon(model, editId);
        model.addAttribute("mode", "amenities");
        model.addAttribute("services", fwBService.getRoomAmenityOptions());
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
        String resolvedCategory = normalizeCategory(category, mode);
        Double resolvedPrice = price == null ? 0D : price;

        if ("amenities".equals(mode)) {
            resolvedPrice = 0D;
            unit = "mục";
        } else if (resolvedPrice <= 0D) {
            ra.addFlashAttribute("error", "Phụ thu / dịch vụ có giá phải lớn hơn 0. Nếu muốn tạo tiện ích miễn phí, hãy chuyển sang tab Tiện ích phòng miễn phí.");
            return "redirect:" + basePath(mode);
        }

        fwBService.saveService(id, name, resolvedPrice, unit, resolvedImage, resolvedCategory, status);
        ra.addFlashAttribute("success", "Đã lưu " + ("amenities".equals(mode) ? "tiện ích phòng." : "phụ thu / dịch vụ."));
        return "redirect:" + basePath(mode);
    }

    @PostMapping("/{id}/hide")
    public String hide(@PathVariable Integer id,
                       @RequestParam(required = false, defaultValue = "services") String mode,
                       RedirectAttributes ra) {
        fwBService.hide(id);
        ra.addFlashAttribute("success", "Đã ẩn khỏi danh sách.");
        return "redirect:" + basePath(mode);
    }

    private String resolveImage(Integer mediaId, String image) {
        if (mediaId != null) {
            return mediaRepository.findById(mediaId).map(Media::getFileUrl).orElse(image == null ? "" : image.trim());
        }
        return image == null ? "" : image.trim();
    }

    private String normalizeCategory(String category, String mode) {
        if ("amenities".equals(mode)) {
            return "Tiện ích phòng";
        }
        return category == null || category.isBlank() ? "Phụ thu / dịch vụ" : category.trim();
    }

    private String basePath(String mode) {
        return "amenities".equals(mode) ? "/staff/fwb/amenities" : "/staff/fwb/management";
    }
}
