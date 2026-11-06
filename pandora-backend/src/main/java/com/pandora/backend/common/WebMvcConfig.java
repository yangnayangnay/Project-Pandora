package com.pandora.backend.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Path;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${pandora.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString() + "/");
    }
}
