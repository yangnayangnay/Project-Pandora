package com.example.project_pandora.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Task implements Serializable {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

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

    @SerializedName("is_urgent")
    private Boolean isUrgent;

    @SerializedName("version")
    private Integer version;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public Task() {}

    public Task(String name, Long assigneeId, Boolean isUrgent) {
        this.name = name;
        this.assigneeId = assigneeId;
        this.isUrgent = isUrgent;
        this.status = "IN_PROGRESS";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    public Boolean getIsUrgent() { return isUrgent; }
    public void setIsUrgent(Boolean isUrgent) { this.isUrgent = isUrgent; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getStatusDisplayName() {
        if (status == null) return "进行中";
        switch (status) {
            case "IN_PROGRESS": return "进行中";
            case "COMPLETED": return "已完成";
            default: return status;
        }
    }

}
