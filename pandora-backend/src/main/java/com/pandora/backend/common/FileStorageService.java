package com.pandora.backend.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${pandora.upload-dir:uploads}")
    private String uploadDir;

    public String storeAvatar(MultipartFile file, Long userId) throws IOException {
        String contentType = file.getContentType();
        String ext;
        if ("image/jpeg".equals(contentType) || "image/jpg".equals(contentType)) {
            ext = "jpg";
        } else if ("image/png".equals(contentType)) {
            ext = "png";
        } else if ("image/webp".equals(contentType)) {
            ext = "webp";
        } else {
            ext = "jpg";
        }

        String fileName = userId + "_" + UUID.randomUUID() + "." + ext;
        Path avatarDir = Path.of(uploadDir, "avatars").toAbsolutePath().normalize();
        Files.createDirectories(avatarDir);
        file.transferTo(avatarDir.resolve(fileName));

        return "/uploads/avatars/" + fileName;
    }
}
