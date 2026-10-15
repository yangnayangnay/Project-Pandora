package com.pandora.backend.draft;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class Draft {
    private Long draftId;
    private Long userId;
    private String draftType;
    private String content;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}