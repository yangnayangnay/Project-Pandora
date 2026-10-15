package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AuditApi {

    @GET("audit/my-today")
    Observable<ApiResponse<List<Map<String, Object>>>> getMyTodayAuditLogs();

    @GET("audit/list")
    Observable<ApiResponse<Map<String, Object>>> listAuditLogs(
            @Query("page") int page,
            @Query("size") int size,
            @Query("operatorId") Long operatorId,
            @Query("startTime") String startTime,
            @Query("endTime") String endTime,
            @Query("operationType") String operationType,
            @Query("keyword") String keyword);
}