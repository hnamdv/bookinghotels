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
                // 1. TẮT HOÀN TOÀN CSRF: Sửa dòng này để fix lỗi chặn đăng nhập Forbidden
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập các file tĩnh, API xác thực và WEBHOOK ngân hàng
                        .requestMatchers(
                                "/", "/home", "/layout", "/layout.html", "/client-home.html",
                                "/login", "/error", "/favicon.ico",
                                "/css/**", "/js/**", "/img/**", "/images/**", "/uploads/**",
                                "/favorites.html", "/roomdetail", "/roomdetail/**", "/room-detail", "/room-detail.html", "/promo-demo.html", "/offers", "/offers.html",
                                "/booking/**", "/invoice/qr",
                                "/api/auth/**", "/api/public/**",
                                "/api/webhook/**" // Mở đường cho SePay bắn tín hiệu
                        ).permitAll()

                        // Hỗ trợ cả role có tiền tố ROLE_ và role cũ không có tiền tố.
                        // Tránh lỗi 403 khi dữ liệu role trong DB không đồng nhất giữa các nhánh.
                        .requestMatchers("/admin/**", "/staff/**", "/api/admin/**").hasAnyAuthority(
                                "ROLE_ADMIN", "ADMIN",
                                "ROLE_MANAGER", "MANAGER",
                                "ROLE_STAFF", "STAFF",
                                "ROLE_USER", "USER",
                                "ROLE_PROMOTION", "PROMOTION",
                                "ROLE_BOOKING", "BOOKING",
                                "ROLE_FWB", "FWB",
                                "ROLE_HOTEL", "HOTEL",
                                "ROLE_IMG", "IMG",
                                "ROLE_ROOM", "ROOM"
                        )

                        .anyRequest().authenticated()
                )
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