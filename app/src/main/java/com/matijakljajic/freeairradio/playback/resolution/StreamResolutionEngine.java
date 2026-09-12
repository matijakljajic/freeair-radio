package com.matijakljajic.freeairradio.playback.resolution;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.BuildConfig;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.playback.resolution.ResolutionResult.ResolutionStatus;
import com.matijakljajic.freeairradio.playback.resolution.ResolvedStreamCandidate.MetadataCapability;
import com.matijakljajic.freeairradio.playback.resolution.ResolvedStreamCandidate.StreamProtocol;
import com.matijakljajic.freeairradio.util.AppLog;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@SuppressWarnings({"unused", "GrazieInspectionRunner"})
public final class StreamResolutionEngine {

    private static final String TAG = "StreamResolutionEngine";
    private static final int MAX_DEPTH = 10;
    private static final int MAX_PREVIEW_BYTES = 64 * 1024;
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .callTimeout(8, TimeUnit.SECONDS)
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();
    @NonNull
    private final PlaylistParser playlistParser = new PlaylistParser();

    @NonNull
    public ResolutionResult resolveUrls(@NonNull String originalUrl) {
        return resolveUrlsInternal(originalUrl, buildCandidates(originalUrl), null);
    }

    @NonNull
    public ResolutionResult resolveUrls(@NonNull Station station) {
        List<String> seedUrls = new ArrayList<>();
        if (station.getResolvedStreamUrl() != null) {
            seedUrls.add(station.getResolvedStreamUrl());
        }
        seedUrls.add(station.getStreamUrl());
        return resolveUrlsInternal(station.getStreamUrl(), seedUrls, station.getName());
    }

    @NonNull
    static List<String> buildCandidates(@NonNull String streamUrl) {
        Set<String> candidates = new LinkedHashSet<>();
        if (streamUrl.startsWith("http://")) {
            candidates.add("https://" + streamUrl.substring("http://".length()));
            candidates.add(streamUrl);
        } else if (streamUrl.startsWith("https://")) {
            candidates.add(streamUrl);
            candidates.add("http://" + streamUrl.substring("https://".length()));
        } else {
            candidates.add(streamUrl);
        }
        return new ArrayList<>(candidates);
    }

    @Nullable
    static String selectBestPlaylistTarget(@NonNull List<String> targets) {
        String bestTarget = null;
        int bestScore = Integer.MIN_VALUE;
        for (String target : targets) {
            int score = scorePlaylistCandidateUrl(target);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }
        return bestTarget;
    }

    @NonNull
    private ResolutionResult resolveUrlsInternal(@NonNull String originalUrl,
                                                 @NonNull List<String> seedUrls,
                                                 @Nullable String stationName) {
        String normalizedOriginalUrl = normalizeCandidateUrl(originalUrl);
        if (normalizedOriginalUrl == null) {
            ResolutionResult invalidResult = new ResolutionResult(
                    originalUrl,
                    null,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    ResolutionStatus.FAILURE,
                    "INVALID_URL"
            );
            logResolutionStart(stationName, originalUrl, seedUrls);
            logResolutionResult(stationName, invalidResult);
            return invalidResult;
        }

        logResolutionStart(stationName, normalizedOriginalUrl, seedUrls);

        Queue<ProbeState> queue = createInitialProbeQueue(seedUrls);
        Set<String> visited = new LinkedHashSet<>();
        Map<String, CandidateTrace> tracesByUrl = new LinkedHashMap<>();
        String failureReason = null;

        while (!queue.isEmpty()) {
            if (Thread.currentThread().isInterrupted()) {
                ResolutionResult cancelledResult = buildResult(
                        originalUrl,
                        tracesByUrl,
                        failureReason,
                        ResolutionStatus.CANCELLED
                );
                logResolutionResult(stationName, cancelledResult);
                return cancelledResult;
            }

            ProbeState state = queue.remove();
            if (!visited.add(state.url)) {
                continue;
            }

            ProbeOutcome outcome = probe(state);
            if (outcome == null) {
                continue;
            }

            recordCandidateTrace(tracesByUrl, outcome.candidate, outcome.chain);
            failureReason = enqueueNextProbeStates(queue, visited, outcome, state.depth, failureReason);
        }

        ResolutionResult result = buildResult(
                originalUrl,
                tracesByUrl,
                failureReason,
                tracesByUrl.isEmpty() ? ResolutionStatus.FAILURE : ResolutionStatus.SUCCESS
        );
        logResolutionResult(stationName, result);
        return result;
    }

