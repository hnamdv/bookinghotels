package org.example.bookinghotels.config;

import org.example.bookinghotels.service.RoomInventoryService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Order(100)
public class RoomInventoryStartup implements ApplicationRunner {
    private final RoomInventoryService roomInventoryService;

    public RoomInventoryStartup(RoomInventoryService roomInventoryService) {
        this.roomInventoryService = roomInventoryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        roomInventoryService.ensureAllRoomTypes();
    }
}
