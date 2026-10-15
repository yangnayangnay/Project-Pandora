package com.example.project_pandora.core.sync;

public class SyncProgress {
    private final int total;
    private final int successCount;
    private final int failedCount;
    private final boolean completed;

    public SyncProgress(int total, int successCount, int failedCount, boolean completed) {
        this.total = total;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.completed = completed;
    }

    public int getTotal() { return total; }
    public int getSuccessCount() { return successCount; }
    public int getFailedCount() { return failedCount; }
    public boolean isCompleted() { return completed; }

    public float ratio() {
        if (total == 0) return 1.0f;
        return (float) (successCount + failedCount) / total;
    }

    public String displayText() {
        if (total == 0) return "无待同步数据";
        StringBuilder sb = new StringBuilder();
        sb.append(successCount).append("/").append(total).append(" 已同步");
        if (failedCount > 0) {
            sb.append("，").append(failedCount).append(" 条失败");
        }
        return sb.toString();
    }

    public String summaryText() {
        return "同步完成：成功 " + successCount + " 条，失败 " + failedCount + " 条";
    }

    public static SyncProgress empty() {
        return new SyncProgress(0, 0, 0, true);
    }

    public static SyncProgress initial(int total) {
        return new SyncProgress(total, 0, 0, false);
    }
}