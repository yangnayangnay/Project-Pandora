package com.example.project_pandora.core.network;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class TokenInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    public TokenInterceptor(Context context) {
        this.tokenManager = TokenManager.getInstance(context);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String token = tokenManager.getToken();

        if (token != null) {
            Request authenticated = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
            return chain.proceed(authenticated);
        }

        return chain.proceed(original);
    }
}