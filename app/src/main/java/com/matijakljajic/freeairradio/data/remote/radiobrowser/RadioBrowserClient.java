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

public final class RadioBrowserClient {

    private static final String BASE_URL = "https://de1.api.radio-browser.info/";
    private static final RadioBrowserClient INSTANCE = new RadioBrowserClient();

    private final RadioBrowserApi api;

    private RadioBrowserClient() {
        Gson gson = new GsonBuilder().create();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(createHeaderInterceptor())
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        api = retrofit.create(RadioBrowserApi.class);
    }

    @NonNull
    public static RadioBrowserClient getInstance() {
        return INSTANCE;
    }

    @NonNull
    public RadioBrowserApi getApi() {
        return api;
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
