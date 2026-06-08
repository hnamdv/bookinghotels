package org.example.bookinghotels;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http

                .csrf(csrf -> csrf.disable())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Cho phép truy cập công khai vào trang chủ và các file tĩnh
                        .requestMatchers("/", "/index.html", "/static/**", "/*.js", "/*.css", "/images/**").permitAll()

                        // 2. Cho phép truy cập công khai vào API Đăng nhập/Đăng ký
                        .requestMatchers("/api/auth/**").permitAll()

                        // 3. Các API còn lại mới yêu cầu có token
                        .anyRequest().permitAll() // Cho phép tất cả mọi thứ
                );
        return http.build();
    }
    }
