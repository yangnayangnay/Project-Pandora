package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.PATCH;
import retrofit2.http.Query;

public interface TaskApi {

    @POST("task")
    Observable<ApiResponse<Map<String, Object>>> createTask(@Body Map<String, Object> task);

    @GET("task/list")
    Observable<ApiResponse<List<Map<String, Object>>>> listTasks(
            @Query("status") String status,
            @Query("assigneeId") Long assigneeId);

    @GET("task/{taskId}")
    Observable<ApiResponse<Map<String, Object>>> getTask(@Path("taskId") Long taskId);

    @PATCH("task/{taskId}/progress")
    Observable<ApiResponse<Map<String, Object>>> updateProgress(
            @Path("taskId") Long taskId,
            @Body Map<String, Object> request);

    @GET("task/quadrant")
    Observable<ApiResponse<Map<String, Object>>> getQuadrantTasks();

    @POST("task/{taskId}/transition")
    Observable<ApiResponse<Map<String, Object>>> transitionStatus(
            @Path("taskId") Long taskId,
            @Body Map<String, String> request);
}