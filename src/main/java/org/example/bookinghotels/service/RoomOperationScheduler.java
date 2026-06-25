package org.example.bookinghotels.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RoomOperationScheduler {
    private final RoomOperationService roomOperationService;

    public RoomOperationScheduler(RoomOperationService roomOperationService) {
        this.roomOperationService = roomOperationService;
    }

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void processRoomOperations() {
        roomOperationService.sendLateCheckInReminders();
        roomOperationService.sendCheckoutDueReminders();
        roomOperationService.autoCheckoutDueBookings();
    }
}
