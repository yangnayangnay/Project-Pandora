package com.pandora.backend.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Path;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类——配置静态资源映射。
 * 将上传的文件目录映射为可访问的 URL，用于头像等文件展示。
 * 被 Spring 容器启动时加载。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${pandora.upload-dir:uploads}")
    private String uploadDir;

    /**
     * 添加资源处理器，将 /uploads/** URL 映射到本地文件系统目录。
     * 用于访问用户上传的头像等文件。
     *
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString() + "/");
    }
}