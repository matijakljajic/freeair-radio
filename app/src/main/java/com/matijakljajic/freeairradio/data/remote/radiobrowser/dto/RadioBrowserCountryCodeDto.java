package com.matijakljajic.freeairradio.data.remote.radiobrowser.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public final class RadioBrowserCountryCodeDto {

    @SerializedName("name")
    private String countryCode;

    @Nullable
    public String getCountryCode() {
        return countryCode;
    }
}
