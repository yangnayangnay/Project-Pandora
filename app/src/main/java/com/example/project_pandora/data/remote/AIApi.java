package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AIApi {

    @POST("ai/mbti/submit")
    Observable<ApiResponse<Map<String, Object>>> submitMbti(@Body Map<String, Object> request);

    @POST("ai/mbti/report/generate")
    Observable<ApiResponse<Map<String, Object>>> generateMbtiReport(@Body Map<String, Object> request);

    @GET("ai/mbti/report")
    Observable<ApiResponse<Map<String, Object>>> getMbtiReport();

    @POST("ai/keyword")
    Observable<ApiResponse<List<Map<String, Object>>>> keywordAnalysis(@Body Map<String, Object> request);

    @GET("ai/suggestion")
    Observable<ApiResponse<Map<String, Object>>> getSuggestion();

    /**
     * Round24 REQ-24-06: 查询当前登录用户当日是否已算
     * 响应 data: {exists, record{originalName, mutualName, changedName, movingLine, analysis, source, divinationDate}}
     */
    @GET("ai/divination/today")
    Observable<ApiResponse<Map<String, Object>>> getDivinationToday();

    /**
     * Round24 REQ-24-06: 起卦收口接口（服务端生成解读并落库，每日一次幂等锁定）
     * 请求: {originalName(必填), mutualName, changedName, movingLine}
     * 响应 data: {alreadyToday, record{originalName, mutualName, changedName, movingLine, analysis, source, divinationDate}}
     */
    @POST("ai/divination/record")
    Observable<ApiResponse<Map<String, Object>>> recordDivination(@Body Map<String, Object> request);


}