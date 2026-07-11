package com.matijakljajic.freeairradio.playback.resolution;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ResolutionResult {

    public enum ResolutionStatus {
        SUCCESS,
        PARTIAL,
        FAILURE,
        CANCELLED
    }

    private final String originalUrl;
    @Nullable
    private final ResolvedStreamCandidate selectedCandidate;
    @NonNull
    private final List<ResolvedStreamCandidate> candidates;
    @NonNull
    private final List<String> resolutionChain;
    @NonNull
    private final ResolutionStatus status;
    @Nullable
    private final String failureReason;

    public ResolutionResult(@NonNull String originalUrl,
                            @Nullable ResolvedStreamCandidate selectedCandidate,
                            @NonNull List<ResolvedStreamCandidate> candidates,
                            @NonNull List<String> resolutionChain,
                            @NonNull ResolutionStatus status,
                            @Nullable String failureReason) {
        this.originalUrl = originalUrl;
        this.selectedCandidate = selectedCandidate;
        this.candidates = Collections.unmodifiableList(candidates);
        this.resolutionChain = Collections.unmodifiableList(resolutionChain);
        this.status = status;
        this.failureReason = failureReason;
    }

    @NonNull
    public String getOriginalUrl() {
        return originalUrl;
    }

    @Nullable
    public ResolvedStreamCandidate getSelectedCandidate() {
        return selectedCandidate;
    }

    @NonNull
    public List<ResolvedStreamCandidate> getCandidates() {
        return candidates;
    }

    @NonNull
    public List<String> getResolutionChain() {
        return resolutionChain;
    }

    @NonNull
    public ResolutionStatus getStatus() {
        return status;
    }

    @Nullable
    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResolutionResult)) {
            return false;
        }
        ResolutionResult that = (ResolutionResult) o;
        return originalUrl.equals(that.originalUrl)
                && Objects.equals(selectedCandidate, that.selectedCandidate)
                && candidates.equals(that.candidates)
                && resolutionChain.equals(that.resolutionChain)
                && status == that.status
                && Objects.equals(failureReason, that.failureReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalUrl, selectedCandidate, candidates, resolutionChain, status, failureReason);
    }

    @NonNull
    @Override
    public String toString() {
        return "ResolutionResult{"
                + "originalUrl='" + originalUrl + '\''
                + ", selectedCandidate=" + selectedCandidate
                + ", candidates=" + candidates
                + ", resolutionChain=" + resolutionChain
                + ", status=" + status
                + ", failureReason='" + failureReason + '\''
                + '}';
    }
}
