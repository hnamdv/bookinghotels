package org.example.bookinghotels;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // Import này
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Kích hoạt @PreAuthorize trên Controller
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
                        // Cho phép truy cập các file tĩnh và API xác thực
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/api/auth/**").permitAll()

                        // Mọi request còn lại chỉ cần ĐĂNG NHẬP là vào được.
                        // Việc phân quyền chi tiết cho từng chức năng sẽ do @PreAuthorize lo.
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
                        .logoutUrl("/api/auth/logout")      // URL gọi để thực hiện logout
                        .logoutSuccessUrl("/login")         // Trang đích sau khi logout thành công
                        .invalidateHttpSession(true)        // Xóa sạch Session
                        .clearAuthentication(true)          // Xóa thông tin xác thực
                        .deleteCookies("JSESSIONID")        // Xóa cookie JSESSIONID
                );


        return http.build();
    }

}