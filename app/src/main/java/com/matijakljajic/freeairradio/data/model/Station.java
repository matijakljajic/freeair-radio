package com.matijakljajic.freeairradio.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

// TODO: replace Serializable with Parcelable and @Parcelize as that's more ideal
public class Station implements Serializable {

    public static final String UNKNOWN = "UNKNOWN";

    private final String id;
    private final String name;
    private final String streamUrl;
    @Nullable
    private final String resolvedStreamUrl;
    @Nullable
    private final String homepage;
    @Nullable
    private final String favicon;
    private final String country;
    private final String countryCode;
    private final String language;
    private final String tags;
    private final String codec;
    private final int bitrate;
    @Nullable
    private final Boolean hls;
    private final StationOrigin origin;

    private Station(@NonNull Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.name = Objects.requireNonNull(builder.name, "name");
        this.streamUrl = Objects.requireNonNull(builder.streamUrl, "streamUrl");
        this.resolvedStreamUrl = normalizeNullable(builder.resolvedStreamUrl);
        this.homepage = normalizeNullable(builder.homepage);
        this.favicon = normalizeNullable(builder.favicon);
        this.country = normalizeUnknown(builder.country);
        this.countryCode = normalizeUnknown(builder.countryCode);
        this.language = normalizeUnknown(builder.language);
        this.tags = normalizeUnknown(builder.tags);
        this.codec = normalizeUnknown(builder.codec);
        this.bitrate = builder.bitrate;
        this.hls = builder.hls;
        this.origin = Objects.requireNonNull(builder.origin, "origin");
    }

    @NonNull
    public static Builder builder(@NonNull String id,
                                  @NonNull String name,
                                  @NonNull String streamUrl,
                                  @NonNull StationOrigin origin) {
        return new Builder(id, name, streamUrl, origin);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    @Nullable
    public String getResolvedStreamUrl() {
        return resolvedStreamUrl;
    }

    @Nullable
    public String getHomepage() {
        return homepage;
    }

    public boolean hasHomepage() {
        return homepage != null;
    }

    @Nullable
    public String getFavicon() {
        return favicon;
    }

    @NonNull
    public String getPlayableStreamUrl() {
        return resolvedStreamUrl != null ? resolvedStreamUrl : streamUrl;
    }

    public String getCountry() {
        return country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getLanguage() {
        return language;
    }

    public String getTags() {
        return tags;
    }

    public String getCodec() {
        return codec;
    }

    public int getBitrate() {
        return bitrate;
    }

    @Nullable
    public Boolean getHls() {
        return hls;
    }

    public StationOrigin getOrigin() {
        return origin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Station)) {
            return false;
        }
        Station station = (Station) o;
        return bitrate == station.bitrate
                && id.equals(station.id)
                && name.equals(station.name)
                && streamUrl.equals(station.streamUrl)
                && Objects.equals(resolvedStreamUrl, station.resolvedStreamUrl)
                && Objects.equals(homepage, station.homepage)
                && Objects.equals(favicon, station.favicon)
                && country.equals(station.country)
                && countryCode.equals(station.countryCode)
                && language.equals(station.language)
                && tags.equals(station.tags)
                && codec.equals(station.codec)
                && Objects.equals(hls, station.hls)
                && origin == station.origin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, streamUrl, resolvedStreamUrl, homepage, favicon, country, countryCode, language, tags, codec, bitrate, hls, origin);
    }

    @NonNull
    @Override
    public String toString() {
        return "Station{" + "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", streamUrl='" + streamUrl + '\'' +
                ", resolvedStreamUrl='" + resolvedStreamUrl + '\'' +
                ", homepage='" + homepage + '\'' +
                ", favicon='" + favicon + '\'' +
                ", country='" + country + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", language='" + language + '\'' +
                ", tags='" + tags + '\'' +
                ", codec='" + codec + '\'' +
                ", bitrate=" + bitrate +
                ", hls=" + hls +
                ", origin=" + origin +
                '}';
    }

    @Nullable
    private static String normalizeNullable(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    @NonNull
    private static String normalizeUnknown(@Nullable String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return UNKNOWN;
        }

        if (normalized.toLowerCase(Locale.ROOT).contains("unknown")) {
            return UNKNOWN;
        }

        return normalized;
    }

    public static final class Builder {
        private final String id;
        private final String name;
        private final String streamUrl;
        private final StationOrigin origin;
        @Nullable
        private String resolvedStreamUrl;
        @Nullable
        private String homepage;
        @Nullable
        private String favicon;
        @Nullable
        private String country;
        @Nullable
        private String countryCode;
        @Nullable
        private String language;
        @Nullable
        private String tags;
        @Nullable
        private String codec;
        private int bitrate;
        @Nullable
        private Boolean hls;

        private Builder(@NonNull String id,
                        @NonNull String name,
                        @NonNull String streamUrl,
                        @NonNull StationOrigin origin) {
            this.id = id;
            this.name = name;
            this.streamUrl = streamUrl;
            this.origin = origin;
        }

        @NonNull
        public Builder setResolvedStreamUrl(@Nullable String resolvedStreamUrl) {
            this.resolvedStreamUrl = resolvedStreamUrl;
            return this;
        }

        @NonNull
        public Builder setHomepage(@Nullable String homepage) {
            this.homepage = homepage;
            return this;
        }

        @NonNull
        public Builder setFavicon(@Nullable String favicon) {
            this.favicon = favicon;
            return this;
        }

        @NonNull
        public Builder setCountry(@Nullable String country) {
            this.country = country;
            return this;
        }

        @NonNull
        public Builder setCountryCode(@Nullable String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        @NonNull
        public Builder setLanguage(@Nullable String language) {
            this.language = language;
            return this;
        }

        @NonNull
        public Builder setTags(@Nullable String tags) {
            this.tags = tags;
            return this;
        }

        @NonNull
        public Builder setCodec(@Nullable String codec) {
            this.codec = codec;
            return this;
        }

        @NonNull
        public Builder setBitrate(int bitrate) {
            this.bitrate = bitrate;
            return this;
        }

        @NonNull
        public Builder setHls(@Nullable Boolean hls) {
            this.hls = hls;
            return this;
        }

        @NonNull
        public Station build() {
            return new Station(this);
        }
    }
}
