package com.pandora.backend.template;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class Template {
    private Long templateId;
    private Long userId;
    private String templateType;
    private String name;
    private String content;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Integer deleted;
}