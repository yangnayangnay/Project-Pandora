package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface LogApi {

    @POST("log")
    Observable<ApiResponse<Map<String, Object>>> createLog(@Body Map<String, Object> request);

    @GET("log/list")
    Observable<ApiResponse<List<Map<String, Object>>>> listLogs(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate,
            @Query("keyword") String keyword
    );

    @GET("log/{logId}")
    Observable<ApiResponse<Map<String, Object>>> getLogDetail(@Path("logId") Long logId);

    @GET("log/stat")
    Observable<ApiResponse<Map<String, Object>>> getLogStats();

    @PUT("log/{logId}")
    Observable<ApiResponse<Map<String, Object>>> editLog(@Path("logId") Long logId, @Body Map<String, Object> request);

    @DELETE("log/{logId}")
    Observable<ApiResponse<Void>> deleteLog(@Path("logId") Long logId);

    @PUT("log/top10")
    Observable<ApiResponse<Void>> updateTop10(@Body List<Map<String, Object>> top10List);

    @GET("log/top10")
    Observable<ApiResponse<List<Map<String, Object>>>> getTop10(@Query("workType") String workType);

    // Round26 B1（4.3 定稿方案）：个人十大增/改/删改走 PersonalTop10Controller（owner 校验 4046），
    // 老端点 PUT log/top10 批量保存因 top10_works 无复合唯一键（ON DUPLICATE 永不触发，纯 INSERT 累积重复行）弃用
    @POST("view/personal-top10/inject")
    Observable<ApiResponse<Map<String, Object>>> injectTop10(@Body Map<String, Object> request);

    @PUT("view/personal-top10/inject/{id}")
    Observable<ApiResponse<Map<String, Object>>> updateTop10Content(@Path("id") Long id, @Body Map<String, Object> request);

    @DELETE("view/personal-top10/inject/{id}")
    Observable<ApiResponse<Void>> deleteTop10(@Path("id") Long id);

    @GET("log/subordinate")
    Observable<ApiResponse<List<Map<String, Object>>>> getSubordinateLogs(
            @Query("subordinateId") Long subordinateId,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate,
            @Query("keyword") String keyword
    );

    @GET("log/subordinates")
    Observable<ApiResponse<List<Map<String, Object>>>> getSubordinates();

    @POST("log/auto-generate")
    Observable<ApiResponse<Map<String, Object>>> autoGenerateLogs();

    @POST("log/draft")
    Observable<ApiResponse<Map<String, Object>>> generateDraft();
}