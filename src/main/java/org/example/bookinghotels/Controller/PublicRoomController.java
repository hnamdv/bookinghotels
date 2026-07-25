package org.example.bookinghotels.Controller;

import org.example.bookinghotels.dto.RoomAvailabilityDto;
import org.example.bookinghotels.dto.RoomTypeSummaryDto;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.example.bookinghotels.repository.RoomRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/rooms")
@CrossOrigin(origins = "*")
public class PublicRoomController {

    private final RoomTypeRepository roomTypeRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final RoomRepository roomRepository;

    public PublicRoomController(RoomTypeRepository roomTypeRepository,
                                BookingDetailRepository bookingDetailRepository,
                                RoomRepository roomRepository) {
        this.roomTypeRepository = roomTypeRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.roomRepository = roomRepository;
    }

    @GetMapping
    public ResponseEntity<List<RoomTypeSummaryDto>> getRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) String bed,
            @RequestParam(required = false) Boolean hasWifi,
            @RequestParam(required = false) Boolean hasBathtub,
            @RequestParam(required = false) Boolean hasBalcony,
            @RequestParam(required = false) Boolean hasTv,
            @RequestParam(required = false) Integer hotelId
    ) {
        String keywordValue = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String bedValue = bed == null ? "" : bed.trim().toLowerCase(Locale.ROOT);

        Map<Integer, RoomType> uniqueRoomTypes = new LinkedHashMap<>();
        for (RoomType roomType : roomTypeRepository.findAllWithImages()) {
            if (roomType != null && roomType.getId() != null) {
                uniqueRoomTypes.putIfAbsent(roomType.getId(), roomType);
            }
        }

        List<RoomTypeSummaryDto> rooms = uniqueRoomTypes.values().stream()
                .filter(room -> hotelId == null || (room.getHotels() != null && hotelId.equals(room.getHotels().getId())))
                .filter(room -> keywordValue.isBlank() || containsKeyword(room, keywordValue))
                .filter(room -> minPrice == null || room.getPrice() == null || room.getPrice() >= minPrice)
                .filter(room -> maxPrice == null || room.getPrice() == null || room.getPrice() <= maxPrice)
                .filter(room -> capacity == null || room.getCapacity() == null || room.getCapacity() >= capacity)
                .filter(room -> bedValue.isBlank() || safe(room.getBed()).toLowerCase(Locale.ROOT).contains(bedValue))
                .filter(room -> hasWifi == null || Boolean.TRUE.equals(room.getHasWifi()) == hasWifi)
                .filter(room -> hasBathtub == null || Boolean.TRUE.equals(room.getHasBathtub()) == hasBathtub)
                .filter(room -> hasBalcony == null || Boolean.TRUE.equals(room.getHasBalcony()) == hasBalcony)
                .filter(room -> hasTv == null || Boolean.TRUE.equals(room.getHasTv()) == hasTv)
                .sorted(Comparator.comparing(RoomType::getId, Comparator.nullsLast(Integer::compareTo)))
                .map(RoomTypeSummaryDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomTypeSummaryDto> getRoomDetail(@PathVariable Integer id) {
        return roomTypeRepository.findDetailById(id)
                .map(RoomTypeSummaryDto::new)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<?> getAvailability(@PathVariable Integer id,
                                             @RequestParam LocalDate checkin,
                                             @RequestParam LocalDate checkout) {
        if (!checkout.isAfter(checkin)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ngày đi phải sau ngày đến."));
        }
        return roomTypeRepository.findById(id)
                .<ResponseEntity<?>>map(roomType -> ResponseEntity.ok(toAvailability(roomType, checkin, checkout)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/availability")
    public ResponseEntity<?> getAllAvailability(@RequestParam LocalDate checkin,
                                                @RequestParam LocalDate checkout,
                                                @RequestParam(required = false) Integer hotelId) {
        if (!checkout.isAfter(checkin)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ngày đi phải sau ngày đến."));
        }
        Map<Integer, RoomType> unique = new LinkedHashMap<>();
        for (RoomType roomType : roomTypeRepository.findAllWithImages()) {
            if (roomType != null && roomType.getId() != null
                    && (hotelId == null || (roomType.getHotels() != null && hotelId.equals(roomType.getHotels().getId())))) {
                unique.putIfAbsent(roomType.getId(), roomType);
            }
        }
        return ResponseEntity.ok(unique.values().stream()
                .map(roomType -> toAvailability(roomType, checkin, checkout))
                .toList());
    }

    private RoomAvailabilityDto toAvailability(RoomType roomType, LocalDate checkin, LocalDate checkout) {
        int total = roomRepository.findByRoomTypeId(roomType.getId()).size();
        int available = roomRepository.findAvailableRooms(roomType.getId(), checkin, checkout).size();
        long booked = Math.max(0, total - available);
        return new RoomAvailabilityDto(roomType.getId(), total, booked, available, checkin, checkout);
    }

    private boolean containsKeyword(RoomType room, String keyword) {
        return safe(room.getNameType()).toLowerCase(Locale.ROOT).contains(keyword)
                || safe(room.getDescription()).toLowerCase(Locale.ROOT).contains(keyword)
                || safe(room.getBed()).toLowerCase(Locale.ROOT).contains(keyword)
                || (room.getHotels() != null && safe(room.getHotels().getName()).toLowerCase(Locale.ROOT).contains(keyword))
                || (room.getHotels() != null && safe(room.getHotels().getAddress()).toLowerCase(Locale.ROOT).contains(keyword));
    }

    private String safe(String value) { return value == null ? "" : value; }
}
