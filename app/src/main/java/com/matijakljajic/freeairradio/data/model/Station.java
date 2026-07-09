package com.matijakljajic.freeairradio.data.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

// TODO: replace Serializable with Parcelable and @Parcelize as that's more ideal
public class Station implements Serializable {

    public static final String UNKNOWN = "UNKNOWN";

    private final String id;
    private final String name;
    private final String streamUrl;
    private final String country;
    private final String language;
    private final String tags;
    private final String codec;
    private final int bitrate;
    private final StationOrigin origin;

    public Station(String id, String name, String streamUrl, StationOrigin origin) {
        this(id, name, streamUrl, "", "", "", "", 0, origin);
    }

    public Station(String id, String name, String streamUrl, String country, StationOrigin origin) {
        this(id, name, streamUrl, country, "", "", "", 0, origin);
    }

    public Station(
            String id,
            String name,
            String streamUrl,
            String country,
            String language,
            StationOrigin origin
    ) {
        this(id, name, streamUrl, country, language, "", "", 0, origin);
    }

    public Station(
            String id,
            String name,
            String streamUrl,
            String country,
            String language,
            String tags,
            StationOrigin origin
    ) {
        this(id, name, streamUrl, country, language, tags, "", 0, origin);
    }

    public Station(
            String id,
            String name,
            String streamUrl,
            String country,
            String language,
            String tags,
            String codec,
            StationOrigin origin
    ) {
        this(id, name, streamUrl, country, language, tags, codec, 0, origin);
    }

    public Station(
            String id,
            String name,
            String streamUrl,
            String country,
            String language,
            String tags,
            String codec,
            int bitrate,
            StationOrigin origin
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.streamUrl = Objects.requireNonNull(streamUrl, "streamUrl");
        this.country = normalizeUnknown(country);
        this.language = normalizeUnknown(language);
        this.tags = normalizeUnknown(tags);
        this.codec = normalizeUnknown(codec);
        this.bitrate = bitrate;
        this.origin = Objects.requireNonNull(origin, "origin");
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

    public String getCountry() {
        return country;
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
                && country.equals(station.country)
                && language.equals(station.language)
                && tags.equals(station.tags)
                && codec.equals(station.codec)
                && origin == station.origin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, streamUrl, country, language, tags, codec, bitrate, origin);
    }

    @NonNull
    @Override
    public String toString() {
        return "Station{" + "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", streamUrl='" + streamUrl + '\'' +
                ", country='" + country + '\'' +
                ", language='" + language + '\'' +
                ", tags='" + tags + '\'' +
                ", codec='" + codec + '\'' +
                ", bitrate=" + bitrate +
                ", origin=" + origin +
                '}';
    }

    private String normalizeUnknown(String value) {
        if (value == null) {
            return UNKNOWN;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return UNKNOWN;
        }

        if (trimmedValue.toLowerCase(Locale.ROOT).contains("unknown")) {
            return UNKNOWN;
        }

        return trimmedValue;
    }
}
