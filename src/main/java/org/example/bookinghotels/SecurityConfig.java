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
                        // 1. Các trang cho phép truy cập tự do (không cần đăng nhập)
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/api/auth/**").permitAll()

                        // 2. TẤT CẢ các URL bắt đầu bằng /admin/... đều phải đăng nhập
                        // Cú pháp .requestMatchers("/admin/**") sẽ bao phủ mọi đường dẫn con
                        .requestMatchers("/admin/**", "/staff/**").authenticated()
                        // 3. Nếu bạn muốn phân quyền cụ thể:
                        // .requestMatchers("/admin/users/**").hasAuthority("ADMIN")

                        // 4. Mọi request khác cũng yêu cầu đăng nhập
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                );

        return http.build();
    }
    // Trong SecurityConfig.java, bạn có thể tạo 1 Bean filter đơn giản
    @Bean
    public Filter loggingFilter() {
        return (request, response, chain) -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                System.out.println("DEBUG - Request tới: " + ((HttpServletRequest)request).getRequestURI());
                System.out.println("DEBUG - User hiện tại: " + auth.getName() + " | Quyền: " + auth.getAuthorities());
            }
            chain.doFilter(request, response);
        };
    }
}