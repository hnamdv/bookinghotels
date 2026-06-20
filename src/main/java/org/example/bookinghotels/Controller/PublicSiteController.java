package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.repository.HotelsRepository;
import org.example.bookinghotels.service.SiteBrandingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/site")
public class PublicSiteController {
    private final HotelsRepository hotelsRepository;
    private final SiteBrandingService brandingService;

    public PublicSiteController(HotelsRepository hotelsRepository, SiteBrandingService brandingService) {
        this.hotelsRepository = hotelsRepository;
        this.brandingService = brandingService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> site() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("siteName", brandingService.get("site.name", "FEELHOME"));
        String siteLogo = brandingService.get("site.logo", brandingService.get("site.circleLogo", brandingService.get("site.headerLogo", "")));
        response.put("headerLogo", siteLogo);
        response.put("circleLogo", siteLogo);
        response.put("slides", brandingService.getList("site.slides"));
        response.put("welcomeText", brandingService.get("site.welcomeText", "Chào mừng đến FeelHome"));
        response.put("welcomeColor", brandingService.get("site.welcomeColor", "#d7b34f"));
        response.put("welcomeEffect", brandingService.get("site.welcomeEffect", "shine"));
        response.put("banner", brandingService.get("site.banner", ""));
        response.put("hotels", hotelsRepository.findAll().stream().map(this::hotelMap).toList());
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> hotelMap(Hotels hotel) {
        String prefix = "hotel." + hotel.getId() + ".";
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", hotel.getId());
        map.put("name", hotel.getName());
        map.put("address", hotel.getAddress());
        map.put("phone", hotel.getPhone());
        map.put("email", hotel.getEmail());
        map.put("description", hotel.getDescription());
        String hotelLogo = brandingService.get(prefix + "logo", brandingService.get(prefix + "circleLogo", brandingService.get(prefix + "headerLogo", hotel.getLogo() == null ? "" : hotel.getLogo())));
        map.put("headerLogo", hotelLogo);
        map.put("circleLogo", hotelLogo);
        map.put("slides", brandingService.getList(prefix + "slides"));
        map.put("welcomeText", brandingService.get(prefix + "welcomeText", "Chào mừng đến " + hotel.getName()));
        map.put("welcomeColor", brandingService.get(prefix + "welcomeColor", brandingService.get("site.welcomeColor", "#d7b34f")));
        map.put("welcomeEffect", brandingService.get(prefix + "welcomeEffect", brandingService.get("site.welcomeEffect", "shine")));
        map.put("banner", hotel.getThumbnail());
        map.put("slug", hotel.getSlug());
        return map;
    }
}