    private static void logResolutionStart(@Nullable String stationName,
                                           @NonNull String originalUrl,
                                           @NonNull List<String> seedUrls) {
        AppLog.d(TAG, "Resolving stream url"
                + " station=" + AppLog.stationName(stationName)
                + " originalUrl=" + AppLog.redactUrl(originalUrl)
                + " seedCount=" + seedUrls.size());
    }

    private static void logResolutionResult(@Nullable String stationName,
                                            @NonNull ResolutionResult result) {
        ResolvedStreamCandidate selectedCandidate = result.getSelectedCandidate();
        AppLog.d(TAG, "Resolved stream url"
                + " station=" + AppLog.stationName(stationName)
                + " status=" + result.getStatus()
                + " candidateCount=" + result.getCandidates().size()
                + " selectedUrl=" + AppLog.redactUrl(selectedCandidate != null ? selectedCandidate.getUrl() : null)
                + " protocol=" + (selectedCandidate != null ? selectedCandidate.getProtocol() : "<none>")
                + " metadataCapability=" + (selectedCandidate != null ? selectedCandidate.getMetadataCapability() : "<none>")
                + " selectionReason=" + (selectedCandidate != null ? selectedCandidate.getSelectionReason() : "<none>")
                + " failureReason=" + (result.getFailureReason() != null ? result.getFailureReason() : "<none>"));
    }

    @NonNull
    private static Queue<ProbeState> createInitialProbeQueue(@NonNull List<String> seedUrls) {
        Queue<ProbeState> queue = new ArrayDeque<>();
        for (String seedUrl : seedUrls) {
            enqueueCandidateVariants(queue, seedUrl, new ArrayList<>(), 0, null);
        }
        return queue;
    }

    private static void recordCandidateTrace(@NonNull Map<String, CandidateTrace> tracesByUrl,
                                             @Nullable ResolvedStreamCandidate candidate,
                                             @NonNull List<String> chain) {
        if (candidate == null) {
            return;
        }

        CandidateTrace existing = tracesByUrl.get(candidate.getUrl());
        if (existing == null || candidate.getPreferenceScore() > existing.candidate.getPreferenceScore()) {
            tracesByUrl.put(candidate.getUrl(), new CandidateTrace(candidate, chain));
        }
    }

    @Nullable
    private static String enqueueNextProbeStates(@NonNull Queue<ProbeState> queue,
                                                 @NonNull Set<String> visited,
                                                 @NonNull ProbeOutcome outcome,
                                                 int currentDepth,
                                                 @Nullable String failureReason) {
        for (String nextUrl : outcome.nextUrls) {
            if (currentDepth + 1 > MAX_DEPTH) {
                failureReason = "DEPTH_EXCEEDED";
                continue;
            }
            enqueueCandidateVariants(
                    queue,
                    nextUrl,
                    new ArrayList<>(outcome.chain),
                    currentDepth + 1,
                    visited
            );
        }
        return failureReason;
    }

    private static void enqueueCandidateVariants(@NonNull Queue<ProbeState> queue,
                                                 @NonNull String sourceUrl,
                                                 @NonNull List<String> chain,
                                                 int depth,
                                                 @Nullable Set<String> visited) {
        for (String candidateUrl : buildCandidates(sourceUrl)) {
            String normalizedUrl = normalizeCandidateUrl(candidateUrl);
            if (normalizedUrl == null) {
                continue;
            }
            if (visited != null && visited.contains(normalizedUrl)) {
                continue;
            }
            queue.add(new ProbeState(normalizedUrl, chain, depth));
        }
    }

