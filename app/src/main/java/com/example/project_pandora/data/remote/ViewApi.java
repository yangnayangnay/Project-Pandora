package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;

import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ViewApi {

    @GET("view/month")
    Observable<ApiResponse<Map<String, Object>>> getMonthView(@Query("month") String month, @Query("todayCoverage") Boolean todayCoverage);

    @GET("view/week")
    Observable<ApiResponse<Map<String, Object>>> getWeekView(@Query("todayCoverage") Boolean todayCoverage);

    @GET("view/day")
    Observable<ApiResponse<Map<String, Object>>> getDayView(@Query("date") String date, @Query("todayCoverage") Boolean todayCoverage);

    @GET("view/mindmap")
    Observable<ApiResponse<Map<String, Object>>> getMindMap();

    @GET("view/info-map")
    Observable<ApiResponse<Map<String, Object>>> getInfoMap();

    @POST("view/personal-top10/inject")
    Observable<ApiResponse<Map<String, Object>>> injectPersonalTop10(@Body Map<String, Object> request);

    @DELETE("view/personal-top10/inject/{id}")
    Observable<ApiResponse<Void>> deletePersonalTop10(@Path("id") Long id);
}