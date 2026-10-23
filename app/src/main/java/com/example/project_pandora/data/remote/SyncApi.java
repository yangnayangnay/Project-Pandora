package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * R44 REQ-D: 云中继推送接口（活跃端=云时批量推送 PENDING+RELAY_PENDING 队列）。
 * 契约（云端 SyncController）：请求 {items:[{clientKey,type,action,sourceId,content}]}；
 * 响应 data.synced[] 逐条 {clientId,status:SYNCED/SKIPPED/NO_MAPPING,cloudId}。
 */
public interface SyncApi {

    @POST("sync")
    Observable<ApiResponse<Map<String, Object>>> relaySync(@Body Map<String, Object> request);

    /** 便捷请求体构建 */
    static Map<String, Object> buildRequest(List<Map<String, Object>> items) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("items", items);
        return body;
    }
}