package com.matijakljajic.freeairradio.data.remote.radiobrowser;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.matijakljajic.freeairradio.BuildConfig;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RadioBrowserClient implements RadioBrowserApiFactory {

    private static final RadioBrowserClient INSTANCE = new RadioBrowserClient();
    private static final Gson GSON = new GsonBuilder().create();

    private final OkHttpClient okHttpClient;

    private RadioBrowserClient() {
        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(createHeaderInterceptor())
                .build();
    }

    @NonNull
    public static RadioBrowserClient getInstance() {
        return INSTANCE;
    }

    @NonNull
    @Override
    public RadioBrowserApi create(@NonNull String baseUrl) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(GSON))
                .build();
        return retrofit.create(RadioBrowserApi.class);
    }

    @NonNull
    private Interceptor createHeaderInterceptor() {
        return chain -> {
            Request request = chain.request().newBuilder()
                    .header("User-Agent", BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME)
                    .header("X-Application-Name", "FreeAirRadio")
                    .header("X-Application-Version", BuildConfig.VERSION_NAME)
                    .build();
            return chain.proceed(request);
        };
    }
}
