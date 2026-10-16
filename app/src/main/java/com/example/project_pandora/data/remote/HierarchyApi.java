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

    /**
     * TASK-R18-012: 新增 getTree 方法调 GET /hierarchy/tree
     * 返回 {roots, orphanNodes} 包装结构，节点含 id/username/realName/displayName/role/children
     * 保留 getHierarchyTree（实际调 relations 接口）不动，向后兼容
     */
    @GET("hierarchy/tree")
    Observable<ApiResponse<Map<String, Object>>> getTree();

    @POST("hierarchy/relation")
    Observable<ApiResponse<Map<String, Object>>> addRelation(@Body Map<String, Object> request);

    @DELETE("hierarchy/relation")
    Observable<ApiResponse<Void>> removeRelation(@Query("superiorId") Long superiorId, @Query("subordinateId") Long subordinateId);

    @GET("hierarchy/subordinates/{userId}")
    Observable<ApiResponse<List<Map<String, Object>>>> getSubordinates(@Path("userId") Long userId);

    @GET("hierarchy/superiors/{userId}")
    Observable<ApiResponse<List<Map<String, Object>>>> getSuperiors(@Path("userId") Long userId);
}