package org.example.bookinghotels.Controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bookinghotels.entity.*;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.example.bookinghotels.repository.FwbRepository;
import org.example.bookinghotels.repository.HotelsRepository;
import org.example.bookinghotels.repository.PromotionRoomTypeRepository;
import org.example.bookinghotels.repository.RoomRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.example.bookinghotels.service.SiteBrandingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class HomeController {

    private static final String FALLBACK_IMAGE = "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80";

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final PromotionRoomTypeRepository promotionRoomTypeRepository;
    private final FwbRepository fwbRepository;
    private final HotelsRepository hotelsRepository;
    private final SiteBrandingService brandingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HomeController(RoomTypeRepository roomTypeRepository,
                          RoomRepository roomRepository,
                          BookingDetailRepository bookingDetailRepository,
                          PromotionRoomTypeRepository promotionRoomTypeRepository,
                          FwbRepository fwbRepository,
                          HotelsRepository hotelsRepository,
                          SiteBrandingService brandingService) {
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.promotionRoomTypeRepository = promotionRoomTypeRepository;
        this.fwbRepository = fwbRepository;
        this.hotelsRepository = hotelsRepository;
        this.brandingService = brandingService;
    }

    @GetMapping("/home")
    public String home(@RequestParam(required = false) LocalDate checkin,
                       @RequestParam(required = false) LocalDate checkout,
                       @RequestParam(required = false) Double minPrice,
                       @RequestParam(required = false) Double maxPrice,
                       @RequestParam(required = false) Integer capacity,
                       @RequestParam(required = false) String bed,
                       @RequestParam(required = false) Boolean hasWifi,
                       @RequestParam(required = false) Boolean hasBathtub,
                       @RequestParam(required = false) Boolean hasBalcony,
                       @RequestParam(required = false) List<Integer> amenityIds,
                       @RequestParam(required = false) Integer hotelId,
                       Model model) {
        DateRange range = normalizeRange(checkin, checkout);
        List<Hotels> hotels = hotelsRepository.findAll();
        Hotels selectedHotel = hotelId == null ? null : hotelsRepository.findById(hotelId).orElse(null);

        List<RoomCard> rooms = buildRoomCards(range.checkin(), range.checkout()).stream()
                .filter(card -> hotelId == null || Objects.equals(card.hotelId(), hotelId))
                .filter(card -> minPrice == null || card.effectivePrice() >= minPrice)
                .filter(card -> maxPrice == null || card.effectivePrice() <= maxPrice)
                .filter(card -> capacity == null || card.capacity() >= capacity)
                .filter(card -> bed == null || bed.isBlank() || card.bed().toLowerCase(Locale.ROOT).contains(bed.toLowerCase(Locale.ROOT)))
                .filter(card -> amenityIds == null || amenityIds.isEmpty() || card.amenityIds().containsAll(amenityIds))
                .filter(card -> !Boolean.TRUE.equals(hasWifi) || card.hasWifi())
                .filter(card -> !Boolean.TRUE.equals(hasBathtub) || card.hasBathtub())
                .filter(card -> !Boolean.TRUE.equals(hasBalcony) || card.hasBalcony())
                .toList();

        Hotels activeHotel = selectedHotel;
        if (activeHotel == null) {
            Set<Integer> visibleHotelIds = rooms.stream()
                    .map(RoomCard::hotelId)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (visibleHotelIds.size() == 1) {
                Integer onlyHotelId = visibleHotelIds.iterator().next();
                activeHotel = hotels.stream()
                        .filter(h -> Objects.equals(h.getId(), onlyHotelId))
                        .findFirst()
                        .orElseGet(() -> hotelsRepository.findById(onlyHotelId).orElse(null));
            }
        }
        String contentPrefix = activeHotel == null || activeHotel.getId() == null ? "site." : "hotel." + activeHotel.getId() + ".";
        List<String> heroSlides = resolveHeroSlides(rooms, contentPrefix);
        String defaultSiteName = brandingService.get("site.name", "FEELHOME HOTEL");
        String activeName = activeHotel == null || activeHotel.getName() == null || activeHotel.getName().isBlank()
                ? defaultSiteName
                : activeHotel.getName();
        String defaultLogo = brandingService.get("site.logo", brandingService.get("site.circleLogo", brandingService.get("site.headerLogo", "")));
        String activeLogo = activeHotel == null
                ? defaultLogo
                : brandingService.get(contentPrefix + "logo", activeHotel.getLogo() == null || activeHotel.getLogo().isBlank() ? defaultLogo : activeHotel.getLogo());

        model.addAttribute("rooms", rooms);
        model.addAttribute("hotels", hotels);
        model.addAttribute("selectedHotelId", hotelId);
        model.addAttribute("activeHotelName", activeName);
        model.addAttribute("featuredRooms", rooms.stream().limit(3).toList());
        model.addAttribute("offers", rooms.stream().filter(RoomCard::hasPromotion).limit(3).toList());
        model.addAttribute("siteName", activeName);
        model.addAttribute("siteLogo", activeLogo);
        model.addAttribute("siteHeroBadge", contentText(contentPrefix, "heroBadge", "Khách sạn nghỉ dưỡng"));
        model.addAttribute("siteHeroAccent", contentText(contentPrefix, "heroAccent", "FeelHome"));
        model.addAttribute("siteHeroTitle", contentText(contentPrefix, "heroTitle", "đặt phòng nhanh, trải nghiệm bền vững"));
        model.addAttribute("siteWelcomeText", contentText(contentPrefix, "welcomeText", "Kiến tạo những khoảng nghỉ được chăm chút riêng cho bạn"));
        model.addAttribute("siteAboutTitle", contentText(contentPrefix, "aboutTitle", "Không gian nghỉ dưỡng có thể thay đổi hình ảnh linh động từ admin"));
        model.addAttribute("siteAboutText", contentText(contentPrefix, "aboutText", "Logo, slideshow, ảnh phòng, ảnh dịch vụ và hình ảnh thư viện đều đi qua phần Quản lý hình ảnh. Khi đổi ảnh trong admin và gắn vào đúng mục, giao diện khách hàng sẽ cập nhật theo dữ liệu hiện có."));
        model.addAttribute("siteGalleryTitle", contentText(contentPrefix, "galleryTitle", "Album hình ảnh nổi bật"));
        model.addAttribute("siteGalleryText", contentText(contentPrefix, "galleryText", "Nút Xem hình ảnh sẽ cuộn xuống album này. Ảnh trong album được quản lý tại Upload Photo cho từng khách sạn hoặc mặc định toàn hệ thống."));
        model.addAttribute("siteCtaTitle", contentText(contentPrefix, "ctaTitle", "Sẵn sàng đặt kỳ nghỉ tiếp theo?"));
        model.addAttribute("siteCtaText", contentText(contentPrefix, "ctaText", "Khách có thể lọc phòng, xem chi tiết, đặt loại phòng và chọn dịch vụ cộng bill ở trang thanh toán."));
        model.addAttribute("siteHeroSlides", heroSlides);
        model.addAttribute("galleryImages", resolveAlbumImages(contentPrefix, heroSlides, rooms));
        model.addAttribute("heroRoomCount", rooms.size());
        model.addAttribute("heroHotelCount", rooms.stream().map(RoomCard::hotelName).filter(Objects::nonNull).distinct().count());
        model.addAttribute("checkin", range.checkin());
        model.addAttribute("checkout", range.checkout());
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("capacity", capacity);
        model.addAttribute("bed", bed);
        model.addAttribute("hasWifi", Boolean.TRUE.equals(hasWifi));
        model.addAttribute("hasBathtub", Boolean.TRUE.equals(hasBathtub));
        model.addAttribute("hasBalcony", Boolean.TRUE.equals(hasBalcony));
        model.addAttribute("amenityIds", amenityIds == null ? List.of() : amenityIds);
        model.addAttribute("hotelId", hotelId);
        model.addAttribute("roomAmenityOptions", getFreeAmenityOptions());
        return "html/client-html/home";
    }

    @GetMapping({"/offers", "/offers.html"})
    public String offers(@RequestParam(required = false) LocalDate checkin,
                         @RequestParam(required = false) LocalDate checkout,
                         Model model) {
        DateRange range = normalizeRange(checkin, checkout);
        List<RoomCard> offers = buildRoomCards(range.checkin(), range.checkout()).stream()
                .filter(RoomCard::hasPromotion)
                .sorted(Comparator.comparing(RoomCard::discountPercent).reversed())
                .toList();
        addPublicBranding(model);
        model.addAttribute("offers", offers);
        model.addAttribute("checkin", range.checkin());
        model.addAttribute("checkout", range.checkout());
        return "html/client-html/offers";
    }

    @GetMapping({"/favorites", "/favorites.html"})
    public String favorites(@RequestParam(required = false) LocalDate checkin,
                            @RequestParam(required = false) LocalDate checkout,
                            Model model) {
        DateRange range = normalizeRange(checkin, checkout);
        addPublicBranding(model);
        model.addAttribute("rooms", buildRoomCards(range.checkin(), range.checkout()));
        model.addAttribute("checkin", range.checkin());
        model.addAttribute("checkout", range.checkout());
        return "html/client-html/favorites";
    }

    private void addPublicBranding(Model model) {
        String siteName = brandingService.get("site.name", "FEELHOME HOTEL");
        String siteLogo = brandingService.get("site.logo",
                brandingService.get("site.circleLogo", brandingService.get("site.headerLogo", "")));
        model.addAttribute("siteName", siteName);
        model.addAttribute("siteLogo", siteLogo);
        model.addAttribute("siteWelcomeText", brandingService.get("site.welcomeText",
                "Kiến tạo những khoảng nghỉ được chăm chút riêng cho bạn"));
    }

    @GetMapping("/roomdetail/{id}")
    public String roomDetailPath(@PathVariable Integer id,
                                 @RequestParam(required = false) LocalDate checkin,
                                 @RequestParam(required = false) LocalDate checkout,
                                 Model model) {
        RoomType roomType = roomTypeRepository.findDetailById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Không tìm thấy loại phòng."));

        DateRange range = normalizeRange(checkin, checkout);
        Map<Integer, RoomCount> counts = buildRoomCounts(range.checkin(), range.checkout());
        RoomCount count = counts.getOrDefault(roomType.getId(), new RoomCount(roomRepository.findByRoomTypeId(roomType.getId()).size(), roomRepository.findByRoomTypeId(roomType.getId()).size()));
        Promotion promotion = findBestPromotion(roomType.getId(), range.checkin(), range.checkout());
        double originalPrice = safePrice(roomType);
        double discountPercent = promotion == null || promotion.getDiscountPercent() == null ? 0D : promotion.getDiscountPercent();
        double discountedPrice = discountPercent > 0 ? originalPrice * (100D - discountPercent) / 100D : originalPrice;

        List<String> images = getImages(roomType);
        Hotels detailHotel = roomType.getHotels();
        String detailPrefix = detailHotel == null || detailHotel.getId() == null ? "site." : "hotel." + detailHotel.getId() + ".";
        String detailName = detailHotel == null || detailHotel.getName() == null || detailHotel.getName().isBlank()
                ? brandingService.get("site.name", "FEELHOME HOTEL")
                : detailHotel.getName();
        String detailLogo = detailHotel == null
                ? brandingService.get("site.logo", brandingService.get("site.circleLogo", brandingService.get("site.headerLogo", "")))
                : brandingService.get(detailPrefix + "logo", detailHotel.getLogo() == null ? "" : detailHotel.getLogo());
        String detailWelcome = brandingService.get(detailPrefix + "welcomeText",
                brandingService.get("site.welcomeText", "Kiến tạo những khoảng nghỉ được chăm chút riêng cho bạn"));
        AmenityData amenityData = parseAmenityData(roomType.getBedOptions());
        long nightCount = (range.checkin() != null && range.checkout() != null) ? java.time.temporal.ChronoUnit.DAYS.between(range.checkin(), range.checkout()) : 0L;

        model.addAttribute("siteName", detailName);
        model.addAttribute("siteLogo", detailLogo);
        model.addAttribute("siteWelcomeText", detailWelcome);
        model.addAttribute("room", roomType);
        model.addAttribute("hotel", detailHotel);
        model.addAttribute("images", images);
        model.addAttribute("mainImage", images.get(0));
        model.addAttribute("bedOptionChips", parseBedOptions(roomType.getBedOptions()));
        model.addAttribute("amenityNames", amenityData.names());
        model.addAttribute("promotion", promotion);
        model.addAttribute("hasPromotion", promotion != null && discountPercent > 0);
        model.addAttribute("originalPrice", originalPrice);
        model.addAttribute("discountedPrice", discountedPrice);
        model.addAttribute("discountPercent", Math.round(discountPercent));
        model.addAttribute("promotionEndAt", promotionEndIso(promotion));
        model.addAttribute("checkin", range.checkin());
        model.addAttribute("checkout", range.checkout());
        model.addAttribute("totalRooms", count.total());
        model.addAttribute("availableRooms", count.available());
        model.addAttribute("nightCount", nightCount);
        model.addAttribute("bookingUrl", buildBookingUrl(roomType.getId(), range.checkin(), range.checkout()));
        return "html/client-html/roomdetail";
    }

    @GetMapping("/roomdetail")
    public String roomDetailQuery(@RequestParam(required = false) Integer id,
                                  @RequestParam(required = false) LocalDate checkin,
                                  @RequestParam(required = false) LocalDate checkout) {
        if (id == null) return "redirect:/home";
        return "redirect:/roomdetail/" + id + buildDateQuery(checkin, checkout);
    }

    @GetMapping({"/room-detail", "/room-detail.html"})
    public String oldRoomDetailAlias(@RequestParam(required = false) Integer id,
                                     @RequestParam(required = false) LocalDate checkin,
                                     @RequestParam(required = false) LocalDate checkout) {
        if (id == null) return "redirect:/home";
        return "redirect:/roomdetail/" + id + buildDateQuery(checkin, checkout);
    }

    @GetMapping({"/", "/layout", "/layout.html"})
    public String homeAliases() {
        return "redirect:/home";
    }

    private List<RoomCard> buildRoomCards(LocalDate checkin, LocalDate checkout) {
        Map<Integer, RoomCount> counts = buildRoomCounts(checkin, checkout);
        Map<Integer, Promotion> promoByRoomType = buildPromotionMap(checkin, checkout);
        List<RoomCard> cards = new ArrayList<>();
        for (RoomType roomType : roomTypeRepository.findAllWithImages()) {
            if (roomType == null || roomType.getId() == null) continue;
            RoomCount count = counts.getOrDefault(roomType.getId(), new RoomCount(0, 0));
            Promotion promotion = promoByRoomType.get(roomType.getId());
            double originalPrice = safePrice(roomType);
            double discountPercent = promotion == null || promotion.getDiscountPercent() == null ? 0D : promotion.getDiscountPercent();
            double effectivePrice = discountPercent > 0 ? originalPrice * (100D - discountPercent) / 100D : originalPrice;
            Hotels hotel = roomType.getHotels();
            AmenityData amenityData = parseAmenityData(roomType.getBedOptions());

            cards.add(new RoomCard(
                    roomType.getId(),
                    hotel == null ? null : hotel.getId(),
                    roomType.getNameType(),
                    hotel == null ? "FeelHome Hotel" : hotel.getName(),
                    getImages(roomType).get(0),
                    originalPrice,
                    effectivePrice,
                    Math.round(discountPercent),
                    promotion != null && discountPercent > 0,
                    promotion == null ? null : promotion.getPromotionName(),
                    promotionEndIso(promotion),
                    count.total(),
                    count.available(),
                    roomType.getCapacity() == null ? 1 : roomType.getCapacity(),
                    roomType.getBed() == null ? "" : roomType.getBed(),
                    roomType.getDescription() == null ? "" : roomType.getDescription(),
                    Boolean.TRUE.equals(roomType.getHasWifi()),
                    Boolean.TRUE.equals(roomType.getHasBathtub()),
                    Boolean.TRUE.equals(roomType.getHasBalcony()),
                    Boolean.TRUE.equals(roomType.getHasTv()),
                    amenityData.ids(),
                    amenityData.names(),
                    "/roomdetail/" + roomType.getId() + buildDateQueryWithHotel(checkin, checkout, hotel == null ? null : hotel.getId()),
                    buildBookingUrl(roomType.getId(), checkin, checkout)
            ));
        }
        return cards;
    }

    private Map<Integer, RoomCount> buildRoomCounts(LocalDate checkin, LocalDate checkout) {
        Map<Integer, Integer> totalByType = new HashMap<>();
        for (Object[] row : roomRepository.countRoomsGroupByRoomType()) {
            if (row[0] != null) totalByType.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
        }

        Map<Integer, Integer> occupiedByType = new HashMap<>();
        if (checkin != null && checkout != null) {
            for (Object[] row : bookingDetailRepository.countOccupiedRoomsByRoomType(checkin, checkout)) {
                if (row[0] != null) occupiedByType.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            }
        }

        Map<Integer, RoomCount> result = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : totalByType.entrySet()) {
            int total = entry.getValue();
            int occupied = occupiedByType.getOrDefault(entry.getKey(), 0);
            result.put(entry.getKey(), new RoomCount(total, Math.max(0, total - occupied)));
        }
        return result;
    }

    private Map<Integer, Promotion> buildPromotionMap(LocalDate checkin, LocalDate checkout) {
        Map<Integer, Promotion> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (PromotionRoomType mapping : promotionRoomTypeRepository.findAll()) {
            if (mapping == null || mapping.getRoomType() == null || mapping.getPromotion() == null) continue;
            Promotion promotion = mapping.getPromotion();
            if (!isPromotionActiveForStay(promotion, checkin, checkout, now)) continue;
            Integer roomTypeId = mapping.getRoomType().getId();
            Promotion current = result.get(roomTypeId);
            double newDiscount = promotion.getDiscountPercent() == null ? 0D : promotion.getDiscountPercent();
            double oldDiscount = current == null || current.getDiscountPercent() == null ? 0D : current.getDiscountPercent();
            if (current == null || newDiscount > oldDiscount) result.put(roomTypeId, promotion);
        }
        return result;
    }

    private Promotion findBestPromotion(Integer roomTypeId, LocalDate checkin, LocalDate checkout) {
        return buildPromotionMap(checkin, checkout).get(roomTypeId);
    }

    private boolean isPromotionActiveForStay(Promotion promotion, LocalDate checkin, LocalDate checkout, LocalDateTime now) {
        if (promotion == null) return false;
        LocalDate start = promotion.getStartDate() == null ? LocalDate.MIN : promotion.getStartDate();
        LocalDate end = promotion.getEndDate() == null ? LocalDate.MAX : promotion.getEndDate();
        LocalTime startTime = promotion.getStartTime() == null ? LocalTime.MIN : promotion.getStartTime();
        LocalTime endTime = promotion.getEndTime() == null ? LocalTime.of(23, 59, 59) : promotion.getEndTime();
        LocalDateTime startAt = LocalDateTime.of(start, startTime);
        LocalDateTime endAt = LocalDateTime.of(end, endTime);
        if (now.isBefore(startAt) || now.isAfter(endAt)) return false;

        if (checkin == null || checkout == null) return true;
        LocalDate lastNight = checkout.minusDays(1);
        return !checkin.isBefore(start) && !lastNight.isAfter(end);
    }

    private String promotionEndIso(Promotion promotion) {
        if (promotion == null || promotion.getEndDate() == null) return null;
        LocalTime time = promotion.getEndTime() == null ? LocalTime.of(23, 59, 59) : promotion.getEndTime();
        return LocalDateTime.of(promotion.getEndDate(), time).toString();
    }

    private double safePrice(RoomType roomType) {
        return roomType.getPrice() == null ? 0D : roomType.getPrice();
    }

    private List<String> getImages(RoomType roomType) {
        if (roomType.getImages() == null) return List.of(FALLBACK_IMAGE);
        List<String> images = roomType.getImages().stream()
                .map(RoomImg::getImage)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        return images.isEmpty() ? List.of(FALLBACK_IMAGE) : images;
    }

    private DateRange normalizeRange(LocalDate checkin, LocalDate checkout) {
        if (checkin == null) return new DateRange(null, null);
        LocalDate out = checkout;
        if (out == null || !out.isAfter(checkin)) out = checkin.plusDays(1);
        return new DateRange(checkin, out);
    }


    private String contentText(String prefix, String key, String defaultValue) {
        String value = brandingService.get(prefix + key, "");
        if (value == null || value.isBlank()) value = brandingService.get("site." + key, "");
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private List<String> resolveHeroSlides(List<RoomCard> rooms, String prefix) {
        List<String> slides = brandingService.getList(prefix + "slides");
        if ((slides == null || slides.isEmpty()) && !"site.".equals(prefix)) slides = brandingService.getList("site.slides");
        if (slides != null) {
            List<String> cleaned = slides.stream().filter(value -> value != null && !value.isBlank()).distinct().toList();
            if (!cleaned.isEmpty()) return cleaned;
        }
        return rooms.stream().map(RoomCard::image).filter(value -> value != null && !value.isBlank()).distinct().limit(4).toList();
    }

    private List<String> resolveAlbumImages(String prefix, List<String> heroSlides, List<RoomCard> rooms) {
        List<String> album = brandingService.getList(prefix + "album");
        if ((album == null || album.isEmpty()) && !"site.".equals(prefix)) {
            album = brandingService.getList("site.album");
        }
        if (album != null) {
            List<String> cleaned = album.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .limit(16)
                    .toList();
            if (!cleaned.isEmpty()) return cleaned;
        }
        return buildGalleryImages(heroSlides, rooms);
    }

    private List<String> buildGalleryImages(List<String> heroSlides, List<RoomCard> rooms) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (heroSlides != null) values.addAll(heroSlides);
        rooms.stream().map(RoomCard::image).filter(value -> value != null && !value.isBlank()).forEach(values::add);
        if (values.isEmpty()) values.add(FALLBACK_IMAGE);
        return values.stream().limit(12).toList();
    }


    private List<FwB> getFreeAmenityOptions() {
        return fwbRepository.findAll().stream()
                .filter(item -> item != null)
                .filter(item -> item.getStatus() == null || item.getStatus().isBlank() || "ACTIVE".equalsIgnoreCase(item.getStatus()) || "SHOW".equalsIgnoreCase(item.getStatus()))
                .filter(item -> item.getPrice() <= 0D)
                .sorted(Comparator.comparing(FwB::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private AmenityData parseAmenityData(String raw) {
        if (raw == null || raw.trim().isBlank() || !raw.trim().startsWith("{")) return new AmenityData(List.of(), List.of());
        try {
            Map<String, Object> payload = objectMapper.readValue(raw.trim(), new TypeReference<Map<String, Object>>() {});
            LinkedHashSet<Integer> ids = new LinkedHashSet<>();
            LinkedHashSet<String> names = new LinkedHashSet<>();
            Object rawIds = payload.get("amenityIds");
            if (rawIds instanceof Collection<?> collection) {
                for (Object item : collection) {
                    try { if (item != null) ids.add(Integer.parseInt(item.toString())); } catch (Exception ignored) {}
                }
            }
            Object amenities = payload.get("amenities");
            if (amenities instanceof Collection<?> collection) {
                for (Object item : collection) {
                    if (item instanceof Map<?, ?> map) {
                        Object id = map.get("id");
                        Object name = map.get("name");
                        try { if (id != null) ids.add(Integer.parseInt(id.toString())); } catch (Exception ignored) {}
                        if (name != null && !name.toString().isBlank()) names.add(name.toString());
                    }
                }
            }
            return new AmenityData(ids.stream().toList(), names.stream().toList());
        } catch (Exception ignored) {
            return new AmenityData(List.of(), List.of());
        }
    }

    private List<String> parseBedOptions(String raw) {
        if (raw == null || raw.trim().isBlank()) return List.of();
        String value = raw.trim();
        List<String> result = new ArrayList<>();
        try {
            if (value.startsWith("[")) {
                List<String> list = objectMapper.readValue(value, new TypeReference<List<String>>() {});
                return list.stream().filter(item -> item != null && !item.isBlank()).distinct().toList();
            }
            if (value.startsWith("{")) {
                Map<String, Object> payload = objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
                Object text = payload.get("text");
                if (text != null) addDelimited(result, text.toString());
                Object options = payload.get("options");
                if (options instanceof Collection<?> collection) {
                    collection.forEach(item -> { if (item != null) result.add(item.toString()); });
                }
                Object amenities = payload.get("amenities");
                if (amenities instanceof Collection<?> collection) {
                    for (Object item : collection) {
                        if (item instanceof Map<?, ?> map && map.get("name") != null) {
                            result.add(map.get("name").toString());
                        } else if (item != null) {
                            result.add(item.toString());
                        }
                    }
                }
                return result.stream().filter(item -> item != null && !item.isBlank()).distinct().toList();
            }
        } catch (Exception ignored) {}
        addDelimited(result, value);
        return result.stream().filter(item -> item != null && !item.isBlank()).distinct().toList();
    }

    private void addDelimited(List<String> result, String value) {
        if (value == null) return;
        Arrays.stream(value.split("[;,|\\n]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .forEach(result::add);
    }

    private String buildBookingUrl(Integer roomTypeId, LocalDate checkin, LocalDate checkout) {
        StringBuilder builder = new StringBuilder("/booking/check?roomTypeId=").append(roomTypeId);
        roomTypeRepository.findById(roomTypeId)
                .map(RoomType::getHotels)
                .filter(h -> h != null && h.getId() != null)
                .ifPresent(h -> builder.append("&hotelId=").append(h.getId()));
        if (checkin != null) builder.append("&checkin=").append(checkin);
        if (checkout != null) builder.append("&checkout=").append(checkout);
        return builder.toString();
    }

    private String buildDateQueryWithHotel(LocalDate checkin, LocalDate checkout, Integer hotelId) {
        List<String> params = new ArrayList<>();
        if (checkin != null) params.add("checkin=" + checkin);
        if (checkout != null) params.add("checkout=" + checkout);
        if (hotelId != null) params.add("hotelId=" + hotelId);
        return params.isEmpty() ? "" : "?" + String.join("&", params);
    }

    private String buildDateQuery(LocalDate checkin, LocalDate checkout) {
        List<String> params = new ArrayList<>();
        if (checkin != null) params.add("checkin=" + checkin);
        if (checkout != null) params.add("checkout=" + checkout);
        return params.isEmpty() ? "" : "?" + String.join("&", params);
    }

    public record DateRange(LocalDate checkin, LocalDate checkout) {}
    public record RoomCount(int total, int available) {}
    public record AmenityData(List<Integer> ids, List<String> names) {}
    public record RoomCard(
            Integer id,
            Integer hotelId,
            String name,
            String hotelName,
            String image,
            double originalPrice,
            double effectivePrice,
            long discountPercent,
            boolean hasPromotion,
            String promotionName,
            String promotionEndAt,
            int totalRooms,
            int availableRooms,
            int capacity,
            String bed,
            String description,
            boolean hasWifi,
            boolean hasBathtub,
            boolean hasBalcony,
            boolean hasTv,
            List<Integer> amenityIds,
            List<String> amenityNames,
            String detailUrl,
            String bookingUrl
    ) {}
}
