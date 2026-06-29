package org.example.bookinghotels.Controller;

import org.example.bookinghotels.service.FwBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Controller
@RequestMapping("/staff/fwb")
public class FwBController {
    @Autowired
    private FwBService fwBService;

    @GetMapping
    @ResponseBody
    public List<Map<String, Object>> getAll() {
        return fwBService.getAll();
    }

    @GetMapping("/status/{status}")
    @ResponseBody
    public List<Map<String, Object>> getByStatus(@PathVariable String status) {
        return fwBService.getByStatus(status);
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Integer id) {
        Map<String, Object> result = fwBService.getById(id);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @ResponseBody
    public Map<String, Object> create(@RequestBody Map<String, Object> data) {
        return fwBService.save(data);
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id,
                                                      @RequestBody Map<String, Object> data) {
        Map<String, Object> updated = fwBService.update(id, data);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        fwBService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleStatus(@PathVariable Integer id) {
        Map<String, Object> updated = fwBService.toggleStatus(id);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }
    @PostMapping("/upload")
    @ResponseBody
    public Map<String, String> uploadImage(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        Path uploadPath =
                Paths.get(
                        "src/main/resources/static/uploads/fwb/"
                );

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String original =
                file.getOriginalFilename();

        String ext = "";

        if (original != null &&
                original.contains(".")) {

            ext =
                    original.substring(
                            original.lastIndexOf(".")
                    );
        }

        String fileName =
                UUID.randomUUID() + ext;

        Files.copy(
                file.getInputStream(),
                uploadPath.resolve(fileName),
                StandardCopyOption.REPLACE_EXISTING
        );

        return Map.of(
                "url",
                "/uploads/fwb/" + fileName
        );
    }

    // TRẢ VỀ TRANG F&B - Đường dẫn mới
    @GetMapping("/management")
    public String fbManagement() {
        return "html/staff-html/fb-management";
    }
}