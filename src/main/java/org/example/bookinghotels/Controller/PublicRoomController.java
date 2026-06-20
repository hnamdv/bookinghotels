package org.example.bookinghotels.Controller;

import org.example.bookinghotels.dto.RoomTypeSummaryDto;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/rooms")
@CrossOrigin(origins = "*")
public class PublicRoomController {

    private final RoomTypeRepository roomTypeRepository;

    public PublicRoomController(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
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

        List<RoomTypeSummaryDto> rooms = uniqueRoomTypes.values()
                .stream()
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

    private boolean containsKeyword(RoomType room, String keyword) {
        return safe(room.getNameType()).toLowerCase(Locale.ROOT).contains(keyword)
                || safe(room.getDescription()).toLowerCase(Locale.ROOT).contains(keyword)
                || safe(room.getBed()).toLowerCase(Locale.ROOT).contains(keyword)
                || (room.getHotels() != null && safe(room.getHotels().getName()).toLowerCase(Locale.ROOT).contains(keyword))
                || (room.getHotels() != null && safe(room.getHotels().getAddress()).toLowerCase(Locale.ROOT).contains(keyword));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
