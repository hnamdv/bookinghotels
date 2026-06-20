package org.example.bookinghotels.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Service
public class SiteBrandingService {
    private static final String LIST_SEPARATOR = "|";
    private final Path settingsFile;

    public SiteBrandingService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.settingsFile = uploadPath.resolve("site-branding.properties");
    }

    public synchronized String get(String key, String defaultValue) {
        return load().getProperty(key, defaultValue);
    }

    public synchronized List<String> getList(String key) {
        String value = get(key, "");
        if (value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    public synchronized void set(String key, String value) {
        Properties properties = load();
        if (value == null || value.isBlank()) properties.remove(key);
        else properties.setProperty(key, value.trim());
        save(properties);
    }

    public synchronized void setList(String key, List<String> values) {
        if (values == null || values.isEmpty()) {
            set(key, null);
            return;
        }
        set(key, String.join(LIST_SEPARATOR, values));
    }

    private Properties load() {
        Properties properties = new Properties();
        if (!Files.exists(settingsFile)) return properties;
        try (InputStream input = Files.newInputStream(settingsFile)) {
            properties.load(input);
        } catch (IOException ignored) {
        }
        return properties;
    }

    private void save(Properties properties) {
        try {
            Files.createDirectories(settingsFile.getParent());
            try (OutputStream output = Files.newOutputStream(settingsFile)) {
                properties.store(output, "FeelHome site branding");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể lưu cấu hình giao diện: " + ex.getMessage(), ex);
        }
    }
}
