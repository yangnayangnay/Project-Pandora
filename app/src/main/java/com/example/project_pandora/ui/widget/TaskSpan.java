package com.example.project_pandora.ui.widget;

import java.util.Calendar;

public class TaskSpan {

    /** R36 REQ-36-A1: 横线来源——我的任务（assignee）与我布置的任务（dispatcher）双源视觉区分 */
    public static final int SOURCE_MINE = 0;
    public static final int SOURCE_DISPATCHED = 1;

    private long taskId;
    private String taskName;
    private Calendar startDate;
    private Calendar endDate;
    private int colorIndex;
    private int laneIndex;
    private int totalLanes;
    private int source = SOURCE_MINE;

    public TaskSpan(long taskId, String taskName, Calendar startDate, Calendar endDate, int colorIndex) {
        this(taskId, taskName, startDate, endDate, colorIndex, SOURCE_MINE);
    }

    public TaskSpan(long taskId, String taskName, Calendar startDate, Calendar endDate, int colorIndex, int source) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.colorIndex = colorIndex;
        this.source = source;
    }

    public int getSource() { return source; }
    public void setSource(int source) { this.source = source; }

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