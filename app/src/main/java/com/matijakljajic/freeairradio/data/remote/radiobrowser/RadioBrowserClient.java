package com.matijakljajic.freeairradio.data.remote.radiobrowser;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.matijakljajic.freeairradio.BuildConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RadioBrowserClient {

    private static final RadioBrowserClient INSTANCE = new RadioBrowserClient();
    private static final Gson GSON = new GsonBuilder().create();
    private static final String APP_NAME = "FreeAirRadio";
    private static final String USER_AGENT =
            BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME;

    private final OkHttpClient okHttpClient;
    private final Map<String, RadioBrowserApi> apiCache = new HashMap<>();

    private RadioBrowserClient() {
        okHttpClient = new OkHttpClient.Builder()
                .callTimeout(8, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .addInterceptor(createHeaderInterceptor())
                .build();
    }

    @NonNull
    public static RadioBrowserClient getInstance() {
        return INSTANCE;
    }

    @NonNull
    public synchronized RadioBrowserApi create(@NonNull String baseUrl) {
        RadioBrowserApi cachedApi = apiCache.get(baseUrl);
        if (cachedApi != null) {
            return cachedApi;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(GSON))
                .build();
        RadioBrowserApi api = retrofit.create(RadioBrowserApi.class);
        apiCache.put(baseUrl, api);
        return api;
    }

    @NonNull
    private Interceptor createHeaderInterceptor() {
        return chain -> {
            Request request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("X-Application-Name", APP_NAME)
                    .header("X-Application-Version", BuildConfig.VERSION_NAME)
                    .build();
            return chain.proceed(request);
        };
    }
}
