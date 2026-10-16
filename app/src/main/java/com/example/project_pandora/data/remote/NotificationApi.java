package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NotificationApi {

    @GET("notification/list")
    Observable<ApiResponse<List<Map<String, Object>>>> listNotifications(@Query("unreadOnly") Boolean unreadOnly);

    @GET("notification/poll")
    Observable<ApiResponse<List<Map<String, Object>>>> pollNotifications(@Query("lastId") Long lastId);

    @PUT("notification/{id}/read")
    Observable<ApiResponse<Void>> markAsRead(@Path("id") Long id);

    /**
     * TASK-R18-006: 查询通知偏好（5 类通知开关状态）
     */
    @GET("notification/preferences")
    Observable<ApiResponse<Map<String, Object>>> getPreferences();

    /**
     * TASK-R18-006: 保存通知偏好（逐类型 upsert）
     */
    @PUT("notification/preferences")
    Observable<ApiResponse<Void>> updatePreferences(@Body Map<String, Object> preferences);
}