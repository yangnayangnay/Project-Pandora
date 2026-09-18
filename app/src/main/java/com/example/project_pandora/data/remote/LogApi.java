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
    Observable<ApiResponse<List<Map<String, Object>>>> listLogs();

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
}