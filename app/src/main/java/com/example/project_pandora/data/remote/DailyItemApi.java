package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;
import java.util.*;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.*;

public interface DailyItemApi {
    @GET("daily-items") Observable<ApiResponse<List<Map<String, Object>>>> list(@Query("month") String month);
    @GET("daily-items/date/{day}") Observable<ApiResponse<List<Map<String, Object>>>> listForDate(@Path("day") String day);
    @POST("daily-items") Observable<ApiResponse<Map<String, Object>>> create(@Body Map<String, Object> request);
    @PATCH("daily-items/{id}") Observable<ApiResponse<Map<String, Object>>> update(@Path("id") long id, @Body Map<String, Object> request);
    @DELETE("daily-items/{id}") Observable<ApiResponse<Void>> delete(@Path("id") long id);
}
