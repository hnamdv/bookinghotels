package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.RoomType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface RoomTypeService {

    // Lấy tất cả loại phòng
    List<RoomType> getAllRoomTypes();

    // Lấy loại phòng theo chi nhánh
    List<RoomType> getRoomTypesByHotelId(Integer hotelId);

    // Lấy chi tiết loại phòng
    RoomType getRoomTypeById(Integer id);

    // Tạo loại phòng
    RoomType createRoomType(RoomType roomType);

    // Tạo loại phòng và tự gán vào chi nhánh
    RoomType createRoomTypeForHotel(RoomType roomType, Integer hotelId);

    // Cập nhật loại phòng
    RoomType updateRoomType(Integer id, RoomType roomTypeDetails);

    // Xóa loại phòng
    void deleteRoomType(Integer id);

    // Upload ảnh
    String uploadRoomTypeImage(
            Integer roomTypeId,
            MultipartFile file
    ) throws IOException;
}