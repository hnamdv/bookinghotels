package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.Media;
import org.example.bookinghotels.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/media")
public class AdminMediaController {

    @Autowired
    private MediaService mediaService;

    // API phục vụ admin bấm nút tải ảnh lên ổ đĩa
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Media savedMedia = mediaService.uploadToLocal(file);
            return ResponseEntity.ok(Map.of(
                    "message", "Upload lên thư mục chỉ định thành công giống WordPress!",
                    "url", savedMedia.getFileUrl() // Trả link ảo dạng /uploads/xxx.jpg về cho frontend hiện ảnh
            ));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi hệ thống không thể lưu file!"));
        }
    }
}