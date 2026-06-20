package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.entity.Media;
import org.example.bookinghotels.entity.RoomImg;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.HotelsRepository;
import org.example.bookinghotels.repository.MediaRepository;
import org.example.bookinghotels.repository.RoomImgRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.example.bookinghotels.service.DatabaseSequenceService;
import org.example.bookinghotels.service.MediaService;
import org.example.bookinghotels.service.SiteBrandingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/admin/media")
public class AdminMediaController {
    private final MediaService mediaService;
    private final MediaRepository mediaRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomImgRepository roomImgRepository;
    private final DatabaseSequenceService sequenceService;
    private final HotelsRepository hotelsRepository;
    private final SiteBrandingService brandingService;

    public AdminMediaController(MediaService mediaService,
                                MediaRepository mediaRepository,
                                RoomTypeRepository roomTypeRepository,
                                RoomImgRepository roomImgRepository,
                                DatabaseSequenceService sequenceService,
                                HotelsRepository hotelsRepository,
                                SiteBrandingService brandingService) {
        this.mediaService = mediaService;
        this.mediaRepository = mediaRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomImgRepository = roomImgRepository;
        this.sequenceService = sequenceService;
        this.hotelsRepository = hotelsRepository;
        this.brandingService = brandingService;
    }

    @GetMapping
    public String page(Model model) {
        List<Media> mediaList = mediaRepository.findAll();
        List<Hotels> hotels = hotelsRepository.findAll();
        model.addAttribute("mediaList", mediaList);
        model.addAttribute("roomTypes", roomTypeRepository.findAllWithImages());
        model.addAttribute("hotels", hotels);
        model.addAttribute("siteName", brandingService.get("site.name", "FEELHOME"));
        model.addAttribute("siteLogo", brandingService.get("site.logo", brandingService.get("site.circleLogo", brandingService.get("site.headerLogo", ""))));
        model.addAttribute("siteSlides", brandingService.getList("site.slides"));

        Map<Integer, String> hotelLogos = new HashMap<>();
        Map<Integer, List<String>> hotelSlides = new HashMap<>();
        for (Hotels hotel : hotels) {
            String prefix = "hotel." + hotel.getId() + ".";
            hotelLogos.put(hotel.getId(), brandingService.get(prefix + "logo",
                    brandingService.get(prefix + "circleLogo",
                            brandingService.get(prefix + "headerLogo", hotel.getLogo() == null ? "" : hotel.getLogo()))));
            hotelSlides.put(hotel.getId(), brandingService.getList(prefix + "slides"));
        }
        model.addAttribute("hotelLogos", hotelLogos);
        model.addAttribute("hotelSlides", hotelSlides);
        return "html/admin-html/media";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam(required = false) Integer roomTypeId,
                         RedirectAttributes ra) {
        try {
            Media media = mediaService.uploadToLocal(file);
            if (roomTypeId != null) attachImage(roomTypeId, media.getFileUrl());
            ra.addFlashAttribute("success", "Đã tải ảnh lên thư viện. Ảnh này có thể dùng lại cho logo, slideshow và loại phòng.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", rootMessage(ex));
        }
        return "redirect:/admin/media";
    }

    @PostMapping("/{mediaId}/attach")
    public String attach(@PathVariable Integer mediaId,
                         @RequestParam Integer roomTypeId,
                         RedirectAttributes ra) {
        try {
            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh."));
            attachImage(roomTypeId, media.getFileUrl());
            ra.addFlashAttribute("success", "Đã gắn ảnh vào loại phòng.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", rootMessage(ex));
        }
        return "redirect:/admin/media";
    }

