package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.Media;
import org.example.bookinghotels.entity.RoomImg;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.MediaRepository;
import org.example.bookinghotels.repository.RoomImgRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoomTypeImageService {

    private final RoomImgRepository roomImgRepository;
    private final MediaRepository mediaRepository;
    private final DatabaseSequenceService sequenceService;

    public RoomTypeImageService(RoomImgRepository roomImgRepository,
                                MediaRepository mediaRepository,
                                DatabaseSequenceService sequenceService) {
        this.roomImgRepository = roomImgRepository;
        this.mediaRepository = mediaRepository;
        this.sequenceService = sequenceService;
    }

    /**
     * Đồng bộ tuyệt đối ảnh của loại phòng theo danh sách checkbox được gửi lên.
     * Ảnh không còn được tick sẽ bị gỡ; ảnh còn tick chỉ tồn tại đúng một bản ghi.
     */
    @Transactional
    public void replaceImages(RoomType roomType, List<Integer> mediaIds) {
        if (roomType == null || roomType.getId() == null) {
            throw new IllegalArgumentException("Loại phòng chưa được lưu nên không thể cập nhật ảnh.");
        }

        Set<Integer> uniqueIds = mediaIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(mediaIds);

        LinkedHashSet<String> selectedUrls = new LinkedHashSet<>();
        if (!uniqueIds.isEmpty()) {
            for (Media media : mediaRepository.findAllById(uniqueIds)) {
                if (media.getFileUrl() != null && !media.getFileUrl().isBlank()) {
                    selectedUrls.add(media.getFileUrl().trim());
                }
            }
        }

        // Xóa toàn bộ liên kết cũ trước, kể cả các bản ghi URL bị trùng từ dữ liệu cũ.
        roomImgRepository.deleteAllByRoomTypeIdBulk(roomType.getId());
        roomImgRepository.flush();

        if (selectedUrls.isEmpty()) {
            return;
        }

        sequenceService.synchronize("room_img");
        for (String imageUrl : selectedUrls) {
            RoomImg roomImg = new RoomImg();
            roomImg.setRoomType(roomType);
            roomImg.setImage(imageUrl);
            roomImgRepository.save(roomImg);
        }
        roomImgRepository.flush();
    }
}
