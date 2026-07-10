package com.matijakljajic.freeairradio.data.remote.radiobrowser;

import androidx.annotation.NonNull;

public interface RadioBrowserApiFactory {

    @NonNull
    RadioBrowserApi create(@NonNull String baseUrl);
}