    @PostMapping("/branding")
    public String updateBranding(@RequestParam(required = false) String siteName,
                                 @RequestParam(required = false) Integer siteLogoMediaId,
                                 @RequestParam(required = false) List<Integer> siteSlideMediaIds,
                                 @RequestParam(defaultValue = "false") boolean clearSiteLogo,
                                 @RequestParam(defaultValue = "false") boolean clearSiteSlides,
                                 @RequestParam(required = false) Integer hotelId,
                                 @RequestParam(required = false) Integer hotelLogoMediaId,
                                 @RequestParam(required = false) List<Integer> hotelSlideMediaIds,
                                 @RequestParam(defaultValue = "false") boolean clearHotelLogo,
                                 @RequestParam(defaultValue = "false") boolean clearHotelSlides,
                                 RedirectAttributes ra) {
        try {
            if (siteName != null && !siteName.isBlank()) brandingService.set("site.name", siteName.trim());
            if (clearSiteLogo) setSiteLogo("");
            else if (siteLogoMediaId != null) setSiteLogo(mediaUrl(siteLogoMediaId));

            if (clearSiteSlides) brandingService.setList("site.slides", List.of());
            else if (siteSlideMediaIds != null) brandingService.setList("site.slides", mediaUrls(siteSlideMediaIds));

            if (hotelId != null) {
                Hotels hotel = hotelsRepository.findById(hotelId)
                        .orElseThrow(() -> new IllegalArgumentException("Khách sạn/chi nhánh không tồn tại."));
                String prefix = "hotel." + hotelId + ".";
                if (clearHotelLogo) {
                    setHotelLogo(prefix, hotel, "");
                } else if (hotelLogoMediaId != null) {
                    setHotelLogo(prefix, hotel, mediaUrl(hotelLogoMediaId));
                }

                if (clearHotelSlides) {
                    brandingService.setList(prefix + "slides", List.of());
                    hotel.setThumbnail(null);
                } else if (hotelSlideMediaIds != null) {
                    List<String> slides = mediaUrls(hotelSlideMediaIds);
                    brandingService.setList(prefix + "slides", slides);
                    hotel.setThumbnail(slides.isEmpty() ? null : slides.get(0));
                }
                hotelsRepository.save(hotel);
            }
            ra.addFlashAttribute("success", "Đã cập nhật logo và slideshow từ thư viện ảnh.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", rootMessage(ex));
        }
        return "redirect:/admin/media";
    }

    private void setSiteLogo(String url) {
        brandingService.set("site.logo", url);
        brandingService.set("site.headerLogo", url);
        brandingService.set("site.circleLogo", url);
    }

    private void setHotelLogo(String prefix, Hotels hotel, String url) {
        brandingService.set(prefix + "logo", url);
        brandingService.set(prefix + "headerLogo", url);
        brandingService.set(prefix + "circleLogo", url);
        hotel.setLogo(url == null || url.isBlank() ? null : url);
    }

    private String mediaUrl(Integer mediaId) {
        return mediaRepository.findById(mediaId)
                .map(Media::getFileUrl)
                .orElseThrow(() -> new IllegalArgumentException("Ảnh đã chọn không còn trong thư viện."));
    }

    private List<String> mediaUrls(List<Integer> ids) {
        if (ids == null) return List.of();
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        for (Integer id : ids) if (id != null) urls.add(mediaUrl(id));
        return new ArrayList<>(urls);
    }

    @PostMapping("/{mediaId}/delete")
    public String delete(@PathVariable Integer mediaId, RedirectAttributes ra) {
        try {
            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh."));
            roomImgRepository.deleteByImage(media.getFileUrl());
            mediaRepository.delete(media);
            mediaService.deletePhysicalFile(media);
            ra.addFlashAttribute("success", "Đã xóa ảnh khỏi thư viện và loại phòng liên quan.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", rootMessage(ex));
        }
        return "redirect:/admin/media";
    }

    private void attachImage(Integer roomTypeId, String imageUrl) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Loại phòng không tồn tại."));
        if (roomImgRepository.existsByRoomTypeIdAndImage(roomTypeId, imageUrl)) return;
        sequenceService.synchronize("room_img");
        RoomImg roomImg = new RoomImg();
        roomImg.setRoomType(roomType);
        roomImg.setImage(imageUrl);
        roomImgRepository.saveAndFlush(roomImg);
    }

    private String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null) root = root.getCause();
        return root.getMessage() == null ? "Có lỗi xảy ra." : root.getMessage();
    }
}
