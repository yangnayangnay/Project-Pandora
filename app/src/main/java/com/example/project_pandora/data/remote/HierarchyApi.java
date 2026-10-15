package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface HierarchyApi {

    @GET("hierarchy/relations")
    Observable<ApiResponse<List<Map<String, Object>>>> getHierarchyTree();

    @POST("hierarchy/relation")
    Observable<ApiResponse<Map<String, Object>>> addRelation(@Body Map<String, Object> request);

    @DELETE("hierarchy/relation")
    Observable<ApiResponse<Void>> removeRelation(@Query("superiorId") Long superiorId, @Query("subordinateId") Long subordinateId);

    @GET("hierarchy/subordinates/{userId}")
    Observable<ApiResponse<List<Map<String, Object>>>> getSubordinates(@Path("userId") Long userId);

    @GET("hierarchy/superiors/{userId}")
    Observable<ApiResponse<List<Map<String, Object>>>> getSuperiors(@Path("userId") Long userId);
}