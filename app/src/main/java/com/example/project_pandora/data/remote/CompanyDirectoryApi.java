package com.example.project_pandora.data.remote;

import com.example.project_pandora.core.network.ApiResponse;
import java.util.List;
import java.util.Map;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CompanyDirectoryApi {
    @GET("company/directory")
    Observable<ApiResponse<List<Map<String, Object>>>> list();

    @GET("company/directory/{userId}")
    Observable<ApiResponse<Map<String, Object>>> detail(@Path("userId") long userId,
            @Query("taskStatus") String taskStatus, @Query("startDate") String startDate,
            @Query("endDate") String endDate);
}
