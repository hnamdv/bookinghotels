package org.example.bookinghotels;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
                                "/favorites.html", "/room-detail.html", "/promo-demo.html", "/offers", "/offers.html",
                                "/api/auth/**", "/api/public/**",
                                "/api/webhook/**" // Mở đường cho SePay bắn tín hiệu
                        ).permitAll()

                        .requestMatchers("/admin/**", "/staff/**").hasAnyAuthority(
                                "ROLE_USER", "ROLE_PROMOTION", "ROLE_BOOKING",
                                "ROLE_FWB", "ROLE_HOTEL", "ROLE_IMG", "ROLE_ROOM"
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