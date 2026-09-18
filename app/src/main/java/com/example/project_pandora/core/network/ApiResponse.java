package com.example.project_pandora.core.network;

public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private String traceId;

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public boolean isSuccess() { return code == 200; }
}