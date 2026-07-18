package com.matijakljajic.freeairradio.ui.homepage;

import androidx.annotation.StringRes;

import com.matijakljajic.freeairradio.R;

public enum HomePageSource {
    NOW_POPULAR(R.string.station_list_title_now_popular),
    FAVORITES(R.string.station_list_title_favorites),
    LOCAL_STATIONS(R.string.station_list_title_local_stations);

    @StringRes
    private final int titleResId;

    HomePageSource(@StringRes int titleResId) {
        this.titleResId = titleResId;
    }

    @StringRes
    public int getTitleResId() {
        return titleResId;
    }
}