    @Nullable
    private ProbeOutcome probe(@NonNull ProbeState state) {
        Request request = new Request.Builder()
                .url(state.url)
                .header("User-Agent", BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME)
                .header("Accept", "audio/*, application/ogg, application/vnd.apple.mpegurl, application/x-mpegURL, */*")
                .header("Icy-MetaData", "1")
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            List<String> chain = mergeChain(state.chain, collectResponseChain(response));
            String finalUrl = response.request().url().toString();
            ResponseBody peekBody = response.peekBody(MAX_PREVIEW_BYTES);
            String body = peekBody.string();
            String contentType = response.header("Content-Type");

            if (isHtml(contentType, body)) {
                return null;
            }

            if (PlaylistParser.isHlsManifest(finalUrl, contentType, body)) {
                ResolvedStreamCandidate candidate = createCandidate(
                        finalUrl,
                        contentType,
                        StreamProtocol.HLS,
                        MetadataCapability.HLS_TIMED_METADATA_POSSIBLE,
                        response,
                        body,
                        "HLS manifest"
                );
                return new ProbeOutcome(candidate, chain, new ArrayList<>());
            }

            if (PlaylistParser.isPlaylistResponse(finalUrl, contentType, body)) {
                List<String> nextUrls = buildPlaylistNextUrls(body, finalUrl);
                if (nextUrls.isEmpty()) {
                    return new ProbeOutcome(null, chain, new ArrayList<>());
                }
                return new ProbeOutcome(null, chain, nextUrls);
            }

            if (isDirectAudioResponse(finalUrl, contentType, body, response)) {
                ResolvedStreamCandidate candidate = createCandidate(
                        finalUrl,
                        contentType,
                        StreamProtocol.CONTINUOUS_HTTP,
                        determineMetadataCapability(contentType, response),
                        response,
                        body,
                        "Direct audio stream"
                );
                return new ProbeOutcome(candidate, chain, new ArrayList<>());
            }
        } catch (IOException ignored) {
            return null;
        }

