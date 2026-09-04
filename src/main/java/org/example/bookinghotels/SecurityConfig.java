package org.example.bookinghotels;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // K BẢO VỆ CSRF
                .authorizeHttpRequests(auth -> auth
                        // 1. Các trang public ai cũng vào được
                        .requestMatchers(
                                "/", "/home", "/layout", "/layout.html",
                                "/favorites", "/favorites.html",
                                "/offers", "/offers.html",
                                "/roomdetail", "/roomdetail/**",
                                "/room-detail", "/room-detail.html",
                                "/booking/**", "/payment/**", "/booking-detail/**", "/qr-payment/**",
                                "/login", "/error", "/favicon.ico",
                                "/css/**", "/js/**", "/img/**", "/images/**", "/uploads/**",
                                "/api/auth/**", "/api/public/**", "/api/webhook/**"
                        ).permitAll()

                        // 2. Nhóm Quản lý Đặt phòng -> Cần quyền ROLE_BOOKING
                        .requestMatchers("/admin/bookings", "/admin/bookings/**").hasAuthority("ROLE_BOOKING")

                        // 3. Nhóm Quản lý Phòng & Loại phòng -> Cần quyền ROLE_ROOM
                        .requestMatchers(
                                "/admin/rooms", "/admin/rooms/**",
                                "/admin/room-types", "/admin/room-types/**"
                        ).hasAuthority("ROLE_ROOM")

                        // 4. Nhóm Hóa đơn & Thống kê / Doanh thu -> Thường đi kèm Booking hoặc Admin
                        .requestMatchers("/admin/invoice-list", "/admin/invoice-list/**", "/admin/thongke", "/admin/thongke/**").hasAnyAuthority("ROLE_BOOKING", "ROLE_USER")

                        // 5. Nhóm Khuyến mãi -> Cần quyền ROLE_PROMOTION
                        .requestMatchers("/staff/promotions", "/staff/promotions/**").hasAuthority("ROLE_PROMOTION")

                        // 6. Nhóm Tiện ích (FWB) -> Cần quyền ROLE_FWB
                        .requestMatchers("/staff/fwb/management", "/staff/fwb/**").hasAuthority("ROLE_FWB")

                        // 7. Nhóm Khách sạn & Nhân sự -> Cần quyền ROLE_HOTEL hoặc ADMIN
                        .requestMatchers(
                                "/admin/hotels", "/admin/hotels/**",
                                "/admin/users", "/admin/users/**"
                        ).hasAnyAuthority("ROLE_HOTEL", "ROLE_ADMIN", "ADMIN")

                        // 8. Nhóm Upload Photo (Media) -> Cần quyền ROLE_IMG
                        .requestMatchers("/admin/media", "/admin/media/**").hasAuthority("ROLE_IMG")

                        .requestMatchers(
                                "/admin/logs", "/admin/logs/**",
                                "/admin/trash", "/admin/trash/**",
                                "/api/admin/**"
                        ).hasAnyAuthority("ROLE_USER", "ADMIN")

                        // 10. Các request còn lại bắt buộc phải đăng nhập
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
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
