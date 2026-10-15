package com.example.project_pandora.ui.widget;

import java.util.Calendar;

public class TaskSpan {
    private long taskId;
    private String taskName;
    private Calendar startDate;
    private Calendar endDate;
    private int colorIndex;
    private int laneIndex;
    private int totalLanes;

    public TaskSpan(long taskId, String taskName, Calendar startDate, Calendar endDate, int colorIndex) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.colorIndex = colorIndex;
    }

    public long getTaskId() { return taskId; }
    public String getTaskName() { return taskName; }
    public Calendar getStartDate() { return startDate; }
    public Calendar getEndDate() { return endDate; }
    public int getColorIndex() { return colorIndex; }
    public int getLaneIndex() { return laneIndex; }
    public int getTotalLanes() { return totalLanes; }

    public void setLaneIndex(int laneIndex) { this.laneIndex = laneIndex; }
    public void setTotalLanes(int totalLanes) { this.totalLanes = totalLanes; }
}