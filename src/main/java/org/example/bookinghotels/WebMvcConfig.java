package org.example.bookinghotels;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Khi frontend gọi <img src="/uploads/abc.jpg">, Spring Boot tự vào ổ D lấy ảnh ra render
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///C:/Users/HP/Documents/img/");
    }
}