        return null;
    }

    @NonNull
    private List<String> buildPlaylistNextUrls(@NonNull String body, @NonNull String finalUrl) {
        List<String> targets = playlistParser.parse(body, finalUrl);
        String preferredTarget = selectBestPlaylistTarget(targets);
        List<String> nextUrls = new ArrayList<>();
        if (preferredTarget != null) {
            nextUrls.add(preferredTarget);
        }
        for (String target : targets) {
            if (target.equals(preferredTarget) || !isAllowedScheme(target)) {
                continue;
            }
            nextUrls.add(target);
        }
        return nextUrls;
    }

    @NonNull
    private static ResolvedStreamCandidate createCandidate(@NonNull String url,
                                                           @Nullable String contentType,
                                                           @NonNull StreamProtocol protocol,
                                                           @NonNull MetadataCapability metadataCapability,
                                                           @NonNull Response response,
                                                           @NonNull String body,
                                                           @NonNull String selectionReason) {
        int preferenceScore = 1000;
        if (url.startsWith("https://")) {
            preferenceScore += 20;
        }
        if (metadataCapability == MetadataCapability.CONFIRMED_ICY) {
            preferenceScore += 25;
        } else if (metadataCapability == MetadataCapability.POSSIBLE_IN_STREAM_METADATA) {
            preferenceScore += 10;
        } else if (metadataCapability == MetadataCapability.HLS_TIMED_METADATA_POSSIBLE) {
            preferenceScore += 15;
        }

        Integer bitrateKbps = parseInteger(response.header("icy-br"));
        if (bitrateKbps != null) {
            preferenceScore += Math.min(20, Math.max(0, bitrateKbps / 64));
        }
        if (hasUsefulStationHeaders(response)) {
            preferenceScore += 5;
        }
        if (looksTokenized(url)) {
            preferenceScore -= 15;
        }

        return new ResolvedStreamCandidate(
                url,
                normalizeNullable(contentType),
                protocol,
                metadataCapability,
                parseInteger(response.header("icy-metaint")),
                normalizeNullable(response.header("icy-name")),
                normalizeNullable(response.header("icy-description")),
                normalizeNullable(response.header("icy-genre")),
                bitrateKbps,
                preferenceScore,
                selectionReason
        );
    }

    @NonNull
    private static ResolutionResult buildResult(@NonNull String originalUrl,
                                                @NonNull Map<String, CandidateTrace> tracesByUrl,
                                                @Nullable String failureReason,
                                                @NonNull ResolutionStatus status) {
        List<CandidateTrace> traces = new ArrayList<>(tracesByUrl.values());
        traces.sort(Comparator.comparingInt((CandidateTrace trace) -> trace.candidate.getPreferenceScore()).reversed());

        List<ResolvedStreamCandidate> candidates = new ArrayList<>(traces.size());
        List<String> selectedChain = new ArrayList<>();
        ResolvedStreamCandidate selectedCandidate = null;
        for (CandidateTrace trace : traces) {
            candidates.add(trace.candidate);
            if (selectedCandidate == null) {
                selectedCandidate = trace.candidate;
                selectedChain = new ArrayList<>(trace.chain);
            }
        }

        ResolutionStatus finalStatus = selectedCandidate != null ? ResolutionStatus.SUCCESS : status;
        if (selectedCandidate == null && failureReason == null && finalStatus == ResolutionStatus.FAILURE) {
            failureReason = "NO_PLAYABLE_CANDIDATE";
        }
        return new ResolutionResult(originalUrl, selectedCandidate, candidates, selectedChain, finalStatus, failureReason);
    }

    @NonNull
    private static List<String> mergeChain(@NonNull List<String> existingChain, @NonNull List<String> responseChain) {
        if (existingChain.isEmpty()) {
            return new ArrayList<>(responseChain);
        }

        List<String> merged = new ArrayList<>(existingChain);
        for (String url : responseChain) {
            if (merged.isEmpty() || !merged.get(merged.size() - 1).equals(url)) {
                merged.add(url);
            }
        }
        return merged;
    }

    @NonNull
    private static List<String> collectResponseChain(@NonNull Response response) {
        List<String> chain = new ArrayList<>();
        Response cursor = response;
        while (cursor != null) {
            chain.add(0, cursor.request().url().toString());
            cursor = cursor.priorResponse();
        }
        return chain;
    }

    private static boolean isDirectAudioResponse(@NonNull String url,
                                                 @Nullable String contentType,
                                                 @NonNull String body,
                                                 @NonNull Response response) {
        String normalizedContentType = normalizeNullable(contentType);
        if (isAudioContentType(normalizedContentType)) {
            return true;
        }

        if (response.header("icy-metaint") != null) {
            return true;
        }

        if (body.isEmpty()) {
            return true;
        }

        String normalizedBody = body.toUpperCase(Locale.ROOT);
        return !normalizedBody.contains("#EXTM3U")
                && !normalizedBody.contains("<ASX")
                && !normalizedBody.contains("<XSPF")
                && !normalizedBody.contains("<HTML");
    }

    private static boolean isHtml(@Nullable String contentType, @NonNull String body) {
        String normalizedContentType = normalizeNullable(contentType);
        if (normalizedContentType != null && normalizedContentType.contains("text/html")) {
            return true;
        }

        String trimmedBody = body.trim().toLowerCase(Locale.ROOT);
        return trimmedBody.startsWith("<html") || trimmedBody.contains("<body");
    }

    @NonNull
    private static MetadataCapability determineMetadataCapability(@Nullable String contentType,
                                                                  @NonNull Response response) {
        if (response.header("icy-metaint") != null) {
            return MetadataCapability.CONFIRMED_ICY;
        }

        if (isAudioMetadataCapableContentType(normalizeNullable(contentType))) {
            return MetadataCapability.POSSIBLE_IN_STREAM_METADATA;
        }

        return MetadataCapability.NO_METADATA_DETECTED;
    }

    private static boolean isAudioContentType(@Nullable String normalizedContentType) {
        return normalizedContentType != null
                && (normalizedContentType.startsWith("audio/")
                || normalizedContentType.contains("application/ogg")
                || normalizedContentType.contains("application/aac")
                || normalizedContentType.contains("application/mp4")
                || normalizedContentType.contains("video/mp2t"));
    }

    private static boolean isAudioMetadataCapableContentType(@Nullable String normalizedContentType) {
        return normalizedContentType != null
                && (normalizedContentType.startsWith("audio/")
                || normalizedContentType.contains("application/ogg")
                || normalizedContentType.contains("application/aac")
                || normalizedContentType.contains("application/mp4"));
    }

    private static boolean hasUsefulStationHeaders(@NonNull Response response) {
        return response.header("icy-name") != null
                || response.header("icy-description") != null
                || response.header("icy-genre") != null;
    }

    @Nullable
    private static String normalizeCandidateUrl(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return null;
        }

        try {
            URI uri = URI.create(trimmedValue);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return trimmedValue;
            }
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return null;
            }
            return uri.toString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isAllowedScheme(@Nullable String value) {
        String normalizedValue = normalizeCandidateUrl(value);
        if (normalizedValue == null) {
            return false;
        }

        try {
            URI uri = URI.create(normalizedValue);
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Nullable
    private static Integer parseInteger(@Nullable String value) {
        if (value == null) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private static String normalizeNullable(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private static boolean looksTokenized(@NonNull String url) {
        String lowerCaseUrl = url.toLowerCase(Locale.ROOT);
        return lowerCaseUrl.contains("token=")
                || lowerCaseUrl.contains("sig=")
                || lowerCaseUrl.contains("signature=")
                || lowerCaseUrl.contains("expires=")
                || lowerCaseUrl.contains("auth=")
                || lowerCaseUrl.contains("key=")
                || lowerCaseUrl.contains("session=");
    }

    private static int scorePlaylistCandidateUrl(@NonNull String url) {
        String lowerCaseUrl = url.toLowerCase(Locale.ROOT);
        int score = 0;
        if (lowerCaseUrl.contains("stream")
                || lowerCaseUrl.contains("live")
                || lowerCaseUrl.contains("radio")
                || lowerCaseUrl.contains("icecast")
                || lowerCaseUrl.contains("shoutcast")
                || lowerCaseUrl.contains("audio")
                || lowerCaseUrl.contains("mp3")
                || lowerCaseUrl.contains("aac")
                || lowerCaseUrl.contains("ogg")
                || lowerCaseUrl.contains("opus")
                || lowerCaseUrl.contains("m4a")) {
            score += 20;
        }
        if (lowerCaseUrl.contains("intro")
                || lowerCaseUrl.contains("jingle")
                || lowerCaseUrl.contains("promo")
                || lowerCaseUrl.contains("advert")
                || lowerCaseUrl.contains("podcast")
                || lowerCaseUrl.contains("announce")) {
            score -= 10;
        }
        if (lowerCaseUrl.startsWith("https://")) {
            score += 5;
        }
        if (looksTokenized(lowerCaseUrl)) {
            score -= 8;
        }
        return score;
    }

    private static final class ProbeState {
        @NonNull
        private final String url;
        @NonNull
        private final List<String> chain;
        private final int depth;

        private ProbeState(@NonNull String url, @NonNull List<String> chain, int depth) {
            this.url = url;
            this.chain = chain;
            this.depth = depth;
        }
    }

    private static final class ProbeOutcome {
        @Nullable
        private final ResolvedStreamCandidate candidate;
        @NonNull
        private final List<String> chain;
        @NonNull
        private final List<String> nextUrls;

        private ProbeOutcome(@Nullable ResolvedStreamCandidate candidate,
                             @NonNull List<String> chain,
                             @NonNull List<String> nextUrls) {
            this.candidate = candidate;
            this.chain = chain;
            this.nextUrls = nextUrls;
        }
    }

    private static final class CandidateTrace {
        @NonNull
        private final ResolvedStreamCandidate candidate;
        @NonNull
        private final List<String> chain;

        private CandidateTrace(@NonNull ResolvedStreamCandidate candidate, @NonNull List<String> chain) {
            this.candidate = candidate;
            this.chain = chain;
        }
    }
}
