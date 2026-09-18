package com.example.project_pandora.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class WorkLog implements Serializable {

    @SerializedName("id")
    private Long id;

    @SerializedName("user_id")
    private Long userId;

    @SerializedName("work_item")
    private String workItem;

    @SerializedName("completion_status")
    private String completionStatus;

    @SerializedName("time_cost")
    private Double timeCost;

    @SerializedName("log_date")
    private String logDate;

    @SerializedName("created_at")
    private String createdAt;

    public WorkLog() {}

    public WorkLog(String workItem, String completionStatus, Double timeCost, String logDate) {
        this.workItem = workItem;
        this.completionStatus = completionStatus;
        this.timeCost = timeCost;
        this.logDate = logDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getWorkItem() { return workItem; }
    public void setWorkItem(String workItem) { this.workItem = workItem; }
    public String getCompletionStatus() { return completionStatus; }
    public void setCompletionStatus(String completionStatus) { this.completionStatus = completionStatus; }
    public Double getTimeCost() { return timeCost; }
    public void setTimeCost(Double timeCost) { this.timeCost = timeCost; }
    public String getLogDate() { return logDate; }
    public void setLogDate(String logDate) { this.logDate = logDate; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getCompletionStatusDisplayName() {
        if (completionStatus == null) return "进行中";
        switch (completionStatus) {
            case "COMPLETED": return "已完成";
            case "IN_PROGRESS": return "进行中";
            case "NOT_STARTED": return "未开始";
            default: return completionStatus;
        }
    }
}