package com.example.project_pandora.core.network;

public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private String traceId;
    /** D-33-02 修复：通知轮询专用可选字段（服务端全局通知 MAX(id)）；其他端点为 null（Gson 容忍缺失） */
    private Long maxNotificationId;

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Long getMaxNotificationId() { return maxNotificationId; }
    public void setMaxNotificationId(Long maxNotificationId) { this.maxNotificationId = maxNotificationId; }

    public boolean isSuccess() { return code == 200; }
}