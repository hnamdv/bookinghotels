package org.example.bookinghotels.Controller;

import jakarta.transaction.Transactional;
import org.example.bookinghotels.dto.RoomTypeSummaryDto;
import org.example.bookinghotels.entity.RoomFavorite;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.RoomFavoriteRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
public class FavoriteRoomController {

    private final RoomFavoriteRepository favoriteRepository;
    private final RoomTypeRepository roomTypeRepository;

    public FavoriteRoomController(RoomFavoriteRepository favoriteRepository, RoomTypeRepository roomTypeRepository) {
        this.favoriteRepository = favoriteRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<RoomTypeSummaryDto>> getFavorites(@PathVariable Integer userId) {
        List<Integer> ids = favoriteRepository.findByUserId(userId)
                .stream()
                .map(RoomFavorite::getRoomType)
                .filter(roomType -> roomType != null && roomType.getId() != null)
                .map(RoomType::getId)
                .collect(Collectors.toList());

        if (ids.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<RoomTypeSummaryDto> rooms = roomTypeRepository.findByIdInWithImages(ids)
                .stream()
                .map(RoomTypeSummaryDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rooms);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addFavorite(
            @RequestParam Integer userId,
            @RequestParam Integer roomTypeId
    ) {
        if (favoriteRepository.existsByUserIdAndRoomType_Id(userId, roomTypeId)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Phòng đã có trong danh sách yêu thích"));
        }

        RoomType roomType = roomTypeRepository.findById(roomTypeId).orElse(null);
        if (roomType == null) {
            return ResponseEntity.notFound().build();
        }

        RoomFavorite favorite = new RoomFavorite();
        favorite.setUserId(userId);
        favorite.setRoomType(roomType);
        favoriteRepository.save(favorite);

        return ResponseEntity.ok(Map.of("success", true, "message", "Đã lưu phòng yêu thích"));
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> removeFavorite(
            @RequestParam Integer userId,
            @RequestParam Integer roomTypeId
    ) {
        favoriteRepository.deleteByUserIdAndRoomType_Id(userId, roomTypeId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã hủy lưu phòng yêu thích"));
    }
}
