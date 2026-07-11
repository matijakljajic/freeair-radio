package com.matijakljajic.freeairradio.playback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.BuildConfig;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.playback.resolution.ResolutionResult;
import com.matijakljajic.freeairradio.playback.resolution.ResolutionResult.ResolutionStatus;
import com.matijakljajic.freeairradio.playback.resolution.ResolvedStreamCandidate;
import com.matijakljajic.freeairradio.playback.resolution.ResolvedStreamCandidate.MetadataCapability;
import com.matijakljajic.freeairradio.playback.resolution.ResolvedStreamCandidate.StreamProtocol;

import java.io.IOException;
import java.io.StringReader;
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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@SuppressWarnings("GrazieInspectionRunner")
public final class StreamResolutionEngine {

    private static final int MAX_DEPTH = 10;
    private static final int MAX_PREVIEW_BYTES = 64 * 1024;
    private static final int MAX_PLAYLIST_ENTRIES = 32;
    private static final int MAX_LINE_LENGTH = 4096;
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .callTimeout(8, TimeUnit.SECONDS)
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    @NonNull
    public ResolutionResult resolveUrls(@NonNull String originalUrl) {
        return resolveUrlsInternal(originalUrl, buildCandidates(originalUrl));
    }

    @NonNull
    public ResolutionResult resolveUrls(@NonNull Station station) {
        List<String> seedUrls = new ArrayList<>();
        if (station.getResolvedStreamUrl() != null) {
            seedUrls.add(station.getResolvedStreamUrl());
        }
        seedUrls.add(station.getStreamUrl());
        return resolveUrlsInternal(station.getStreamUrl(), seedUrls);
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

    static boolean isPlaylistUrl(@NonNull String url) {
        String lowerCaseUrl = url.toLowerCase(Locale.ROOT);
        return lowerCaseUrl.endsWith(".m3u")
                || lowerCaseUrl.endsWith(".m3u8")
                || lowerCaseUrl.endsWith(".pls")
                || lowerCaseUrl.endsWith(".xspf")
                || lowerCaseUrl.endsWith(".asx");
    }

    @Nullable
    static String extractPlaylistTarget(@NonNull String body, @NonNull String baseUrl) {
        List<String> targets = extractPlaylistTargets(body, baseUrl);
        return selectBestPlaylistTarget(targets);
    }

    static boolean isHlsManifest(@NonNull String url,
                                 @Nullable String contentType,
                                 @NonNull String body) {
        String normalizedContentType = normalizeNullable(contentType);
        if (normalizedContentType != null) {
            if (normalizedContentType.contains("mpegurl")
                    || normalizedContentType.contains("vnd.apple.mpegurl")
                    || normalizedContentType.contains("x-mpegurl")) {
                return true;
            }
        }

        String normalizedBody = body.toUpperCase(Locale.ROOT);
        return normalizedBody.contains("#EXT-X-STREAM-INF")
                || normalizedBody.contains("#EXT-X-TARGETDURATION")
                || normalizedBody.contains("#EXT-X-MEDIA-SEQUENCE")
                || normalizedBody.contains("#EXT-X-VERSION")
                || url.toLowerCase(Locale.ROOT).endsWith(".m3u8");
    }

    static boolean isPlaylistResponse(@NonNull String url,
                                      @Nullable String contentType,
                                      @NonNull String body) {
        if (isHlsManifest(url, contentType, body)) {
            return false;
        }

        if (isPlaylistUrl(url)) {
            return true;
        }

        String normalizedContentType = normalizeNullable(contentType);
        if (normalizedContentType != null) {
            if (normalizedContentType.contains("scpls")
                    || normalizedContentType.contains("mpegurl")
                    || normalizedContentType.contains("xspf")
                    || normalizedContentType.contains("playlist")
                    || normalizedContentType.contains("xml")) {
                return true;
            }
        }

        String normalizedBody = body.toUpperCase(Locale.ROOT);
        return normalizedBody.contains("#EXTM3U")
                || normalizedBody.contains("[PLAYLIST]")
                || normalizedBody.contains("<ASX")
                || normalizedBody.contains("<XSPF")
                || normalizedBody.contains("<LOCATION")
                || normalizedBody.contains("<REF ")
                || normalizedBody.contains("FILE1=")
                || normalizedBody.contains("URL1=");
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
    static List<String> extractPlaylistTargets(@NonNull String body, @NonNull String baseUrl) {
        List<String> targets = new ArrayList<>();
        String trimmedBody = body.trim();
        if (trimmedBody.isEmpty()) {
            return targets;
        }

        if (looksLikeXmlPlaylist(trimmedBody)) {
            targets.addAll(extractXmlPlaylistTargets(trimmedBody, baseUrl));
            return dedupeUrls(targets);
        }

        String[] lines = trimmedBody.split("\\r?\\n");
        for (String line : lines) {
            if (targets.size() >= MAX_PLAYLIST_ENTRIES) {
                break;
            }
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.length() > MAX_LINE_LENGTH || trimmedLine.startsWith("#")) {
                continue;
            }

            if (trimmedLine.regionMatches(true, 0, "File", 0, 4) && trimmedLine.contains("=")) {
                String value = trimmedLine.substring(trimmedLine.indexOf('=') + 1).trim();
                addResolvedUrl(targets, baseUrl, value);
                continue;
            }

            if (looksLikeUrl(trimmedLine)) {
                addResolvedUrl(targets, baseUrl, trimmedLine);
            }
        }

        if (targets.isEmpty() && looksLikeUrl(trimmedBody)) {
            addResolvedUrl(targets, baseUrl, trimmedBody);
        }
        return dedupeUrls(targets);
    }

    @NonNull
    private ResolutionResult resolveUrlsInternal(@NonNull String originalUrl, @NonNull List<String> seedUrls) {
        String normalizedOriginalUrl = normalizeCandidateUrl(originalUrl);
        if (normalizedOriginalUrl == null) {
            return new ResolutionResult(originalUrl, null, new ArrayList<>(), new ArrayList<>(), ResolutionStatus.FAILURE, "INVALID_URL");
        }

        Queue<ProbeState> queue = new ArrayDeque<>();
        for (String seedUrl : seedUrls) {
            for (String candidateSeedUrl : buildCandidates(seedUrl)) {
                String normalizedSeedUrl = normalizeCandidateUrl(candidateSeedUrl);
                if (normalizedSeedUrl != null) {
                    queue.add(new ProbeState(normalizedSeedUrl, new ArrayList<>(), 0));
                }
            }
        }

        Set<String> visited = new LinkedHashSet<>();
        Map<String, CandidateTrace> tracesByUrl = new LinkedHashMap<>();
        String failureReason = null;

        while (!queue.isEmpty()) {
            if (Thread.currentThread().isInterrupted()) {
                return buildResult(originalUrl, tracesByUrl, failureReason, ResolutionStatus.CANCELLED);
            }

            ProbeState state = queue.remove();
            if (!visited.add(state.url)) {
                continue;
            }

            ProbeOutcome outcome = probe(state);
            if (outcome == null) {
                continue;
            }

            if (outcome.candidate != null) {
                CandidateTrace existing = tracesByUrl.get(outcome.candidate.getUrl());
                if (existing == null || outcome.candidate.getPreferenceScore() > existing.candidate.getPreferenceScore()) {
                    tracesByUrl.put(outcome.candidate.getUrl(), new CandidateTrace(outcome.candidate, outcome.chain));
                }
            }

            for (String nextUrl : outcome.nextUrls) {
                if (state.depth + 1 > MAX_DEPTH) {
                    failureReason = "DEPTH_EXCEEDED";
                    continue;
                }
                for (String candidateNextUrl : buildCandidates(nextUrl)) {
                    String normalizedNextUrl = normalizeCandidateUrl(candidateNextUrl);
                    if (normalizedNextUrl == null || visited.contains(normalizedNextUrl)) {
                        continue;
                    }
                    List<String> nextChain = new ArrayList<>(outcome.chain);
                    queue.add(new ProbeState(normalizedNextUrl, nextChain, state.depth + 1));
                }
            }
        }

        return buildResult(originalUrl, tracesByUrl, failureReason, tracesByUrl.isEmpty() ? ResolutionStatus.FAILURE : ResolutionStatus.SUCCESS);
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

            if (isHlsManifest(finalUrl, contentType, body)) {
                ResolvedStreamCandidate candidate = createCandidate(
                        finalUrl,
                        contentType,
                        StreamProtocol.HLS,
                        true,
                        MetadataCapability.HLS_TIMED_METADATA_POSSIBLE,
                        response,
                        body,
                        "HLS manifest"
                );
                return new ProbeOutcome(candidate, chain, new ArrayList<>());
            }

            if (isPlaylistResponse(finalUrl, contentType, body)) {
                List<String> targets = extractPlaylistTargets(body, finalUrl);
                String bestTarget = selectBestPlaylistTarget(targets);
                List<String> nextUrls = new ArrayList<>();
                if (bestTarget != null) {
                    nextUrls.add(bestTarget);
                }
                for (String target : targets) {
                    if (target.equals(bestTarget)) {
                        continue;
                    }
                    if (!isAllowedScheme(target)) {
                        continue;
                    }
                    nextUrls.add(target);
                }
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
                        false,
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
    private static ResolvedStreamCandidate createCandidate(@NonNull String url,
                                                           @Nullable String contentType,
                                                           @NonNull StreamProtocol protocol,
                                                           boolean hls,
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
                true,
                hls,
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
        if (normalizedContentType != null) {
            if (normalizedContentType.startsWith("audio/")
                    || normalizedContentType.contains("application/ogg")
                    || normalizedContentType.contains("application/aac")
                    || normalizedContentType.contains("application/mp4")
                    || normalizedContentType.contains("video/mp2t")) {
                return true;
            }
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
    static List<String> extractPlaylistTargetsFromXml(@NonNull String body, @NonNull String baseUrl) {
        return dedupeUrls(extractXmlPlaylistTargets(body, baseUrl));
    }

    @NonNull
    private static List<String> extractXmlPlaylistTargets(@NonNull String body, @NonNull String baseUrl) {
        List<String> targets = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
            collectXmlTargets(document.getDocumentElement(), baseUrl, targets);
        } catch (ParserConfigurationException | IOException | org.xml.sax.SAXException ignored) {
            return targets;
        }
        return targets;
    }

    private static void collectXmlTargets(@Nullable Node node,
                                         @NonNull String baseUrl,
                                         @NonNull List<String> targets) {
        if (node == null || targets.size() >= MAX_PLAYLIST_ENTRIES) {
            return;
        }

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            String nodeName = element.getNodeName().toLowerCase(Locale.ROOT);
            if ("location".equals(nodeName) || "ref".equals(nodeName)) {
                String value;
                if (element.hasAttribute("href")) {
                    value = element.getAttribute("href");
                } else {
                    value = element.getTextContent();
                }
                addResolvedUrl(targets, baseUrl, value);
            }
        }

        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            collectXmlTargets(childNodes.item(i), baseUrl, targets);
        }
    }

    private static void addResolvedUrl(@NonNull List<String> targets, @NonNull String baseUrl, @Nullable String value) {
        String normalizedValue = normalizeCandidateUrl(value);
        if (normalizedValue == null) {
            return;
        }

        String resolved = resolveAgainstBase(baseUrl, normalizedValue);
        if (resolved != null) {
            targets.add(resolved);
        }
    }

    @Nullable
    private static String resolveAgainstBase(@NonNull String baseUrl, @NonNull String value) {
        try {
            URI uri = URI.create(value);
            if (uri.isAbsolute()) {
                return uri.toString();
            }
            return URI.create(baseUrl).resolve(uri).toString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @NonNull
    private static List<String> dedupeUrls(@NonNull List<String> urls) {
        return new ArrayList<>(new LinkedHashSet<>(urls));
    }

    @NonNull
    private static MetadataCapability determineMetadataCapability(@Nullable String contentType,
                                                                  @NonNull Response response) {
        if (response.header("icy-metaint") != null) {
            return MetadataCapability.CONFIRMED_ICY;
        }

        String normalizedContentType = normalizeNullable(contentType);
        if (normalizedContentType != null) {
            if (normalizedContentType.contains("audio/")
                    || normalizedContentType.contains("application/ogg")
                    || normalizedContentType.contains("application/aac")
                    || normalizedContentType.contains("application/mp4")) {
                return MetadataCapability.POSSIBLE_IN_STREAM_METADATA;
            }
        }

        return MetadataCapability.NO_METADATA_DETECTED;
    }

    private static boolean hasUsefulStationHeaders(@NonNull Response response) {
        return response.header("icy-name") != null
                || response.header("icy-description") != null
                || response.header("icy-genre") != null;
    }

    private static boolean looksLikeXmlPlaylist(@NonNull String body) {
        String normalizedBody = body.toUpperCase(Locale.ROOT);
        return normalizedBody.contains("<XSPF")
                || normalizedBody.contains("<ASX")
                || normalizedBody.contains("<LOCATION")
                || normalizedBody.contains("<REF ");
    }

    private static boolean looksLikeUrl(@NonNull String value) {
        return value.startsWith("http://") || value.startsWith("https://");
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
