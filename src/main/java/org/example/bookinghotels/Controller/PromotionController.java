package org.example.bookinghotels.Controller;

import org.springframework.security.access.prepost.PreAuthorize;

import org.example.bookinghotels.entity.Promotion;
import org.example.bookinghotels.service.SystemManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.bookinghotels.dto.PromotionCheckResponse;
import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@CrossOrigin(origins = "*")
public class PromotionController {

    private final SystemManagementService systemManagementService;

    public PromotionController(SystemManagementService systemManagementService) {
        this.systemManagementService = systemManagementService;
    }

    @GetMapping
    public ResponseEntity<List<Promotion>> getAllPromotions() {
        return ResponseEntity.ok(systemManagementService.getAllPromotions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Promotion> getPromotionById(@PathVariable Integer id) {
        return ResponseEntity.ok(systemManagementService.getPromotionById(id));
    }


    @GetMapping("/check")
    public ResponseEntity<PromotionCheckResponse> checkPromotionCode(@RequestParam String code) {
        return ResponseEntity.ok(systemManagementService.checkPromotionCode(code));
    }
    
    @PreAuthorize("hasAuthority('ROLE_PROMOTION')")
    @PostMapping
    public ResponseEntity<Promotion> createPromotion(@RequestBody Promotion promotion) {
        return ResponseEntity.ok(systemManagementService.savePromotion(promotion));
    }

    @PreAuthorize("hasAuthority('ROLE_PROMOTION')")
    @PutMapping("/{id}")
    public ResponseEntity<Promotion> updatePromotion(
            @PathVariable Integer id,
            @RequestBody Promotion promotion
    ) {
        return ResponseEntity.ok(systemManagementService.updatePromotion(id, promotion));
    }

    @PreAuthorize("hasAuthority('ROLE_PROMOTION')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePromotion(@PathVariable Integer id) {
        systemManagementService.deletePromotion(id);
        return ResponseEntity.ok("Xóa chương trình khuyến mãi thành công");
    }
}