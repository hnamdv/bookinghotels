package org.example.bookinghotels.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.bookinghotels.entity.FavoriteRoom;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.FavoriteRoomRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.example.bookinghotels.service.FavoriteOwnerService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.time.LocalDateTime;

/**
 * Controller thao tác Favorites bằng POST form + Repository.
 * Không dùng localStorage và không cần client-favorites.js.
 */
@Controller
public class FavoriteController {

    private final FavoriteRoomRepository favoriteRoomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final FavoriteOwnerService favoriteOwnerService;

    public FavoriteController(FavoriteRoomRepository favoriteRoomRepository,
                              RoomTypeRepository roomTypeRepository,
                              FavoriteOwnerService favoriteOwnerService) {
        this.favoriteRoomRepository = favoriteRoomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.favoriteOwnerService = favoriteOwnerService;
    }

    @PostMapping("/favorites/toggle")
    @Transactional
    public String toggle(@RequestParam Integer roomTypeId,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         Authentication authentication) {
        String ownerKey = favoriteOwnerService.resolve(request, response, authentication);

        if (favoriteRoomRepository.existsByOwnerKeyAndRoomType_Id(ownerKey, roomTypeId)) {
            favoriteRoomRepository.deleteByOwnerKeyAndRoomType_Id(ownerKey, roomTypeId);
        } else {
            RoomType roomType = roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("Loại phòng không tồn tại: " + roomTypeId));

            FavoriteRoom favorite = new FavoriteRoom();
            favorite.setOwnerKey(ownerKey);
            favorite.setRoomType(roomType);
            favorite.setCreatedAt(LocalDateTime.now());
            favoriteRoomRepository.saveAndFlush(favorite);
        }

        return redirectBack(request, "/favorites");
    }

    @PostMapping("/favorites/remove")
    @Transactional
    public String remove(@RequestParam Integer roomTypeId,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         Authentication authentication) {
        String ownerKey = favoriteOwnerService.resolve(request, response, authentication);
        favoriteRoomRepository.deleteByOwnerKeyAndRoomType_Id(ownerKey, roomTypeId);
        return redirectBack(request, "/favorites");
    }

    @PostMapping("/favorites/clear")
    @Transactional
    public String clear(HttpServletRequest request,
                        HttpServletResponse response,
                        Authentication authentication) {
        String ownerKey = favoriteOwnerService.resolve(request, response, authentication);
        favoriteRoomRepository.deleteByOwnerKey(ownerKey);
        return "redirect:/favorites";
    }

    private String redirectBack(HttpServletRequest request, String fallback) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) return "redirect:" + fallback;
        try {
            URI uri = URI.create(referer);
            String path = uri.getPath();
            if (path == null || path.isBlank() || !path.startsWith("/") || path.startsWith("//")) {
                return "redirect:" + fallback;
            }
            String query = uri.getRawQuery();
            return "redirect:" + path + (query == null || query.isBlank() ? "" : "?" + query);
        } catch (Exception ignored) {
            return "redirect:" + fallback;
        }
    }
}
