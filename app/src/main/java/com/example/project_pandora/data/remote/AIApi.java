package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface AIApi {

    @POST("ai/mbti/submit")
    Observable<ApiResponse<Map<String, Object>>> submitMbti(@Body Map<String, Object> request);

    @GET("ai/mbti/report")
    Observable<ApiResponse<Map<String, Object>>> getMbtiReport();

    @POST("ai/keyword")
    Observable<ApiResponse<List<Map<String, Object>>>> keywordAnalysis(@Body Map<String, Object> request);

    @GET("ai/suggestion")
    Observable<ApiResponse<Map<String, Object>>> getSuggestion();

    @GET("ai/divination")
    Observable<ApiResponse<Map<String, Object>>> getDivination();
}