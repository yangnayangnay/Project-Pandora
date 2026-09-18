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

    private static final String BASE_URL = "http://10.0.2.2:8080/api/";
    private static NetworkClient instance;
    private final Retrofit retrofit;

    private NetworkClient(Context context) {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new TokenInterceptor(context))
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
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