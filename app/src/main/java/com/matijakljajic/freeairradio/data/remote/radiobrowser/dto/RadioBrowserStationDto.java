package com.matijakljajic.freeairradio.data.remote.radiobrowser.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public final class RadioBrowserStationDto {

    @SuppressWarnings("GrazieInspectionRunner")
    @SerializedName("stationuuid")
    private String stationUuid;

    @SerializedName("name")
    private String name;

    @SerializedName("url")
    private String url;

    @SerializedName("url_resolved")
    private String urlResolved;

    @SerializedName("homepage")
    private String homepage;

    @SerializedName("favicon")
    private String favicon;

    @SerializedName("country")
    private String country;

    @SuppressWarnings("GrazieInspectionRunner")
    @SerializedName("countrycode")
    private String countryCode;

    @SerializedName("language")
    private String language;

    @SerializedName("tags")
    private String tags;

    @SerializedName("codec")
    private String codec;

    @SerializedName("bitrate")
    private int bitrate;

    @SerializedName("clickcount")
    private int clickCount;

    @SerializedName("hls")
    private String hls;

    @Nullable
    public String getStationUuid() {
        return stationUuid;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    @Nullable
    public String getUrlResolved() {
        return urlResolved;
    }

    @Nullable
    public String getHomepage() {
        return homepage;
    }

    @Nullable
    public String getFavicon() {
        return favicon;
    }

    @Nullable
    public String getCountry() {
        return country;
    }

    @Nullable
    public String getCountryCode() {
        return countryCode;
    }

    @Nullable
    public String getLanguage() {
        return language;
    }

    @Nullable
    public String getTags() {
        return tags;
    }

    @Nullable
    public String getCodec() {
        return codec;
    }

    public int getBitrate() {
        return bitrate;
    }

    public int getClickCount() {
        return clickCount;
    }

    @Nullable
    public String getHls() {
        return hls;
    }
}
