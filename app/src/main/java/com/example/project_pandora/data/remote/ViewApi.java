package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;

import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ViewApi {

    @GET("view/month")
    Observable<ApiResponse<Map<String, Object>>> getMonthView(@Query("month") String month);

    @GET("view/week")
    Observable<ApiResponse<Map<String, Object>>> getWeekView();

    @GET("view/day")
    Observable<ApiResponse<Map<String, Object>>> getDayView(@Query("date") String date);

    @GET("view/mindmap")
    Observable<ApiResponse<Map<String, Object>>> getMindMap();

    @GET("view/info-map")
    Observable<ApiResponse<Map<String, Object>>> getInfoMap();
}