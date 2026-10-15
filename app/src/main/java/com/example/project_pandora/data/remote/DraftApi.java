package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;

import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface DraftApi {

    @PUT("draft")
    Observable<ApiResponse<Map<String, Object>>> saveDraft(@Body Map<String, Object> request);

    @GET("draft")
    Observable<ApiResponse<Map<String, Object>>> getDraft(@Query("draftType") String draftType);

    @DELETE("draft")
    Observable<ApiResponse<Void>> deleteDraft(@Query("draftType") String draftType);
}