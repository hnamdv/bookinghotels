package org.example.bookinghotels;

import jakarta.servlet.http.HttpServletResponse;
import org.example.bookinghotels.repository.UserRepository;
import org.example.bookinghotels.security.DynamicRoleRefreshFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.net.URI;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserRepository userRepository) throws Exception {
        DynamicRoleRefreshFilter dynamicRoleRefreshFilter = new DynamicRoleRefreshFilter(userRepository);

        http
                .csrf(csrf -> csrf.disable())

                // Làm mới role từ DB trước bước authorization.
                // Role vừa cấp sẽ có hiệu lực ngay ở request kế tiếp, không cần login lại.
                .addFilterBefore(dynamicRoleRefreshFilter, AuthorizationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        // Trang/thành phần public
                        .requestMatchers(
                                "/", "/home", "/layout", "/layout.html", "/client-home.html",
                                "/login", "/error", "/access-denied", "/favicon.ico",
                                "/css/**", "/js/**", "/img/**", "/images/**", "/uploads/**",
                                "/favorites", "/roomdetail", "/roomdetail/**", "/room-detail", "/room-detail.html",
                                "/promo-demo.html", "/offers", "/offers.html",
                                "/invoice/qr",
                                "/api/auth/**", "/api/public/**", "/api/webhook/**"
                        ).permitAll()

                        // ===== NGHIỆP VỤ NHÂN VIÊN =====
                        // Mọi tài khoản nhân viên đã đăng nhập đều được dùng 3 nghiệp vụ này.
                        .requestMatchers(
                                "/admin/bookings", "/admin/bookings/**",
                                "/admin/api/booking-details", "/admin/api/booking-details/**",
                                "/api/admin/booking-details", "/api/admin/booking-details/**",
                                "/admin/rooms", "/admin/rooms/**",
                                "/admin/invoice-list", "/admin/invoice-list/**",
                                "/admin/invoice-list-custom", "/admin/invoice-list-custom/**",
                                "/admin/invoice/delete", "/admin/invoice/delete/**",
                                "/booking/admin", "/booking/admin/**"
                        ).authenticated()

                        // Luồng đặt phòng phía khách vẫn public.
                        .requestMatchers("/booking/**").permitAll()

                        // ===== HỆ THỐNG QUẢN LÝ - PHÂN QUYỀN THEO ROLE =====
                        .requestMatchers("/admin/users", "/admin/users/**", "/api/admin/users", "/api/admin/users/**")
                        .hasAuthority("ROLE_USER")

                        .requestMatchers("/admin/room-types", "/admin/room-types/**")
                        .hasAuthority("ROLE_ROOM")

                        .requestMatchers("/admin/hotels", "/admin/hotels/**", "/api/admin/hotels", "/api/admin/hotels/**")
                        .hasAuthority("ROLE_HOTEL")

                        .requestMatchers("/staff/promotions", "/staff/promotions/**")
                        .hasAuthority("ROLE_PROMOTION")

                        .requestMatchers("/staff/fwb", "/staff/fwb/**", "/admin/addons", "/admin/addons/**", "/admin/services", "/admin/services/**", "/pos", "/pos/**")
                        .hasAuthority("ROLE_FWB")

                        .requestMatchers("/admin/media", "/admin/media/**")
                        .hasAuthority("ROLE_IMG")

                        .requestMatchers("/admin/thongke", "/admin/thongke/**")
                        .hasAnyAuthority("ROLE_BOOKING", "ROLE_HOTEL", "ROLE_ROOM")

                        .requestMatchers("/admin/logs", "/admin/logs/**", "/admin/trash", "/admin/trash/**")
                        .hasAnyAuthority("ROLE_HOTEL", "ROLE_ROOM")

                        .requestMatchers("/admin")
                        .authenticated()

                        // Mọi request khác không public đều cần đăng nhập.
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, denied) -> {
                    String path = request.getRequestURI();
                    String accept = request.getHeader("Accept");
                    String requestedWith = request.getHeader("X-Requested-With");

                    boolean apiRequest = path.startsWith("/api/")
                            || "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                            || (accept != null && accept.contains("application/json"));

                    if (apiRequest) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setCharacterEncoding("UTF-8");
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Bạn không có quyền thực hiện chức năng này.\"}");
                        return;
                    }

                    String backUrl = "/admin";
                    String referer = request.getHeader("Referer");
                    if (referer != null && !referer.isBlank()) {
                        try {
                            URI uri = URI.create(referer);
                            String candidate = uri.getRawPath();
                            if (candidate != null
                                    && candidate.startsWith("/")
                                    && !candidate.equals(path)
                                    && !candidate.startsWith("/access-denied")) {
                                backUrl = candidate;
                                if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
                                    backUrl += "?" + uri.getRawQuery();
                                }
                            }
                        } catch (IllegalArgumentException ignored) {
                            // Referer không hợp lệ -> dùng /admin.
                        }
                    }

                    request.getSession(true).setAttribute("ACCESS_DENIED_BACK_URL", backUrl);
                    response.sendRedirect("/access-denied");
                }))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/login")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }
}
