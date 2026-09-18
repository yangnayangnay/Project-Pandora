package com.example.project_pandora.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Task implements Serializable {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("priority")
    private String priority;

    @SerializedName("status")
    private String status;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("progress_note")
    private String progressNote;

    @SerializedName("assignee_id")
    private Long assigneeId;

    @SerializedName("dispatcher_id")
    private Long dispatcherId;

    @SerializedName("is_important")
    private Boolean isImportant;

    @SerializedName("is_urgent")
    private Boolean isUrgent;

    @SerializedName("version")
    private Integer version;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public Task() {}

    public Task(String name, String priority, Long assigneeId, Boolean isImportant, Boolean isUrgent) {
        this.name = name;
        this.priority = priority;
        this.assigneeId = assigneeId;
        this.isImportant = isImportant;
        this.isUrgent = isUrgent;
        this.status = "PENDING";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getProgressNote() { return progressNote; }
    public void setProgressNote(String progressNote) { this.progressNote = progressNote; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public Long getDispatcherId() { return dispatcherId; }
    public void setDispatcherId(Long dispatcherId) { this.dispatcherId = dispatcherId; }
    public Boolean getIsImportant() { return isImportant; }
    public void setIsImportant(Boolean isImportant) { this.isImportant = isImportant; }
    public Boolean getIsUrgent() { return isUrgent; }
    public void setIsUrgent(Boolean isUrgent) { this.isUrgent = isUrgent; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getPriorityDisplayName() {
        if (priority == null) return "中";
        switch (priority) {
            case "LOW": return "低";
            case "MEDIUM": return "中";
            case "HIGH": return "高";
            case "URGENT": return "紧急";
            default: return priority;
        }
    }

    public String getStatusDisplayName() {
        if (status == null) return "待接收";
        switch (status) {
            case "PENDING": return "待接收";
            case "IN_PROGRESS": return "进行中";
            case "COMPLETED": return "已完成";
            case "CONFIRMED": return "已确认";
            case "REJECTED": return "已退回";
            default: return status;
        }
    }

    public String getQuadrantName() {
        boolean important = isImportant != null && isImportant;
        boolean urgent = isUrgent != null && isUrgent;
        if (important && urgent) return "重要且紧急";
        if (important && !urgent) return "重要不紧急";
        if (!important && urgent) return "紧急不重要";
        return "不重要不紧急";
    }
}