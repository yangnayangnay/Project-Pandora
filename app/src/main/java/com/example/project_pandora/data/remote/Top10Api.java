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

public interface Top10Api {

    @GET("top10/company-important")
    Observable<ApiResponse<List<Map<String, Object>>>> getCompanyImportant();

    @POST("top10/company-important")
    Observable<ApiResponse<Map<String, Object>>> createCompanyImportant(@Body Map<String, Object> request);

    @PUT("top10/company-important/{id}")
    Observable<ApiResponse<Map<String, Object>>> updateCompanyImportant(@Path("id") long id, @Body Map<String, Object> request);

    @DELETE("top10/company-important/{id}")
    Observable<ApiResponse<Void>> deleteCompanyImportant(@Path("id") long id);

    @GET("top10/company-dispatch")
    Observable<ApiResponse<List<Map<String, Object>>>> getCompanyDispatch();

    @POST("top10/company-dispatch/mark")
    Observable<ApiResponse<Map<String, Object>>> markCompanyDispatch(@Body Map<String, Object> request);

    @DELETE("top10/company-dispatch/mark/{taskId}")
    Observable<ApiResponse<Void>> unmarkCompanyDispatch(@Path("taskId") long taskId);

    @POST("top10/company-dispatch/create")
    Observable<ApiResponse<Map<String, Object>>> createAndMarkCompanyDispatch(@Body Map<String, Object> request);
}