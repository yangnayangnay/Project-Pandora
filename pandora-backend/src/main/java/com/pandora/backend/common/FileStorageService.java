package com.pandora.backend.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 文件存储服务——处理用户上传文件（如头像）的存储。
 * 支持 JPG/PNG/WEBP 格式，文件命名采用用户ID+UUID，防止冲突。
 */
@Service
public class FileStorageService {

    @Value("${pandora.upload-dir:uploads}")
    private String uploadDir;

    /**
     * 存储用户头像文件。
     * 根据 MIME 类型推断扩展名，使用 UUID 生成唯一文件名。
     * 学习备注: 若 Content-Type 不可识别，默认使用 jpg 扩展名。
     *
     * @param file 上传的文件
     * @param userId 用户ID
     * @return 文件访问路径（如 /uploads/avatars/{filename}）
     * @throws IOException 文件写入失败时抛出
     */
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