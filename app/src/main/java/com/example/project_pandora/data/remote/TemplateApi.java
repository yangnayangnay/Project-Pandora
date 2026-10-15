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

public interface TemplateApi {

    @POST("template")
    Observable<ApiResponse<Map<String, Object>>> saveTemplate(@Body Map<String, Object> request);

    @GET("template")
    Observable<ApiResponse<List<Map<String, Object>>>> listTemplates(@Query("templateType") String templateType);

    @PUT("template/{templateId}")
    Observable<ApiResponse<Void>> updateTemplate(@Path("templateId") Long templateId, @Body Map<String, Object> request);

    @DELETE("template/{templateId}")
    Observable<ApiResponse<Void>> deleteTemplate(@Path("templateId") Long templateId);

    @POST("template/migrate")
    Observable<ApiResponse<Map<String, Object>>> migrateTemplates(@Body Map<String, Object> request);
}