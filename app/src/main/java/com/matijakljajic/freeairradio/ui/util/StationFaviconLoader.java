package com.matijakljajic.freeairradio.ui.util;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.artwork.StationArtworkBitmapLoader;
import com.matijakljajic.freeairradio.artwork.StationArtworkResolver;
import com.matijakljajic.freeairradio.data.model.Station;

import java.util.List;

public final class StationFaviconLoader {

    private static final int MIN_ACCEPTABLE_FAVICON_SIZE_PX = 48;
    private static final int FAVICON_FADE_OUT_DURATION_MS = 90;
    private static final int FAVICON_FADE_IN_DURATION_MS = 140;
    private static final int INITIAL_ITEM_REVEAL_STAGGER_STEP_MS = 28;
    private static final float FIT_CENTER_ASPECT_RATIO_THRESHOLD = 1.25f;
    private static final TransparentBorderTrimTransformation TRANSPARENT_BORDER_TRIM_TRANSFORMATION =
            new TransparentBorderTrimTransformation();

    private StationFaviconLoader() {
    }

    public static void loadInto(@NonNull ImageView imageView, @Nullable Station station) {
        loadIntoInternal(imageView, station, -1);
    }

    public static void loadInto(@NonNull ImageView imageView,
                                @Nullable Station station,
                                int initialRevealOrder) {
        loadIntoInternal(imageView, station, initialRevealOrder);
    }

    private static void loadIntoInternal(@NonNull ImageView imageView,
                                         @Nullable Station station,
                                         int initialRevealOrder) {
        resetImageView(imageView);
        if (station == null) {
            clear(imageView);
            return;
        }

        String requestTag = buildRequestTag(station);
        prepareViewForLoad(imageView, requestTag);
        loadResolvedCandidateUrls(
                imageView,
                station,
                StationArtworkResolver.getBestAvailableUrls(station),
                requestTag,
                initialRevealOrder
        );
        if (!hasDirectArtworkUrl(station)
                && !requestDeepResolution(imageView, station, requestTag, initialRevealOrder)) {
            showDefaultIcon(imageView);
        }
    }

    private static void resetImageView(@NonNull ImageView imageView) {
        imageView.animate().cancel();
        imageView.setAlpha(1f);
    }

    private static void prepareViewForLoad(@NonNull ImageView imageView,
                                           @NonNull String requestTag) {
        if (!TextUtils.equals(requestTag, (String) imageView.getTag(R.id.station_favicon_request_tag))) {
            showDefaultIcon(imageView);
        }
        imageView.setTag(R.id.station_favicon_request_tag, requestTag);
        imageView.setTag(R.id.station_favicon_deep_resolve_tag, null);
    }

    public static void clear(@NonNull ImageView imageView) {
        imageView.animate().cancel();
        imageView.setAlpha(1f);
        Glide.with(imageView).clear(imageView);
        imageView.setImageDrawable(null);
        imageView.setTag(R.id.station_favicon_request_tag, null);
        imageView.setTag(R.id.station_favicon_signature_tag, null);
        imageView.setTag(R.id.station_favicon_deep_resolve_tag, null);
        imageView.setTag(R.id.station_favicon_displayed_url_tag, null);
    }

    private static void loadResolvedCandidateUrls(@NonNull ImageView imageView,
                                                  @NonNull Station station,
                                                  @NonNull List<String> artworkUrls,
                                                  @NonNull String requestTag,
                                                  int initialRevealOrder) {
        String signature = buildSignature(artworkUrls);
        if (TextUtils.isEmpty(signature)) {
            showDefaultIcon(imageView);
            return;
        }

        if (TextUtils.equals(signature, (String) imageView.getTag(R.id.station_favicon_signature_tag))) {
            restoreScaleTypeFromDisplayedArtwork(imageView);
            return;
        }

        imageView.setTag(R.id.station_favicon_signature_tag, signature);
        loadCandidateUrlInto(
                imageView,
                station,
                artworkUrls,
                0,
                requestTag,
                signature,
                initialRevealOrder
        );
    }

