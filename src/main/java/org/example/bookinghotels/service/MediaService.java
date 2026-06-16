package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.Media;
import org.example.bookinghotels.repository.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class MediaService {

    private final String UPLOAD_DIR = "C:/Users/HP/Documents/img/";

    @Autowired
    private MediaRepository mediaRepository;

    public Media uploadToLocal(MultipartFile file) throws IOException {
        File folder = new File(UPLOAD_DIR);
        if (!folder.exists()) {
            boolean created = folder.mkdirs(); // Gán biến để hết bị cảnh báo "ignored result"
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String safeFileName = UUID.randomUUID().toString() + extension;

        Path targetPath = Paths.get(UPLOAD_DIR).resolve(safeFileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        Media media = new Media();
        media.setFileName(safeFileName);
        media.setFileType(file.getContentType());
        media.setUploadPath(targetPath.toAbsolutePath().toString()); // Dùng toAbsolutePath() chuẩn mã nguồn
        media.setFileUrl("/uploads/" + safeFileName);

        return mediaRepository.save(media);
    }
}