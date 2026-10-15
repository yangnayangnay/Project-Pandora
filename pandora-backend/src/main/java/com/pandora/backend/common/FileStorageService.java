package com.pandora.backend.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = userId + "_" + timestamp + "." + ext;

        File avatarDir = new File(uploadDir + "/avatars/");
        if (!avatarDir.exists()) {
            avatarDir.mkdirs();
        }

        File destFile = new File(avatarDir, fileName);
        file.transferTo(destFile);

        return "/uploads/avatars/" + fileName;
    }
}