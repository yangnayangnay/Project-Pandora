package com.example.project_pandora.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Top10Work implements Serializable {

    @SerializedName("id")
    private Long id;

    @SerializedName("user_id")
    private Long userId;

    @SerializedName("work_type")
    private String workType;

    @SerializedName("rank_order")
    private Integer rankOrder;

    @SerializedName("content")
    private String content;

    @SerializedName("related_task_id")
    private Long relatedTaskId;

    public Top10Work() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }
    public Integer getRankOrder() { return rankOrder; }
    public void setRankOrder(Integer rankOrder) { this.rankOrder = rankOrder; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getRelatedTaskId() { return relatedTaskId; }
    public void setRelatedTaskId(Long relatedTaskId) { this.relatedTaskId = relatedTaskId; }

    public String getWorkTypeDisplayName() {
        if (workType == null) return "个人重要";
        switch (workType) {
            case "COMPANY_IMPORTANT": return "公司重要";
            case "COMPANY_DISPATCH": return "公司派发";
            case "PERSONAL_IMPORTANT": return "个人重要";
            default: return workType;
        }
    }
}