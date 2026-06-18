package org.example.bookinghotels;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "ten_cloud_cua_ban",
                "api_key", "api_key_cua_ban",
                "api_secret", "api_secret_cua_ban",
                "secure", true
        ));
    }
}