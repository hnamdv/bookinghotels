package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.Media;
import org.example.bookinghotels.repository.MediaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;

@Service
public class MediaService {
    private final MediaRepository mediaRepository;
    private final DatabaseSequenceService sequenceService;
    private final Path uploadDirectory;
    private final Path staticUploadDirectory;

    public MediaService(MediaRepository mediaRepository,
                        DatabaseSequenceService sequenceService,
                        @Value("${app.upload-dir:uploadsx}") String uploadDir) {
        this.mediaRepository = mediaRepository;
        this.sequenceService = sequenceService;
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.staticUploadDirectory = Paths.get("src/main/resources/static/uploads").toAbsolutePath().normalize();
    }

    public Media uploadToLocal(MultipartFile file) throws IOException {
        validate(file);
        Files.createDirectories(uploadDirectory);
        Files.createDirectories(staticUploadDirectory);

        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT) : "";
        String safeName = UUID.randomUUID() + extension;

        Path target = uploadDirectory.resolve(safeName).normalize();
        if (!target.startsWith(uploadDirectory)) {
            throw new IOException("Đường dẫn tệp không hợp lệ");
        }
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // Mirror ảnh vào static/uploads để khi push/pull source, máy khác vẫn có file ảnh nếu thư mục này được commit.
        Path staticTarget = staticUploadDirectory.resolve(safeName).normalize();
        if (staticTarget.startsWith(staticUploadDirectory)) {
            Files.copy(target, staticTarget, StandardCopyOption.REPLACE_EXISTING);
        }

        sequenceService.synchronize("media");
        Media media = new Media();
        media.setFileName(safeName);
        media.setFileType(file.getContentType());
        media.setUploadPath(target.toString());
        media.setFileUrl("/uploads/" + safeName);
        return mediaRepository.saveAndFlush(media);
    }

    public void deletePhysicalFile(Media media) {
        if (media == null || media.getFileName() == null) return;
        try {
            if (media.getUploadPath() != null) Files.deleteIfExists(Paths.get(media.getUploadPath()));
        } catch (IOException ignored) {}
        try {
            Files.deleteIfExists(staticUploadDirectory.resolve(media.getFileName()).normalize());
        } catch (IOException ignored) {}
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn ảnh.");
        String type = file.getContentType();
        if (type == null || !type.startsWith("image/")) throw new IllegalArgumentException("Tệp tải lên phải là hình ảnh.");
        if (file.getSize() > 10L * 1024 * 1024) throw new IllegalArgumentException("Ảnh không được vượt quá 10 MB.");
    }
}
