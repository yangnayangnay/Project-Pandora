package com.example.project_pandora.core.network;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkClient {

    private static NetworkClient instance;
    private final Retrofit retrofit;

    /** Round29: 委托 ServerUrlManager（默认双轨常量与 isEmulator() 已迁入该类），静态签名不变 */
    public static String getBaseUrl() {
        return ServerUrlManager.getBaseUrl();
    }

    private NetworkClient(Context context) {
        String baseUrl = getBaseUrl();
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        // Round28 风险R1 强制项: 透传 API Key 禁止完整泄露 logcat（BODY 级日志下必须掩码）
        loggingInterceptor.redactHeader("X-AI-Api-Key");

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                // Round29 生效机制（组2.1 方案B）: BaseUrlInterceptor 居首位应用拦截器——
                // URL 重写先行，Token 鉴权头/AI 透传头/日志均基于重写后 URL 工作；
                // 占位 baseUrl 不影响实际请求目标，真实目标由拦截器按 ServerUrlManager 决定
                .addInterceptor(new BaseUrlInterceptor(baseUrl))
                .addInterceptor(new TokenInterceptor(context))
                .addInterceptor(new AiConfigInterceptor(context))
                .addInterceptor(loggingInterceptor)
                // DEF-32-02 修复：连接建立类超时 15s→5s（校园网公网 RTT<100ms，5s 含 50 倍余量）；
                // 读/写超时保持宽松（业务响应含 AI 兜底分析耗时），重试机制不动——
                // 不可达场景下（连接失败 5s + 重试）按钮锁死窗口由 ~46s 收敛至 ≤10s 反馈可达
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }

    public static synchronized NetworkClient getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkClient(context.getApplicationContext());
        }
        return instance;
    }

    public <T> T createService(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }
}