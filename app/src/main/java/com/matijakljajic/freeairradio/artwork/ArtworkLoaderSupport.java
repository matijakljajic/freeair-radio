package com.matijakljajic.freeairradio.artwork;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.matijakljajic.freeairradio.BuildConfig;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

final class ArtworkLoaderSupport {

    private static final String USER_AGENT = "FreeAirRadio/" + BuildConfig.VERSION_NAME;
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();
    private static final Executor NETWORK_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private ArtworkLoaderSupport() {
    }

    @NonNull
    static String userAgent() {
        return USER_AGENT;
    }

    @NonNull
    static OkHttpClient httpClient() {
        return HTTP_CLIENT;
    }

    @NonNull
    static Executor networkExecutor() {
        return NETWORK_EXECUTOR;
    }

    static void postToMain(@NonNull Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
            return;
        }
        MAIN_HANDLER.post(runnable);
    }
}
