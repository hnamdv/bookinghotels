package org.example.bookinghotels.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.bookinghotels.repository.FavoriteRoomRepository;
import org.example.bookinghotels.service.FavoriteOwnerService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Cấp favoriteCount + favoriteRoomIds cho header/card public từ Repository.
 * Không đọc localStorage.
 */
@ControllerAdvice
public class PublicFavoriteModelAdvice {

    private final FavoriteRoomRepository favoriteRoomRepository;
    private final FavoriteOwnerService favoriteOwnerService;

    public PublicFavoriteModelAdvice(FavoriteRoomRepository favoriteRoomRepository,
                                     FavoriteOwnerService favoriteOwnerService) {
        this.favoriteRoomRepository = favoriteRoomRepository;
        this.favoriteOwnerService = favoriteOwnerService;
    }

    @ModelAttribute
    public void addFavoriteModel(Model model,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 Authentication authentication) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/admin") || uri.startsWith("/staff") || uri.startsWith("/api")) return;

        String ownerKey = favoriteOwnerService.resolve(request, response, authentication);
        List<Integer> ids = favoriteRoomRepository.findRoomTypeIdsByOwnerKey(ownerKey);
        Set<Integer> idSet = new LinkedHashSet<>(ids);
        model.addAttribute("favoriteRoomIds", idSet);
        model.addAttribute("favoriteCount", idSet.size());
    }
}
