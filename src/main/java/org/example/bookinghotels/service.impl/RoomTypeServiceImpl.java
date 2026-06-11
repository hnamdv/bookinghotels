package org.example.bookinghotels.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import net.coobird.thumbnailator.Thumbnails;
import org.example.bookinghotels.entity.RoomImg;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.RoomImgRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.example.bookinghotels.service.RoomTypeService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomImgRepository roomImgRepository;

    @Autowired
    private Cloudinary cloudinary;

    @Override
    public List<RoomType> getAllRoomTypes() { return roomTypeRepository.findAll(); }

    @Override
    public RoomType createRoomType(RoomType roomType) { return roomTypeRepository.save(roomType); }

    @Override
    public RoomType updateRoomType(Integer id, RoomType roomTypeDetails) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng với ID: " + id));

        // Bạn có thể tạm thời comment dòng này lại bằng dấu // nếu chưa rõ thuộc tính tên trong Entity của bạn là gì
        // roomType.setName(roomTypeDetails.getName());

        return roomTypeRepository.save(roomType);
    }

    @Override
    public void deleteRoomType(Integer id) { roomTypeRepository.deleteById(id); }

    @Override
    @Transactional
    public String uploadRoomTypeImage(Integer roomTypeId, MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File trống hoặc vượt quá dung lượng cho phép (5MB)!");
        }

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Loại phòng không tồn tại!"));

        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) throw new IllegalArgumentException("Tập tin không phải là ảnh hợp lệ!");

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // 🛠️ Thuật toán CHỐNG LAG: Giới hạn chiều ngang tối đa 1920px
        if (width > 1920) {
            double ratio = 1920.0 / width;
            width = 1920;
            height = (int) (height * ratio);
        }

        // Nén chất lượng xuống 75% và ép đuôi về .jpg để tối ưu hóa bộ nhớ dung lượng thấp
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(originalImage)
                .size(width, height)
                .outputFormat("jpg")
                .outputQuality(0.75)
                .toOutputStream(outputStream);

        Map<?, ?> uploadResult = cloudinary.uploader().upload(outputStream.toByteArray(),
                ObjectUtils.asMap("folder", "hotel_room_types"));

        String imageUrl = (String) uploadResult.get("secure_url");

        // Lưu bản ghi vào bảng RoomImg khớp 100% với file entity RoomImg.java của bạn
        RoomImg roomImg = new RoomImg();
        roomImg.setRoomType(roomType);
        roomImg.setImage(imageUrl); // Thuộc tính 'image' dạng String
        roomImgRepository.save(roomImg);

        return imageUrl;
    }
}