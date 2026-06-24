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
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập các trang công khai (không cần login)
                        .requestMatchers(
                                "/", "/home", "/layout", "/layout.html", "/client-home.html",
                                "/login", "/register", "/error", "/favicon.ico",
                                "/css/**", "/js/**", "/img/**", "/images/**", "/uploads/**",
                                "/favorites.html", "/room-detail.html", "/promo-demo.html", "/offers", "/offers.html",
                                "/api/auth/**", "/api/public/**"
                        ).permitAll()

                        // Cho phép truy cập các file HTML tĩnh (cần login)
                        .requestMatchers(
                                "/*.html",
                                "/reception-pos.html",
                                "/fwb-manager.html",
                                "/pos-order.html"
                        ).hasAnyRole("USER", "ADMIN")  // Chỉ USER hoặc ADMIN mới vào được

                        // Các endpoint yêu cầu quyền cụ thể
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/staff/**").hasAnyRole("USER", "ADMIN")

                        // Tất cả request khác đều cần xác thực
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/api/auth/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}