package com.matijakljajic.freeairradio.artwork;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.data.model.Station;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class StationArtworkResolver {

    private static final int MAX_HTML_BYTES = 256 * 1024;
    private static final int MAX_MANIFEST_BYTES = 128 * 1024;
    private static final Pattern LINK_TAG_PATTERN =
            Pattern.compile("<link\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_TAG_PATTERN =
            Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))"
    );
    private static final String[] COMMON_MANIFEST_PATHS = {
            "/site.webmanifest",
            "/manifest.webmanifest",
            "/manifest.json"
    };
    private static final Map<String, List<String>> RESOLVED_URL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> WORKING_URL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> SVG_URL_FLAGS = new ConcurrentHashMap<>();
    private static final Map<String, List<ResolutionCallback>> PENDING_CALLBACKS =
            new ConcurrentHashMap<>();

    private StationArtworkResolver() {
    }

    public interface ResolutionCallback {
        void onResolved(@NonNull List<String> artworkUrls);
    }

    @NonNull
    public static List<String> getBestAvailableUrls(@Nullable Station station) {
        if (station == null) {
            return Collections.emptyList();
        }

        String cacheKey = buildCacheKey(station);
        List<String> artworkUrls = RESOLVED_URL_CACHE.get(cacheKey);
        if (artworkUrls == null) {
            artworkUrls = buildFallbackUrls(station);
        }
        return prependWorkingUrl(cacheKey, artworkUrls);
    }

    @Nullable
    public static String getBestAvailableUrl(@Nullable Station station) {
        List<String> urls = getBestAvailableUrls(station);
        return urls.isEmpty() ? null : urls.get(0);
    }

    @Nullable
    public static String getBestRasterUrl(@Nullable Station station) {
        if (station == null) {
            return null;
        }

        for (String url : getBestAvailableUrls(station)) {
            if (!isSvgUrl(url)) {
                return url;
            }
        }
        return null;
    }

    public static boolean isSvgUrl(@Nullable String url) {
        if (url == null) {
            return false;
        }
        Boolean cachedFlag = SVG_URL_FLAGS.get(url);
        if (cachedFlag != null) {
            return cachedFlag;
        }
        return url.toLowerCase(Locale.ROOT).contains(".svg");
    }

    public static boolean hasResolvedCandidates(@Nullable Station station) {
        if (station == null) {
            return false;
        }
        return RESOLVED_URL_CACHE.containsKey(buildCacheKey(station));
    }

    public static boolean hasHomepageSource(@Nullable Station station) {
        return station != null && normalizeRemoteUrl(station.getHomepage()) != null;
    }

    public static void markLoadSucceeded(@NonNull Station station, @Nullable String url) {
        String normalizedUrl = normalizeRemoteUrl(url);
        if (normalizedUrl == null) {
            return;
        }

        String cacheKey = buildCacheKey(station);
        WORKING_URL_CACHE.put(cacheKey, normalizedUrl);

        List<String> cachedUrls = RESOLVED_URL_CACHE.get(cacheKey);
        if (cachedUrls == null || cachedUrls.isEmpty()) {
            return;
        }

        List<String> reorderedUrls = new ArrayList<>(cachedUrls.size());
        reorderedUrls.add(normalizedUrl);
        for (String cachedUrl : cachedUrls) {
            if (!normalizedUrl.equals(cachedUrl)) {
                reorderedUrls.add(cachedUrl);
            }
        }
        RESOLVED_URL_CACHE.put(cacheKey, Collections.unmodifiableList(reorderedUrls));
    }

    public static void resolve(@Nullable Station station,
                               @NonNull ResolutionCallback callback) {
        if (station == null) {
            postResolved(callback, Collections.emptyList());
            return;
        }

        String cacheKey = buildCacheKey(station);
        List<String> cachedUrls = RESOLVED_URL_CACHE.get(cacheKey);
        if (cachedUrls != null) {
            postResolved(callback, prependWorkingUrl(cacheKey, cachedUrls));
            return;
        }

        synchronized (PENDING_CALLBACKS) {
            List<ResolutionCallback> callbacks = PENDING_CALLBACKS.get(cacheKey);
            if (callbacks != null) {
                callbacks.add(callback);
                return;
            }

            callbacks = new ArrayList<>();
            callbacks.add(callback);
            PENDING_CALLBACKS.put(cacheKey, callbacks);
        }

        ArtworkLoaderSupport.networkExecutor().execute(() -> resolveInBackground(station, cacheKey));
    }

    private static void resolveInBackground(@NonNull Station station, @NonNull String cacheKey) {
        List<String> resolvedUrls = resolveCandidateUrls(station);
        if (resolvedUrls.isEmpty()) {
            resolvedUrls = buildFallbackUrls(station);
        }

        List<String> cachedUrls = List.copyOf(resolvedUrls);
        RESOLVED_URL_CACHE.put(cacheKey, cachedUrls);

        List<ResolutionCallback> callbacks;
        synchronized (PENDING_CALLBACKS) {
            callbacks = PENDING_CALLBACKS.remove(cacheKey);
        }
        if (callbacks == null) {
            return;
        }

        List<String> deliveredUrls = prependWorkingUrl(cacheKey, cachedUrls);
        for (ResolutionCallback callback : callbacks) {
            postResolved(callback, deliveredUrls);
        }
    }

    @NonNull
    private static List<String> resolveCandidateUrls(@NonNull Station station) {
        List<ArtworkCandidate> candidates = new ArrayList<>();
        int order = 0;

        String faviconUrl = normalizeRemoteUrl(station.getFavicon());
        if (faviconUrl != null) {
            order = addCandidate(candidates, faviconUrl, 0, false, 2, order, isSvg(faviconUrl));
        }

        String homepageUrl = normalizeRemoteUrl(station.getHomepage());
        if (homepageUrl == null) {
            return deduplicateAndRank(candidates);
        }

        HttpUrl baseUrl = HttpUrl.parse(homepageUrl);
        if (baseUrl == null) {
            return deduplicateAndRank(candidates);
        }

        String html = fetchText(baseUrl, MAX_HTML_BYTES);
        if (html != null) {
            int homepageCandidateCountBefore = candidates.size();
            order = collectHomepageCandidates(html, baseUrl, candidates, order);
            order = collectMetaArtworkCandidates(html, baseUrl, candidates, order);
            if (candidates.size() == homepageCandidateCountBefore) {
                order = collectCommonManifestCandidates(baseUrl, candidates, order);
            }
        } else {
            order = collectCommonManifestCandidates(baseUrl, candidates, order);
        }

        HttpUrl faviconIcoUrl = baseUrl.resolve("/favicon.ico");
        if (faviconIcoUrl != null) {
            addCandidate(candidates, faviconIcoUrl.toString(), 16, true, 0, order, false);
        }

        return deduplicateAndRank(candidates);
    }

    private static int collectHomepageCandidates(@NonNull String html,
                                                 @NonNull HttpUrl baseUrl,
                                                 @NonNull List<ArtworkCandidate> candidates,
                                                 int order) {
        Matcher matcher = LINK_TAG_PATTERN.matcher(html);
        while (matcher.find()) {
            Map<String, String> attributes = parseAttributes(matcher.group());
            String href = attributes.get("href");
            if (TextUtils.isEmpty(href)) {
                continue;
            }

            String rel = lowercase(attributes.get("rel"));
            if (TextUtils.isEmpty(rel)) {
                continue;
            }

            if (rel.contains("manifest")) {
                HttpUrl manifestUrl = resolveAbsoluteUrl(baseUrl, href);
                if (manifestUrl != null) {
                    order = collectManifestCandidates(manifestUrl, candidates, order);
                }
                continue;
            }

            if (!isIconRel(rel)) {
                continue;
            }

            HttpUrl iconUrl = resolveAbsoluteUrl(baseUrl, href);
            if (iconUrl == null) {
                continue;
            }

            String type = lowercase(attributes.get("type"));
            SizeDescriptor sizeDescriptor = parseSizeDescriptor(attributes.get("sizes"));
            order = addCandidate(
                    candidates,
                    iconUrl.toString(),
                    sizeDescriptor.maxSize,
                    sizeDescriptor.square,
                    1,
                    order,
                    isSvg(iconUrl.toString(), type)
            );
        }
        return order;
    }

    private static int collectMetaArtworkCandidates(@NonNull String html,
                                                    @NonNull HttpUrl baseUrl,
                                                    @NonNull List<ArtworkCandidate> candidates,
                                                    int order) {
        Matcher matcher = META_TAG_PATTERN.matcher(html);
        while (matcher.find()) {
            Map<String, String> attributes = parseAttributes(matcher.group());
            String content = attributes.get("content");
            if (TextUtils.isEmpty(content)) {
                continue;
            }

            String property = lowercase(attributes.get("property"));
            String name = lowercase(attributes.get("name"));
            String itemProp = lowercase(attributes.get("itemprop"));
            if (!isMetaArtworkReference(property, name, itemProp)) {
                continue;
            }

            HttpUrl artworkUrl = resolveAbsoluteUrl(baseUrl, content);
            if (artworkUrl == null) {
                continue;
            }

            order = addCandidate(candidates, artworkUrl.toString(), 0, false, -1, order, isSvg(artworkUrl.toString()));
        }
        return order;
    }

    private static int collectCommonManifestCandidates(@NonNull HttpUrl baseUrl,
                                                       @NonNull List<ArtworkCandidate> candidates,
                                                       int order) {
        for (String manifestPath : COMMON_MANIFEST_PATHS) {
            HttpUrl manifestUrl = baseUrl.resolve(manifestPath);
            if (manifestUrl == null) {
                continue;
            }

            int updatedOrder = collectManifestCandidates(manifestUrl, candidates, order);
            if (updatedOrder != order) {
                return updatedOrder;
            }
        }
        return order;
    }

    private static int collectManifestCandidates(@NonNull HttpUrl manifestUrl,
                                                 @NonNull List<ArtworkCandidate> candidates,
                                                 int order) {
        String manifestText = fetchText(manifestUrl, MAX_MANIFEST_BYTES);
        if (manifestText == null) {
            return order;
        }

        try {
            JSONObject manifestJson = new JSONObject(manifestText);
            JSONArray iconsArray = manifestJson.optJSONArray("icons");
            if (iconsArray == null) {
                return order;
            }

            for (int i = 0; i < iconsArray.length(); i++) {
                JSONObject icon = iconsArray.optJSONObject(i);
                if (icon == null) {
                    continue;
                }

                String src = icon.optString("src", "");
                if (TextUtils.isEmpty(src)) {
                    continue;
                }

                HttpUrl iconUrl = resolveAbsoluteUrl(manifestUrl, src);
                if (iconUrl == null) {
                    continue;
                }

                String type = lowercase(icon.optString("type", ""));
                SizeDescriptor sizeDescriptor = parseSizeDescriptor(icon.optString("sizes", ""));
                order = addCandidate(
                        candidates,
                        iconUrl.toString(),
                        sizeDescriptor.maxSize,
                        sizeDescriptor.square,
                        3,
                        order,
                        isSvg(iconUrl.toString(), type)
                );
            }
        } catch (JSONException ignored) {
        }

        return order;
    }

    @NonNull
    private static List<String> deduplicateAndRank(@NonNull List<ArtworkCandidate> candidates) {
        candidates.sort(
                Comparator.comparingInt((ArtworkCandidate candidate) -> candidate.isVector() ? 1 : 0).reversed()
                        .thenComparing(Comparator.comparingInt(ArtworkCandidate::getDeclaredSize).reversed())
                        .thenComparing(Comparator.comparingInt((ArtworkCandidate candidate) -> candidate.isSquare() ? 1 : 0).reversed())
                        .thenComparing(Comparator.comparingInt(ArtworkCandidate::getSourcePriority).reversed())
                        .thenComparingInt(ArtworkCandidate::getOrder)
        );

        Set<String> uniqueUrls = new LinkedHashSet<>();
        for (ArtworkCandidate candidate : candidates) {
            uniqueUrls.add(candidate.url);
        }
        return new ArrayList<>(uniqueUrls);
    }

    @NonNull
    private static List<String> prependWorkingUrl(@NonNull String cacheKey,
                                                  @NonNull List<String> urls) {
        String workingUrl = WORKING_URL_CACHE.get(cacheKey);
        if (workingUrl == null || urls.isEmpty() || workingUrl.equals(urls.get(0))) {
            return urls;
        }

        List<String> reorderedUrls = new ArrayList<>(urls.size());
        reorderedUrls.add(workingUrl);
        for (String url : urls) {
            if (!workingUrl.equals(url)) {
                reorderedUrls.add(url);
            }
        }
        return reorderedUrls;
    }

    @NonNull
    private static List<String> buildFallbackUrls(@NonNull Station station) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        String faviconUrl = normalizeRemoteUrl(station.getFavicon());
        if (faviconUrl != null) {
            urls.add(faviconUrl);
        }

        return new ArrayList<>(urls);
    }

    private static int addCandidate(@NonNull List<ArtworkCandidate> candidates,
                                    @NonNull String url,
                                    int declaredSize,
                                    boolean square,
                                    int sourcePriority,
                                    int order,
                                    boolean vector) {
        SVG_URL_FLAGS.put(url, vector);
        candidates.add(new ArtworkCandidate(url, declaredSize, square, sourcePriority, order, vector));
        return order + 1;
    }

    private static void postResolved(@NonNull ResolutionCallback callback,
                                     @NonNull List<String> artworkUrls) {
        List<String> deliveredUrls = List.copyOf(artworkUrls);
        ArtworkLoaderSupport.postToMain(() -> callback.onResolved(deliveredUrls));
    }

    @Nullable
    private static String fetchText(@NonNull HttpUrl url, int maxBytes) {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ArtworkLoaderSupport.userAgent())
                .header("Accept", "text/html,application/manifest+json,application/json,*/*")
                .build();

        try (Response response = ArtworkLoaderSupport.httpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return null;
            }

            return readBody(responseBody, maxBytes);
        } catch (IOException ignored) {
            return null;
        }
    }

    @NonNull
    private static String readBody(@NonNull ResponseBody responseBody, int maxBytes)
            throws IOException {
        try (InputStream inputStream = responseBody.byteStream()) {
            byte[] buffer = new byte[4096];
            StringBuilder builder = new StringBuilder();
            int remainingBytes = maxBytes;

            while (remainingBytes > 0) {
                int read = inputStream.read(buffer, 0, Math.min(buffer.length, remainingBytes));
                if (read == -1) {
                    break;
                }
                builder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                remainingBytes -= read;
            }

            return builder.toString();
        }
    }

    @NonNull
    private static Map<String, String> parseAttributes(@NonNull String tag) {
        Map<String, String> attributes = new HashMap<>();
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(tag);
        while (matcher.find()) {
            String key = lowercase(matcher.group(1));
            String value = matcher.group(3);
            if (value == null) {
                value = matcher.group(4);
            }
            if (value == null) {
                value = matcher.group(5);
            }
            if (key != null && value != null) {
                attributes.put(key, value);
            }
        }
        return attributes;
    }

    private static boolean isIconRel(@NonNull String rel) {
        return rel.contains("icon");
    }

    private static boolean isMetaArtworkReference(@Nullable String property,
                                                  @Nullable String name,
                                                  @Nullable String itemProp) {
        return isMetaArtworkKey(property)
                || isMetaArtworkKey(name)
                || isMetaArtworkKey(itemProp);
    }

    private static boolean isMetaArtworkKey(@Nullable String key) {
        if (key == null) {
            return false;
        }
        return key.equals("og:image")
                || key.equals("og:image:url")
                || key.equals("twitter:image")
                || key.equals("twitter:image:src")
                || key.equals("msapplication-tileimage")
                || key.equals("image");
    }

    private static boolean isSvg(@NonNull String url) {
        return isSvg(url, null);
    }

    private static boolean isSvg(@NonNull String url, @Nullable String type) {
        if (type != null && type.contains("svg")) {
            return true;
        }
        return url.toLowerCase(Locale.ROOT).contains(".svg");
    }

    @NonNull
    private static SizeDescriptor parseSizeDescriptor(@Nullable String sizesValue) {
        if (TextUtils.isEmpty(sizesValue)) {
            return new SizeDescriptor(0, false);
        }

        String[] sizeTokens = sizesValue.trim().split("\\s+");
        int maxSize = 0;
        boolean square = false;
        for (String sizeToken : sizeTokens) {
            if ("any".equalsIgnoreCase(sizeToken)) {
                maxSize = Math.max(maxSize, 2048);
                continue;
            }

            String[] dimensions = sizeToken.toLowerCase(Locale.ROOT).split("x");
            if (dimensions.length != 2) {
                continue;
            }

            try {
                int width = Integer.parseInt(dimensions[0]);
                int height = Integer.parseInt(dimensions[1]);
                int largestDimension = Math.max(width, height);
                if (largestDimension > maxSize) {
                    maxSize = largestDimension;
                    square = width == height;
                } else if (largestDimension == maxSize && width == height) {
                    square = true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return new SizeDescriptor(maxSize, square);
    }

    @Nullable
    private static HttpUrl resolveAbsoluteUrl(@NonNull HttpUrl baseUrl, @Nullable String href) {
        if (TextUtils.isEmpty(href)) {
            return null;
        }
        return baseUrl.resolve(href);
    }

    @NonNull
    private static String buildCacheKey(@NonNull Station station) {
        return station.getId()
                + '|'
                + station.getHomepage()
                + '|'
                + station.getFavicon();
    }

    @Nullable
    private static String normalizeRemoteUrl(@Nullable String url) {
        if (url == null) {
            return null;
        }

        String trimmedUrl = url.trim();
        if (trimmedUrl.isEmpty()) {
            return null;
        }

        String lowercaseUrl = trimmedUrl.toLowerCase(Locale.ROOT);
        if (!lowercaseUrl.startsWith("https://") && !lowercaseUrl.startsWith("http://")) {
            return null;
        }

        return trimmedUrl;
    }

    @Nullable
    private static String lowercase(@Nullable String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private static final class ArtworkCandidate {
        @NonNull
        private final String url;
        private final int declaredSize;
        private final boolean square;
        private final int sourcePriority;
        private final int order;
        private final boolean vector;

        private ArtworkCandidate(@NonNull String url,
                                 int declaredSize,
                                 boolean square,
                                 int sourcePriority,
                                 int order,
                                 boolean vector) {
            this.url = url;
            this.declaredSize = declaredSize;
            this.square = square;
            this.sourcePriority = sourcePriority;
            this.order = order;
            this.vector = vector;
        }

        private int getDeclaredSize() {
            return declaredSize;
        }

        private boolean isSquare() {
            return square;
        }

        private int getSourcePriority() {
            return sourcePriority;
        }

        private int getOrder() {
            return order;
        }

        private boolean isVector() {
            return vector;
        }
    }

    private static final class SizeDescriptor {
        private final int maxSize;
        private final boolean square;

        private SizeDescriptor(int maxSize, boolean square) {
            this.maxSize = maxSize;
            this.square = square;
        }
    }
}
