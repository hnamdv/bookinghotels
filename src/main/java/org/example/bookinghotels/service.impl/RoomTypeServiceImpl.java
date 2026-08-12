package org.example.bookinghotels.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import net.coobird.thumbnailator.Thumbnails;

import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.entity.RoomImg;
import org.example.bookinghotels.entity.RoomType;

import org.example.bookinghotels.repository.HotelsRepository;
import org.example.bookinghotels.repository.RoomImgRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;

import org.example.bookinghotels.service.RoomTypeService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomImgRepository roomImgRepository;
    private final HotelsRepository hotelsRepository;
    private final Cloudinary cloudinary;

    public RoomTypeServiceImpl(
            RoomTypeRepository roomTypeRepository,
            RoomImgRepository roomImgRepository,
            HotelsRepository hotelsRepository,
            Cloudinary cloudinary
    ) {
        this.roomTypeRepository = roomTypeRepository;
        this.roomImgRepository = roomImgRepository;
        this.hotelsRepository = hotelsRepository;
        this.cloudinary = cloudinary;
    }

    // =====================================================
    // LẤY TẤT CẢ LOẠI PHÒNG
    // =====================================================

    @Override
    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findAllWithImages();
    }

    // =====================================================
    // LẤY LOẠI PHÒNG THEO CHI NHÁNH
    // =====================================================

    @Override
    public List<RoomType> getRoomTypesByHotelId(Integer hotelId) {

        return roomTypeRepository.findByHotelId(hotelId);
    }

    // =====================================================
    // LẤY CHI TIẾT LOẠI PHÒNG
    // =====================================================

    @Override
    public RoomType getRoomTypeById(Integer id) {

        return roomTypeRepository.findDetailById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy loại phòng với ID: " + id
                        )
                );
    }

    // =====================================================
    // TẠO LOẠI PHÒNG
    // =====================================================

    @Override
    public RoomType createRoomType(RoomType roomType) {

        return roomTypeRepository.save(roomType);
    }

    // =====================================================
    // TẠO LOẠI PHÒNG CHO CHI NHÁNH ACTIVE
    // =====================================================

    @Override
    public RoomType createRoomTypeForHotel(
            RoomType roomType,
            Integer hotelId
    ) {

        if (hotelId == null) {
            throw new IllegalArgumentException(
                    "Chưa chọn chi nhánh đang hoạt động!"
            );
        }

        Hotels hotel = hotelsRepository.findById(hotelId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy chi nhánh với ID: " + hotelId
                        )
                );

        // Gán loại phòng vào chi nhánh active
        roomType.setHotels(hotel);

        return roomTypeRepository.save(roomType);
    }

    // =====================================================
    // CẬP NHẬT LOẠI PHÒNG
    // =====================================================

    @Override
    public RoomType updateRoomType(
            Integer id,
            RoomType roomTypeDetails
    ) {

        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy loại phòng với ID: " + id
                        )
                );

        roomType.setPrice(roomTypeDetails.getPrice());
        roomType.setCapacity(roomTypeDetails.getCapacity());
        roomType.setNameType(roomTypeDetails.getNameType());
        roomType.setBed(roomTypeDetails.getBed());
        roomType.setDescription(roomTypeDetails.getDescription());

        roomType.setHasBathtub(
                roomTypeDetails.getHasBathtub()
        );

        roomType.setHasWifi(
                roomTypeDetails.getHasWifi()
        );

        roomType.setHasTv(
                roomTypeDetails.getHasTv()
        );

        roomType.setHasBalcony(
                roomTypeDetails.getHasBalcony()
        );

        roomType.setArea(roomTypeDetails.getArea());
        roomType.setBedOptions(roomTypeDetails.getBedOptions());
        roomType.setTotalRooms(roomTypeDetails.getTotalRooms());
        roomType.setTaxAndFee(roomTypeDetails.getTaxAndFee());

        // Nếu form có truyền chi nhánh mới thì cập nhật
        if (roomTypeDetails.getHotels() != null
                && roomTypeDetails.getHotels().getId() != null) {

            Hotels hotel = hotelsRepository.findById(
                    roomTypeDetails.getHotels().getId()
            ).orElseThrow(() ->
                    new RuntimeException(
                            "Không tìm thấy chi nhánh!"
                    )
            );

            roomType.setHotels(hotel);
        }

        return roomTypeRepository.save(roomType);
    }

    // =====================================================
    // XÓA LOẠI PHÒNG
    // =====================================================

    @Override
    public void deleteRoomType(Integer id) {

        roomTypeRepository.deleteById(id);
    }

    // =====================================================
    // UPLOAD ẢNH LOẠI PHÒNG
    // =====================================================

    @Override
    @Transactional
    public String uploadRoomTypeImage(
            Integer roomTypeId,
            MultipartFile file
    ) throws IOException {

        if (file.isEmpty()
                || file.getSize() > 5 * 1024 * 1024) {

            throw new IllegalArgumentException(
                    "File trống hoặc vượt quá dung lượng cho phép (5MB)!"
            );
        }

        RoomType roomType = roomTypeRepository
                .findById(roomTypeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Loại phòng không tồn tại!"
                        )
                );

        BufferedImage originalImage =
                ImageIO.read(file.getInputStream());

        if (originalImage == null) {

            throw new IllegalArgumentException(
                    "Tập tin không phải là ảnh hợp lệ!"
            );
        }

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // Giới hạn chiều ngang tối đa 1920px
        if (width > 1920) {

            double ratio = 1920.0 / width;

            width = 1920;
            height = (int) (height * ratio);
        }

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        Thumbnails.of(originalImage)
                .size(width, height)
                .outputFormat("jpg")
                .outputQuality(0.75)
                .toOutputStream(outputStream);

        Map<?, ?> uploadResult =
                cloudinary.uploader().upload(
                        outputStream.toByteArray(),
                        ObjectUtils.asMap(
                                "folder",
                                "hotel_room_types"
                        )
                );

        String imageUrl =
                (String) uploadResult.get("secure_url");

        RoomImg roomImg = new RoomImg();

        roomImg.setRoomType(roomType);
        roomImg.setImage(imageUrl);

        roomImgRepository.save(roomImg);

        return imageUrl;
    }
}