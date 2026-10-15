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
import retrofit2.http.Query;

public interface PermissionApi {

    @GET("permission/function/codes")
    Observable<ApiResponse<List<Map<String, Object>>>> getFunctionCodes();

    @GET("permission/function/subjects")
    Observable<ApiResponse<List<Map<String, Object>>>> getSubjects(@Query("functionCode") String functionCode);

    @POST("permission/function/subjects")
    Observable<ApiResponse<Map<String, Object>>> addSubjects(@Body Map<String, Object> request);

    @DELETE("permission/function/subjects")
    Observable<ApiResponse<Void>> removeSubjects(@Body Map<String, Object> request);

    @PUT("permission/function/subjects")
    Observable<ApiResponse<Void>> replaceSubjects(@Body Map<String, Object> request);

    @GET("permission/function/history")
    Observable<ApiResponse<List<Map<String, Object>>>> getHistory(@Query("functionCode") String functionCode,
                                                                   @Query("startDate") String startDate,
                                                                   @Query("endDate") String endDate);
}