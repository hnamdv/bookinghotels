package org.example.bookinghotels.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.bookinghotels.entity.User;
import org.example.bookinghotels.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Làm mới quyền của người đang đăng nhập trực tiếp từ DB trước khi kiểm tra quyền.
 * Nhờ vậy khi quản trị viên thêm/bớt role cho chính tài khoản đang dùng,
 * quyền mới có hiệu lực ngay ở request tiếp theo mà không cần đăng xuất/đăng nhập lại.
 */
public class DynamicRoleRefreshFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public DynamicRoleRefreshFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/admin")
                || path.startsWith("/staff")
                || path.startsWith("/pos")
                || path.startsWith("/api/admin")
                || path.startsWith("/booking/admin"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();

        if (current != null
                && current.isAuthenticated()
                && !(current instanceof AnonymousAuthenticationToken)
                && current.getName() != null) {

            userRepository.findByUsername(current.getName()).ifPresent(user -> refreshAuthentication(current, user));
        }

        filterChain.doFilter(request, response);
    }

    private void refreshAuthentication(Authentication current, User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .toList();

        UsernamePasswordAuthenticationToken refreshed =
                new UsernamePasswordAuthenticationToken(current.getPrincipal(), current.getCredentials(), authorities);
        refreshed.setDetails(current.getDetails());
        SecurityContextHolder.getContext().setAuthentication(refreshed);
    }
}
