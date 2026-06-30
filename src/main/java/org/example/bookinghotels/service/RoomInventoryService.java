package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.entity.RoomImg;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.RoomRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class RoomInventoryService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    public RoomInventoryService(RoomRepository roomRepository,
                                RoomTypeRepository roomTypeRepository) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    /**
     * Đồng bộ số phòng vật lý theo RoomType.totalRooms.
     * Chỉ tự tạo phòng còn thiếu; không tự xóa phòng để tránh mất lịch sử booking.
     */
    @Transactional
    public int ensurePhysicalRooms(RoomType roomType) {
        if (roomType == null || roomType.getId() == null) return 0;

        int requested = roomType.getTotalRooms() == null ? 1 : Math.max(1, roomType.getTotalRooms());
        List<Room> existing = roomRepository.findByRoomTypeId(roomType.getId());
        int missing = requested - existing.size();

        if (missing > 0) {
            int nextNumber = nextNumericRoomNumber();
            String thumbnail = firstImage(roomType);
            for (int i = 0; i < missing; i++) {
                while (roomRepository.existsByRoomNumberIgnoreCase(String.valueOf(nextNumber))) {
                    nextNumber++;
                }
                Room room = new Room();
                room.setRoomType(roomType);
                room.setRoomNumber(String.valueOf(nextNumber));
                room.setSlug("phong-" + nextNumber);
                room.setThumbnail(thumbnail);
                roomRepository.save(room);
                nextNumber++;
            }
            roomRepository.flush();
        }

        return syncActualRoomCount(roomType);
    }

    /**
     * Đồng bộ RoomType.totalRooms theo đúng số bản ghi Room vật lý hiện có.
     * Dùng sau khi thêm, sửa, chuyển loại hoặc xóa phòng ở trang quản lý phòng.
     */
    @Transactional
    public int syncActualRoomCount(RoomType roomType) {
        if (roomType == null || roomType.getId() == null) return 0;
        int actual = roomRepository.findByRoomTypeId(roomType.getId()).size();
        if (!Integer.valueOf(actual).equals(roomType.getTotalRooms())) {
            roomType.setTotalRooms(actual);
            roomTypeRepository.saveAndFlush(roomType);
        }
        return actual;
    }

    @Transactional
    public void ensureAllRoomTypes() {
        for (RoomType roomType : roomTypeRepository.findAll()) {
            ensurePhysicalRooms(roomType);
        }
    }

    private int nextNumericRoomNumber() {
        return roomRepository.findAll().stream()
                .map(Room::getRoomNumber)
                .filter(value -> value != null && value.matches("\\d+"))
                .map(Integer::valueOf)
                .max(Comparator.naturalOrder())
                .map(max -> Math.max(101, max + 1))
                .orElse(101);
    }

    private String firstImage(RoomType roomType) {
        if (roomType.getImages() == null) return null;
        return roomType.getImages().stream()
                .map(RoomImg::getImage)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
