package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;

import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("auth/login")
    Observable<ApiResponse<Map<String, Object>>> login(@Body Map<String, String> request);

    @POST("auth/register")
    Observable<ApiResponse<Map<String, Object>>> register(@Body Map<String, String> request);

    @POST("auth/logout")
    Observable<ApiResponse<Void>> logout();

    @POST("auth/refresh")
    Observable<ApiResponse<Map<String, String>>> refresh(@Body Map<String, String> request);
}