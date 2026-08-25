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
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
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