    private static void loadCandidateUrlInto(@NonNull ImageView imageView,
                                             @NonNull Station station,
                                             @NonNull List<String> artworkUrls,
                                             int candidateIndex,
                                             @NonNull String requestTag,
                                             @NonNull String signature,
                                             int initialRevealOrder) {
        if (candidateIndex >= artworkUrls.size()) {
            showDefaultIcon(imageView);
            return;
        }

        String urlToLoad = artworkUrls.get(candidateIndex);
        Bitmap cachedBitmap = StationArtworkBitmapLoader.getCachedBitmap(urlToLoad);
        if (cachedBitmap != null) {
            boolean animateReplacement = isShowingResolvedArtwork(imageView)
                    && !TextUtils.equals(urlToLoad, getDisplayedUrl(imageView));
            displayResolvedBitmap(
                    imageView,
                    station,
                    artworkUrls,
                    candidateIndex,
                    requestTag,
                    signature,
                    urlToLoad,
                    cachedBitmap,
                    animateReplacement,
                    initialRevealOrder
            );
            return;
        }

        if (StationArtworkResolver.isSvgUrl(urlToLoad)) {
            loadSvgCandidateUrlInto(
                    imageView,
                    station,
                    artworkUrls,
                    candidateIndex,
                    requestTag,
                    signature,
                    urlToLoad,
                    initialRevealOrder
            );
            return;
        }

        loadRasterCandidateUrlInto(
                imageView,
                station,
                artworkUrls,
                candidateIndex,
                requestTag,
                signature,
                urlToLoad,
                initialRevealOrder
        );
    }

