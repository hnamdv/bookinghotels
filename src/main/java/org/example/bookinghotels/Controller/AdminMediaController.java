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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/admin/media")
@PreAuthorize("hasAuthority('ROLE_IMG')")
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
        Map<Integer, String> mediaSizes = new HashMap<>();
        for (Media media : mediaList) {
            mediaSizes.put(media.getId(), readableFileSize(media));
        }
        model.addAttribute("mediaSizes", mediaSizes);
        model.addAttribute("roomTypes", roomTypeRepository.findAllWithImages());
        model.addAttribute("hotels", hotels);
        model.addAttribute("siteName", brandingService.get("site.name", "FEELHOME"));
        model.addAttribute("siteLogo", brandingService.get("site.logo", brandingService.get("site.circleLogo", brandingService.get("site.headerLogo", ""))));
        model.addAttribute("siteSlides", brandingService.getList("site.slides"));
        model.addAttribute("siteAlbum", brandingService.getList("site.album"));
        model.addAttribute("siteWelcomeText", brandingService.get("site.welcomeText", "Chào mừng đến FeelHome"));
        model.addAttribute("siteWelcomeColor", brandingService.get("site.welcomeColor", "#d7b34f"));
        model.addAttribute("siteWelcomeEffect", brandingService.get("site.welcomeEffect", "shine"));
        addContentAttributes(model, "site", "site.");

        Map<Integer, String> hotelLogos = new HashMap<>();
        Map<Integer, List<String>> hotelSlides = new HashMap<>();
        Map<Integer, List<String>> hotelAlbums = new HashMap<>();
        Map<Integer, String> hotelWelcomeTexts = new HashMap<>();
        Map<Integer, String> hotelWelcomeColors = new HashMap<>();
        Map<Integer, String> hotelWelcomeEffects = new HashMap<>();
        Map<Integer, Map<String, String>> hotelContent = new HashMap<>();
        for (Hotels hotel : hotels) {
            String prefix = "hotel." + hotel.getId() + ".";
            hotelLogos.put(hotel.getId(), brandingService.get(prefix + "logo",
                    brandingService.get(prefix + "circleLogo",
                            brandingService.get(prefix + "headerLogo", hotel.getLogo() == null ? "" : hotel.getLogo()))));
            hotelSlides.put(hotel.getId(), brandingService.getList(prefix + "slides"));
            hotelAlbums.put(hotel.getId(), brandingService.getList(prefix + "album"));
            hotelWelcomeTexts.put(hotel.getId(), brandingService.get(prefix + "welcomeText", "Chào mừng đến " + hotel.getName()));
            hotelWelcomeColors.put(hotel.getId(), brandingService.get(prefix + "welcomeColor", brandingService.get("site.welcomeColor", "#d7b34f")));
            hotelWelcomeEffects.put(hotel.getId(), brandingService.get(prefix + "welcomeEffect", brandingService.get("site.welcomeEffect", "shine")));
            hotelContent.put(hotel.getId(), contentMap(prefix));
        }
        model.addAttribute("hotelLogos", hotelLogos);
        model.addAttribute("hotelSlides", hotelSlides);
        model.addAttribute("hotelAlbums", hotelAlbums);
        model.addAttribute("hotelWelcomeTexts", hotelWelcomeTexts);
        model.addAttribute("hotelWelcomeColors", hotelWelcomeColors);
        model.addAttribute("hotelWelcomeEffects", hotelWelcomeEffects);
        model.addAttribute("hotelContent", hotelContent);
        return "html/admin-html/media";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile[] files,
                         @RequestParam(required = false) Integer roomTypeId,
                         RedirectAttributes ra) {
        try {
            if (files == null || files.length == 0) {
                throw new IllegalArgumentException("Vui lòng chọn ít nhất một ảnh.");
            }
            int uploaded = 0;
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                Media media = mediaService.uploadToLocal(file);
                if (roomTypeId != null) attachImage(roomTypeId, media.getFileUrl());
                uploaded++;
            }
            if (uploaded == 0) throw new IllegalArgumentException("File ảnh rỗng hoặc không hợp lệ.");
            ra.addFlashAttribute("success", "Đã tải " + uploaded + " ảnh lên thư viện. Bây giờ có thể tick ảnh cho logo, slideshow, album, loại phòng hoặc dịch vụ.");
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
                                 @RequestParam(required = false) String siteWelcomeText,
                                 @RequestParam(required = false) String siteWelcomeColor,
                                 @RequestParam(required = false) String siteWelcomeEffect,
                                 @RequestParam(required = false) String siteHeroBadge,
                                 @RequestParam(required = false) String siteHeroAccent,
                                 @RequestParam(required = false) String siteHeroTitle,
                                 @RequestParam(required = false) String siteAboutTitle,
                                 @RequestParam(required = false) String siteAboutText,
                                 @RequestParam(required = false) String siteGalleryTitle,
                                 @RequestParam(required = false) String siteGalleryText,
                                 @RequestParam(required = false) String siteCtaTitle,
                                 @RequestParam(required = false) String siteCtaText,
                                 @RequestParam(required = false) List<Integer> siteSlideMediaIds,
                                 @RequestParam(required = false) List<Integer> siteAlbumMediaIds,
                                 @RequestParam(defaultValue = "false") boolean clearSiteLogo,
                                 @RequestParam(defaultValue = "false") boolean clearSiteSlides,
                                 @RequestParam(defaultValue = "false") boolean clearSiteAlbum,
                                 @RequestParam(required = false) Integer hotelId,
                                 @RequestParam(required = false) Integer hotelLogoMediaId,
                                 @RequestParam(required = false) String hotelWelcomeText,
                                 @RequestParam(required = false) String hotelWelcomeColor,
                                 @RequestParam(required = false) String hotelWelcomeEffect,
                                 @RequestParam(required = false) String hotelHeroBadge,
                                 @RequestParam(required = false) String hotelHeroAccent,
                                 @RequestParam(required = false) String hotelHeroTitle,
                                 @RequestParam(required = false) String hotelAboutTitle,
                                 @RequestParam(required = false) String hotelAboutText,
                                 @RequestParam(required = false) String hotelGalleryTitle,
                                 @RequestParam(required = false) String hotelGalleryText,
                                 @RequestParam(required = false) String hotelCtaTitle,
                                 @RequestParam(required = false) String hotelCtaText,
                                 @RequestParam(required = false) List<Integer> hotelSlideMediaIds,
                                 @RequestParam(required = false) List<Integer> hotelAlbumMediaIds,
                                 @RequestParam(defaultValue = "false") boolean clearHotelLogo,
                                 @RequestParam(defaultValue = "false") boolean clearHotelSlides,
                                 @RequestParam(defaultValue = "false") boolean clearHotelAlbum,
                                 RedirectAttributes ra) {
        try {
            if (siteName != null && !siteName.isBlank()) brandingService.set("site.name", siteName.trim());
            if (siteWelcomeText != null && !siteWelcomeText.isBlank()) brandingService.set("site.welcomeText", siteWelcomeText.trim());
            if (siteWelcomeColor != null && siteWelcomeColor.matches("^#[0-9a-fA-F]{6}$")) brandingService.set("site.welcomeColor", siteWelcomeColor);
            if (siteWelcomeEffect != null && List.of("shine", "glow", "none").contains(siteWelcomeEffect)) brandingService.set("site.welcomeEffect", siteWelcomeEffect);
            setContentValues("site.", siteHeroBadge, siteHeroAccent, siteHeroTitle, siteAboutTitle, siteAboutText, siteGalleryTitle, siteGalleryText, siteCtaTitle, siteCtaText);
            if (clearSiteLogo) setSiteLogo("");
            else if (siteLogoMediaId != null) setSiteLogo(mediaUrl(siteLogoMediaId));

            if (clearSiteSlides) brandingService.setList("site.slides", List.of());
            else if (siteSlideMediaIds != null) brandingService.setList("site.slides", mediaUrls(siteSlideMediaIds));

            if (clearSiteAlbum) brandingService.setList("site.album", List.of());
            else if (siteAlbumMediaIds != null) brandingService.setList("site.album", mediaUrls(siteAlbumMediaIds));

            if (hotelId != null) {
                Hotels hotel = hotelsRepository.findById(hotelId)
                        .orElseThrow(() -> new IllegalArgumentException("Khách sạn/chi nhánh không tồn tại."));
                String prefix = "hotel." + hotelId + ".";
                if (hotelWelcomeText != null && !hotelWelcomeText.isBlank()) brandingService.set(prefix + "welcomeText", hotelWelcomeText.trim());
                if (hotelWelcomeColor != null && hotelWelcomeColor.matches("^#[0-9a-fA-F]{6}$")) brandingService.set(prefix + "welcomeColor", hotelWelcomeColor);
                if (hotelWelcomeEffect != null && List.of("shine", "glow", "none").contains(hotelWelcomeEffect)) brandingService.set(prefix + "welcomeEffect", hotelWelcomeEffect);
                setContentValues(prefix, hotelHeroBadge, hotelHeroAccent, hotelHeroTitle, hotelAboutTitle, hotelAboutText, hotelGalleryTitle, hotelGalleryText, hotelCtaTitle, hotelCtaText);
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

                if (clearHotelAlbum) {
                    brandingService.setList(prefix + "album", List.of());
                } else if (hotelAlbumMediaIds != null) {
                    brandingService.setList(prefix + "album", mediaUrls(hotelAlbumMediaIds));
                }
                hotelsRepository.save(hotel);
            }
            ra.addFlashAttribute("success", "Đã cập nhật logo, slideshow, album ảnh và nội dung trang Home.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", rootMessage(ex));
        }
        return "redirect:/admin/media";
    }


    private void addContentAttributes(Model model, String modelPrefix, String keyPrefix) {
        Map<String, String> values = contentMap(keyPrefix);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            model.addAttribute(modelPrefix + Character.toUpperCase(entry.getKey().charAt(0)) + entry.getKey().substring(1), entry.getValue());
        }
    }

    private Map<String, String> contentMap(String prefix) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("heroBadge", brandingService.get(prefix + "heroBadge", ""));
        values.put("heroAccent", brandingService.get(prefix + "heroAccent", ""));
        values.put("heroTitle", brandingService.get(prefix + "heroTitle", ""));
        values.put("aboutTitle", brandingService.get(prefix + "aboutTitle", ""));
        values.put("aboutText", brandingService.get(prefix + "aboutText", ""));
        values.put("galleryTitle", brandingService.get(prefix + "galleryTitle", ""));
        values.put("galleryText", brandingService.get(prefix + "galleryText", ""));
        values.put("ctaTitle", brandingService.get(prefix + "ctaTitle", ""));
        values.put("ctaText", brandingService.get(prefix + "ctaText", ""));
        return values;
    }

    private void setContentValues(String prefix,
                                  String heroBadge,
                                  String heroAccent,
                                  String heroTitle,
                                  String aboutTitle,
                                  String aboutText,
                                  String galleryTitle,
                                  String galleryText,
                                  String ctaTitle,
                                  String ctaText) {
        if (heroBadge != null) brandingService.set(prefix + "heroBadge", heroBadge);
        if (heroAccent != null) brandingService.set(prefix + "heroAccent", heroAccent);
        if (heroTitle != null) brandingService.set(prefix + "heroTitle", heroTitle);
        if (aboutTitle != null) brandingService.set(prefix + "aboutTitle", aboutTitle);
        if (aboutText != null) brandingService.set(prefix + "aboutText", aboutText);
        if (galleryTitle != null) brandingService.set(prefix + "galleryTitle", galleryTitle);
        if (galleryText != null) brandingService.set(prefix + "galleryText", galleryText);
        if (ctaTitle != null) brandingService.set(prefix + "ctaTitle", ctaTitle);
        if (ctaText != null) brandingService.set(prefix + "ctaText", ctaText);
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

    private String readableFileSize(Media media) {
        if (media == null) return "Không rõ dung lượng";
        try {
            Path path = null;
            if (media.getUploadPath() != null && !media.getUploadPath().isBlank()) {
                path = Paths.get(media.getUploadPath()).toAbsolutePath().normalize();
            }
            if ((path == null || !Files.exists(path)) && media.getFileUrl() != null && media.getFileUrl().startsWith("/uploads/")) {
                String fileName = media.getFileUrl().substring("/uploads/".length());
                path = Paths.get("uploads").resolve(fileName).toAbsolutePath().normalize();
            }
            long bytes = path != null && Files.exists(path) ? Files.size(path) : 0L;
            if (bytes <= 0L) return "Không rõ dung lượng";
            double value = bytes;
            String[] units = {"B", "KB", "MB", "GB"};
            int unit = 0;
            while (value >= 1024 && unit < units.length - 1) {
                value /= 1024.0;
                unit++;
            }
            return unit == 0 ? String.format(Locale.ROOT, "%.0f %s", value, units[unit])
                    : String.format(Locale.ROOT, "%.2f %s", value, units[unit]);
        } catch (Exception ignored) {
            return "Không rõ dung lượng";
        }
    }

    private String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null) root = root.getCause();
        return root.getMessage() == null ? "Có lỗi xảy ra." : root.getMessage();
    }
}
