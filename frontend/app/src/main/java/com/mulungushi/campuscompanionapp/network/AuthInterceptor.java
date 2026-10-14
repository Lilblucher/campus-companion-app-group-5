package com.mulungushi.campuscompanionapp.network;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final Context context;

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder builder = originalRequest.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");

        String token = SessionManager.getInstance(context).getAuthToken();
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        Response response = chain.proceed(builder.build());

        if (response.code() == 401 && token != null && !token.isEmpty()) {
            SessionManager.getInstance(context).clearSession();
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Intent intent = new Intent(context, Class.forName("com.mulungushi.campuscompanionapp.LoginActivity"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intent);
                } catch (ClassNotFoundException ignored) { }
            });
        }

        return response;
    }
}