    private static void loadRasterCandidateUrlInto(@NonNull ImageView imageView,
                                                   @NonNull Station station,
                                                   @NonNull List<String> artworkUrls,
                                                   int candidateIndex,
                                                   @NonNull String requestTag,
                                                   @NonNull String signature,
                                                   @NonNull String urlToLoad,
                                                   int initialRevealOrder) {
        RequestBuilder<Bitmap> requestBuilder = Glide.with(imageView)
                .asBitmap()
                .load(urlToLoad)
                .transform(TRANSPARENT_BORDER_TRIM_TRANSFORMATION);
        requestBuilder = applyCurrentPlaceholder(requestBuilder, imageView.getDrawable());

        requestBuilder.listener(new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e,
                                        Object model,
                                        @NonNull Target<Bitmap> target,
                                        boolean isFirstResource) {
                if (!isCurrentLoad(imageView, requestTag, signature)) {
                    return true;
                }
                // Glide forbids starting a new request directly from a request callback.
                imageView.post(() -> {
                    if (!isCurrentLoad(imageView, requestTag, signature)) {
                        return;
                    }
                    advanceCandidateOrResolve(
                            imageView,
                            station,
                            artworkUrls,
                            candidateIndex,
                            requestTag,
                            signature,
                            initialRevealOrder
                    );
                });
                return true;
            }

            @Override
            public boolean onResourceReady(@NonNull Bitmap resource,
                                           @NonNull Object model,
                                           Target<Bitmap> target,
                                           @NonNull DataSource dataSource,
                                           boolean isFirstResource) {
                if (!isCurrentLoad(imageView, requestTag, signature)) {
                    return true;
                }

                StationArtworkBitmapLoader.cacheBitmap(urlToLoad, resource);
                displayResolvedBitmap(
                        imageView,
                        station,
                        artworkUrls,
                        candidateIndex,
                        requestTag,
                        signature,
                        urlToLoad,
                        resource,
                        true,
                        initialRevealOrder
                );
                return true;
            }
        }).into(imageView);
    }

    @NonNull
    private static RequestBuilder<Bitmap> applyCurrentPlaceholder(@NonNull RequestBuilder<Bitmap> requestBuilder,
                                                                  @Nullable Drawable currentDrawable) {
        if (currentDrawable != null) {
            return requestBuilder.placeholder(currentDrawable).error(currentDrawable);
        }
        return requestBuilder.placeholder(R.drawable.ic_station_favicon_default)
                .error(R.drawable.ic_station_favicon_default);
    }

    private static void loadSvgCandidateUrlInto(@NonNull ImageView imageView,
                                                @NonNull Station station,
                                                @NonNull List<String> artworkUrls,
                                                int candidateIndex,
                                                @NonNull String requestTag,
                                                @NonNull String signature,
                                                @NonNull String urlToLoad,
                                                int initialRevealOrder) {
        if (!isShowingResolvedArtwork(imageView)) {
            showDefaultIcon(imageView);
        }
        boolean animateReplacement = StationArtworkBitmapLoader.getCachedBitmap(urlToLoad) == null;
        StationArtworkBitmapLoader.loadSvgBitmap(urlToLoad, bitmap -> {
            if (!isCurrentLoad(imageView, requestTag, signature)) {
                return;
            }
            if (bitmap == null) {
                advanceCandidateOrResolve(
                        imageView,
                        station,
                        artworkUrls,
                        candidateIndex,
                        requestTag,
                        signature,
                        initialRevealOrder
                );
                return;
            }

            displayResolvedBitmap(
                    imageView,
                    station,
                    artworkUrls,
                    candidateIndex,
                    requestTag,
                    signature,
                    urlToLoad,
                    bitmap,
                    animateReplacement,
                    initialRevealOrder
            );
        });
    }

    private static boolean isSmall(@NonNull Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }
        return Math.min(width, height) < MIN_ACCEPTABLE_FAVICON_SIZE_PX;
    }

    @NonNull
    private static String buildRequestTag(@NonNull Station station) {
        return station.getId() + '|' + station.getHomepage() + '|'
                + station.getFavicon();
    }

    @NonNull
    private static String buildSignature(@NonNull List<String> artworkUrls) {
        return TextUtils.join("|", artworkUrls);
    }

    private static boolean isSameRequest(@NonNull ImageView imageView, @NonNull String requestTag) {
        return TextUtils.equals(requestTag, (String) imageView.getTag(R.id.station_favicon_request_tag));
    }

    private static boolean isCurrentSignature(@NonNull ImageView imageView, @NonNull String signature) {
        return TextUtils.equals(signature, (String) imageView.getTag(R.id.station_favicon_signature_tag));
    }

    private static boolean isCurrentLoad(@NonNull ImageView imageView,
                                         @NonNull String requestTag,
                                         @NonNull String signature) {
        return isSameRequest(imageView, requestTag) && isCurrentSignature(imageView, signature);
    }

    private static void replaceBitmapWithFade(@NonNull ImageView imageView,
                                              @NonNull Bitmap bitmap,
                                              @NonNull String urlToLoad,
                                              @NonNull String requestTag,
                                              @NonNull String signature,
                                              long startDelayMs) {
        imageView.animate().cancel();
        Drawable currentDrawable = imageView.getDrawable();
        if (currentDrawable == null) {
            showResolvedBitmap(imageView, bitmap, urlToLoad);
            return;
        }

        Runnable startAnimation = () -> {
            if (!isCurrentLoad(imageView, requestTag, signature)) {
                return;
            }
            imageView.animate()
                    .alpha(0f)
                    .setDuration(FAVICON_FADE_OUT_DURATION_MS)
                    .withEndAction(() -> {
                        if (!isCurrentLoad(imageView, requestTag, signature)) {
                            imageView.setAlpha(1f);
                            return;
                        }
                        showResolvedBitmap(imageView, bitmap, urlToLoad);
                        imageView.setAlpha(0f);
                        imageView.animate()
                                .alpha(1f)
                                .setDuration(FAVICON_FADE_IN_DURATION_MS)
                                .start();
                    })
                    .start();
        };
        if (startDelayMs > 0L) {
            imageView.postDelayed(startAnimation, startDelayMs);
            return;
        }
        startAnimation.run();
    }

    private static void displayResolvedBitmap(@NonNull ImageView imageView,
                                              @NonNull Station station,
                                              @NonNull List<String> artworkUrls,
                                              int candidateIndex,
                                              @NonNull String requestTag,
                                              @NonNull String signature,
                                              @NonNull String urlToLoad,
                                              @NonNull Bitmap bitmap,
                                              boolean animateReplacement,
                                              int initialRevealOrder) {
        if (isSmall(bitmap) && candidateIndex + 1 < artworkUrls.size()) {
            imageView.post(() -> loadCandidateUrlInto(
                    imageView,
                    station,
                    artworkUrls,
                    candidateIndex + 1,
                    requestTag,
                    signature,
                    initialRevealOrder
            ));
            return;
        }

        if (isSmall(bitmap)) {
            requestDeepResolution(imageView, station, requestTag, initialRevealOrder);
        }

        StationArtworkResolver.markLoadSucceeded(station, urlToLoad);
        if (animateReplacement && isShowingResolvedArtwork(imageView)) {
            replaceBitmapWithFade(imageView, bitmap, urlToLoad, requestTag, signature, 0L);
            return;
        }

        if (animateReplacement && initialRevealOrder >= 0) {
            replaceBitmapWithFade(
                    imageView,
                    bitmap,
                    urlToLoad,
                    requestTag,
                    signature,
                    getInitialRevealDelayMs(initialRevealOrder)
            );
            return;
        }

        imageView.animate().cancel();
        showResolvedBitmap(imageView, bitmap, urlToLoad);
    }

    private static void showDefaultIcon(@NonNull ImageView imageView) {
        showDefaultIcon(imageView, false);
    }

    private static void showDefaultIcon(@NonNull ImageView imageView, boolean animateReplacement) {
        if (animateReplacement && isShowingResolvedArtwork(imageView) && imageView.getDrawable() != null) {
            imageView.animate().cancel();
            imageView.animate()
                    .alpha(0f)
                    .setDuration(FAVICON_FADE_OUT_DURATION_MS)
                    .withEndAction(() -> {
                        applyDefaultIcon(imageView);
                        imageView.setAlpha(0f);
                        imageView.animate()
                                .alpha(1f)
                                .setDuration(FAVICON_FADE_IN_DURATION_MS)
                                .start();
                    })
                    .start();
            return;
        }

        applyDefaultIcon(imageView);
    }

    private static void applyDefaultIcon(@NonNull ImageView imageView) {
        imageView.animate().cancel();
        imageView.setAlpha(1f);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(R.drawable.ic_station_favicon_default);
        imageView.setTag(R.id.station_favicon_displayed_url_tag, null);
    }

    private static void showResolvedBitmap(@NonNull ImageView imageView,
                                           @NonNull Bitmap bitmap,
                                           @NonNull String urlToLoad) {
        applyAdaptiveScaleType(imageView, bitmap);
        imageView.setAlpha(1f);
        imageView.setImageBitmap(bitmap);
        imageView.setTag(R.id.station_favicon_displayed_url_tag, urlToLoad);
    }

    private static boolean isShowingResolvedArtwork(@NonNull ImageView imageView) {
        return !TextUtils.isEmpty(getDisplayedUrl(imageView));
    }

    private static void restoreScaleTypeFromDisplayedArtwork(@NonNull ImageView imageView) {
        String displayedUrl = getDisplayedUrl(imageView);
        if (TextUtils.isEmpty(displayedUrl)) {
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return;
        }

        Bitmap cachedBitmap = StationArtworkBitmapLoader.getCachedBitmap(displayedUrl);
        if (cachedBitmap == null) {
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return;
        }

        applyAdaptiveScaleType(imageView, cachedBitmap);
    }

    @Nullable
    private static String getDisplayedUrl(@NonNull ImageView imageView) {
        return (String) imageView.getTag(R.id.station_favicon_displayed_url_tag);
    }

    private static long getInitialRevealDelayMs(int initialRevealOrder) {
        return Math.max(0, initialRevealOrder) * (long) INITIAL_ITEM_REVEAL_STAGGER_STEP_MS;
    }

    private static void applyAdaptiveScaleType(@NonNull ImageView imageView,
                                               @NonNull Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= 0 || height <= 0) {
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return;
        }

        float longestSideRatio = Math.max(width, height) / (float) Math.min(width, height);
        imageView.setScaleType(longestSideRatio >= FIT_CENTER_ASPECT_RATIO_THRESHOLD
                ? ImageView.ScaleType.FIT_CENTER
                : ImageView.ScaleType.CENTER_CROP);
    }

    private static boolean requestDeepResolution(@NonNull ImageView imageView,
                                                 @NonNull Station station,
                                                 @NonNull String requestTag,
                                                 int initialRevealOrder) {
        if (!StationArtworkResolver.hasHomepageSource(station)
                || !isSameRequest(imageView, requestTag)
                || Boolean.TRUE.equals(imageView.getTag(R.id.station_favicon_deep_resolve_tag))) {
            return false;
        }

        imageView.setTag(R.id.station_favicon_deep_resolve_tag, true);
        StationArtworkResolver.resolve(station, artworkUrls -> {
            if (!isSameRequest(imageView, requestTag)) {
                return;
            }
            // Resolver callbacks may arrive while a Glide listener is still unwinding.
            imageView.post(() -> {
                if (!isSameRequest(imageView, requestTag)) {
                    return;
                }
                loadResolvedCandidateUrls(
                        imageView,
                        station,
                        artworkUrls,
                        requestTag,
                        initialRevealOrder
                );
            });
        });
        return true;
    }

    private static void advanceCandidateOrResolve(@NonNull ImageView imageView,
                                                  @NonNull Station station,
                                                  @NonNull List<String> artworkUrls,
                                                  int candidateIndex,
                                                  @NonNull String requestTag,
                                                  @NonNull String signature,
                                                  int initialRevealOrder) {
        if (candidateIndex + 1 < artworkUrls.size()) {
            imageView.post(() -> loadCandidateUrlInto(
                    imageView,
                    station,
                    artworkUrls,
                    candidateIndex + 1,
                    requestTag,
                    signature,
                    initialRevealOrder
            ));
            return;
        }

        if (!requestDeepResolution(imageView, station, requestTag, initialRevealOrder)) {
            showDefaultIcon(imageView);
        }
    }

    private static boolean hasDirectArtworkUrl(@NonNull Station station) {
        if (StationArtworkResolver.hasResolvedCandidates(station)) {
            return true;
        }
        String favicon = station.getFavicon();
        if (favicon == null) {
            return false;
        }
        String trimmed = favicon.trim().toLowerCase();
        return trimmed.startsWith("https://") || trimmed.startsWith("http://");
    }
}
