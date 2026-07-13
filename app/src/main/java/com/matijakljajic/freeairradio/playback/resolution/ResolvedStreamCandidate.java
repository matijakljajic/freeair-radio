package com.matijakljajic.freeairradio.playback.resolution;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public final class ResolvedStreamCandidate {

    public enum StreamProtocol {
        CONTINUOUS_HTTP,
        HLS
    }

    public enum MetadataCapability {
        CONFIRMED_ICY,
        POSSIBLE_IN_STREAM_METADATA,
        HLS_TIMED_METADATA_POSSIBLE,
        NO_METADATA_DETECTED
    }

    private final String url;
    @Nullable
    private final String contentType;
    private final StreamProtocol protocol;
    private final boolean playable;
    private final boolean hls;
    private final MetadataCapability metadataCapability;
    @Nullable
    private final Integer icyMetaInterval;
    @Nullable
    private final String icyName;
    @Nullable
    private final String icyDescription;
    @Nullable
    private final String icyGenre;
    @Nullable
    private final Integer bitrateKbps;
    private final int preferenceScore;
    @NonNull
    private final String selectionReason;

    public ResolvedStreamCandidate(@NonNull String url,
                                   @Nullable String contentType,
                                   @NonNull StreamProtocol protocol,
                                   boolean playable,
                                   boolean hls,
                                   @NonNull MetadataCapability metadataCapability,
                                   @Nullable Integer icyMetaInterval,
                                   @Nullable String icyName,
                                   @Nullable String icyDescription,
                                   @Nullable String icyGenre,
                                   @Nullable Integer bitrateKbps,
                                   int preferenceScore,
                                   @NonNull String selectionReason) {
        this.url = url;
        this.contentType = contentType;
        this.protocol = protocol;
        this.playable = playable;
        this.hls = hls;
        this.metadataCapability = metadataCapability;
        this.icyMetaInterval = icyMetaInterval;
        this.icyName = icyName;
        this.icyDescription = icyDescription;
        this.icyGenre = icyGenre;
        this.bitrateKbps = bitrateKbps;
        this.preferenceScore = preferenceScore;
        this.selectionReason = selectionReason;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    @Nullable
    public String getContentType() {
        return contentType;
    }

    @NonNull
    public StreamProtocol getProtocol() {
        return protocol;
    }

    public boolean isPlayable() {
        return playable;
    }

    public boolean isHls() {
        return hls;
    }

    @NonNull
    public MetadataCapability getMetadataCapability() {
        return metadataCapability;
    }

    @Nullable
    public Integer getIcyMetaInterval() {
        return icyMetaInterval;
    }

    @Nullable
    public String getIcyName() {
        return icyName;
    }

    @Nullable
    public String getIcyDescription() {
        return icyDescription;
    }

    @Nullable
    public String getIcyGenre() {
        return icyGenre;
    }

    @Nullable
    public Integer getBitrateKbps() {
        return bitrateKbps;
    }

    public int getPreferenceScore() {
        return preferenceScore;
    }

    @NonNull
    public String getSelectionReason() {
        return selectionReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResolvedStreamCandidate)) {
            return false;
        }
        ResolvedStreamCandidate that = (ResolvedStreamCandidate) o;
        return playable == that.playable
                && hls == that.hls
                && preferenceScore == that.preferenceScore
                && url.equals(that.url)
                && Objects.equals(contentType, that.contentType)
                && protocol == that.protocol
                && metadataCapability == that.metadataCapability
                && Objects.equals(icyMetaInterval, that.icyMetaInterval)
                && Objects.equals(icyName, that.icyName)
                && Objects.equals(icyDescription, that.icyDescription)
                && Objects.equals(icyGenre, that.icyGenre)
                && Objects.equals(bitrateKbps, that.bitrateKbps)
                && selectionReason.equals(that.selectionReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, contentType, protocol, playable, hls, metadataCapability, icyMetaInterval, icyName, icyDescription, icyGenre, bitrateKbps, preferenceScore, selectionReason);
    }

    @NonNull
    @Override
    public String toString() {
        return "ResolvedStreamCandidate{"
                + "url='" + url + '\''
                + ", contentType='" + contentType + '\''
                + ", protocol=" + protocol
                + ", playable=" + playable
                + ", hls=" + hls
                + ", metadataCapability=" + metadataCapability
                + ", icyMetaInterval=" + icyMetaInterval
                + ", icyName='" + icyName + '\''
                + ", icyDescription='" + icyDescription + '\''
                + ", icyGenre='" + icyGenre + '\''
                + ", bitrateKbps=" + bitrateKbps
                + ", preferenceScore=" + preferenceScore
                + ", selectionReason='" + selectionReason + '\''
                + '}';
    }
}
