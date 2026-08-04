package org.example.bookinghotels;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@EnableAsync
public class BookinghotelsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookinghotelsApplication.class, args);
    }

}
