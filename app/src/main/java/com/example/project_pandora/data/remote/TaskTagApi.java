package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;
import java.util.*;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.*;

public interface TaskTagApi {
    @GET("task-tags") Observable<ApiResponse<List<Map<String, Object>>>> list();
    @POST("task-tags") Observable<ApiResponse<Map<String, Object>>> create(@Body Map<String, Object> body);
    @PATCH("task-tags/{id}") Observable<ApiResponse<Map<String, Object>>> update(@Path("id") long id, @Body Map<String, Object> body);
    @DELETE("task-tags/{id}") Observable<ApiResponse<Void>> delete(@Path("id") long id);
}
