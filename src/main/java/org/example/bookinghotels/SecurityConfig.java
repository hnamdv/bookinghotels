package org.example.bookinghotels;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
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

    // CHỈ GIỮ 1 BEAN SecurityFilterChain DUY NHẤT
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Cho phép tất cả các đường dẫn không cần đăng nhập (TẠM THỜI để test)
                        .requestMatchers(
                                "/",
                                "/home",
                                "/home/layout",
                                "/layout.html",
                                "/room-detail.html",
                                "/favorites.html",
                                "/offers.html",
                                "/login.html",
                                "/register.html",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/img/**",
                                "/favicon.ico",
                                "/api/public/**",
                                "/api/favorites/**",
                                "/api/favorites",
                                "/login",
                                "/api/auth/**",
                                "/admin/**",
                                "/booking/**"
                        ).permitAll()
                        .anyRequest().permitAll()  // TẠM THỜI cho phép tất cả
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                );

        return http.build();
    }

    // Filter log (tuỳ chọn)
    @Bean
    public Filter loggingFilter() {
        return (request, response, chain) -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                System.out.println("DEBUG - Request tới: " + ((HttpServletRequest) request).getRequestURI());
                System.out.println("DEBUG - User hiện tại: " + auth.getName() + " | Quyền: " + auth.getAuthorities());
            }
            chain.doFilter(request, response);
        };
    }

    // UserDetailsService tạm thời (dùng in-memory để test)
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin")
                        .password("{noop}123456")  // {noop} = plain text password
                        .roles("ADMIN")
                        .authorities("ADMIN")
                        .build()
        );
    }
}