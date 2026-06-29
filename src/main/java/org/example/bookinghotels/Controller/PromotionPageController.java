package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Promotion;
import org.example.bookinghotels.service.SystemManagementService;
import org.springframework.security.access.prepost.PreAuthorize;  // BỎ COMMENT
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@PreAuthorize("hasAuthority('ROLE_PROMOTION')")  // BỎ COMMENT
@RequestMapping("/staff/promotions")
public class PromotionPageController {

    private final SystemManagementService systemManagementService;

    public PromotionPageController(SystemManagementService systemManagementService) {
        this.systemManagementService = systemManagementService;
    }

    @GetMapping
    public String promotionPage(Model model) {
        model.addAttribute("promotion", new Promotion());
        model.addAttribute("promotions", systemManagementService.getAllPromotions());
        return "html/staff-html/promotions";
    }

    @PostMapping("/save")
    public String savePromotion(@ModelAttribute Promotion promotion) {
        if (promotion.getId() == null) {
            systemManagementService.savePromotion(promotion);
        } else {
            systemManagementService.updatePromotion(promotion.getId(), promotion);
        }
        return "redirect:/staff/promotions";
    }

    @GetMapping("/edit/{id}")
    public String editPromotion(@PathVariable Integer id, Model model) {
        model.addAttribute("promotion", systemManagementService.getPromotionById(id));
        model.addAttribute("promotions", systemManagementService.getAllPromotions());
        return "html/staff-html/promotions";
    }

    @GetMapping("/delete/{id}")
    public String deletePromotion(@PathVariable Integer id) {
        systemManagementService.deletePromotion(id);
        return "redirect:/staff/promotions";
    }
}