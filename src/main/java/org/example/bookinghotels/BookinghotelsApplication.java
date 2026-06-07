package org.example.bookinghotels;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class BookinghotelsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookinghotelsApplication.class, args);
    }

}
