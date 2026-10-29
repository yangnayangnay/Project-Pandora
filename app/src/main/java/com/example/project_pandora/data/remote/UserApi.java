package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserApi {

    @PUT("user/me/password")
    Observable<ApiResponse<Void>> changePassword(@Body Map<String, String> request);

    @GET("user/me")
    Observable<ApiResponse<Map<String, Object>>> getCurrentUser();

    @PUT("user/me")
    Observable<ApiResponse<Void>> updateProfile(@Body Map<String, Object> request);

    @Multipart
    @POST("user/me/avatar")
    Observable<ApiResponse<Map<String, Object>>> uploadAvatar(@Part MultipartBody.Part file, @Part("source") RequestBody source);

    @GET("user/reset-requests")
    Observable<ApiResponse<Map<String, Object>>> listResetRequests(@Query("status") String status);

    @POST("user/reset-requests/{id}/approve")
    Observable<ApiResponse<Map<String, Object>>> approveResetRequest(@Path("id") Long id, @Body Map<String, String> request);

    @POST("user/reset-requests/{id}/reject")
    Observable<ApiResponse<Void>> rejectResetRequest(@Path("id") Long id, @Body Map<String, String> request);

    @POST("user/{userId}/reset-password")
    Observable<ApiResponse<Map<String, Object>>> resetUserPassword(@Path("userId") Long userId, @Body Map<String, String> request);

    @GET("user/settings")
    Observable<ApiResponse<Map<String, Object>>> getUserSettings();

    @PUT("user/settings")
    Observable<ApiResponse<Void>> updateUserSettings(@Body Map<String, Object> request);

    @PUT("user/birthday")
    Observable<ApiResponse<Map<String, Object>>> updateBirthday(@Body Map<String, Object> request);

    @GET("user/list")
    Observable<ApiResponse<List<Map<String, Object>>>> getAllUsers();

    @GET("user/relations")
    Observable<ApiResponse<Map<String, Object>>> getUserRelations();
}