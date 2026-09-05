package org.example.bookinghotels.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

/**
 * Xác định chủ sở hữu danh sách yêu thích hoàn toàn phía server.
 * - Đã đăng nhập: USER:<username> để giữ qua nhiều phiên đăng nhập.
 * - Khách public: GUEST:<uuid> lấy từ cookie HttpOnly do server cấp.
 * Cookie giúp Favorites vẫn còn khi đóng/mở trình duyệt mà không cần localStorage/JavaScript.
 */
@Service
public class FavoriteOwnerService {

    private static final String COOKIE_NAME = "FH_FAVORITE_OWNER";
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 180; // 180 ngày

    public String resolve(HttpServletRequest request,
                          HttpServletResponse response,
                          Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getName() != null
                && !authentication.getName().isBlank()) {
            return "USER:" + authentication.getName().trim().toLowerCase(Locale.ROOT);
        }

        String token = readGuestToken(request);
        if (token == null) {
            token = UUID.randomUUID().toString();
            Cookie cookie = new Cookie(COOKIE_NAME, token);
            cookie.setHttpOnly(true);
            cookie.setSecure(request.isSecure());
            cookie.setPath("/");
            cookie.setMaxAge(COOKIE_MAX_AGE);
            response.addCookie(cookie);
        }
        return "GUEST:" + token;
    }

    private String readGuestToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (!COOKIE_NAME.equals(cookie.getName())) continue;
            String value = cookie.getValue();
            if (value == null || value.isBlank()) return null;
            try {
                UUID.fromString(value);
                return value;
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }
}
