package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.RoomType;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface RoomTypeService {
    List<RoomType> getAllRoomTypes();
    RoomType createRoomType(RoomType roomType);
    RoomType updateRoomType(Integer id, RoomType roomTypeDetails);
    void deleteRoomType(Integer id);
    // Hàm upload ảnh cho Loại phòng
    String uploadRoomTypeImage(Integer roomTypeId, MultipartFile file) throws IOException;
}