package com.matijakljajic.freeairradio.artwork;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.matijakljajic.freeairradio.ui.util.TransparentBorderTrimTransformation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class StationArtworkBitmapLoader {

    private static final int DEFAULT_BITMAP_EDGE_PX = 256;
    private static final int BITMAP_CACHE_SIZE_BYTES = 4 * 1024 * 1024;
    private static final LruCache<String, Bitmap> BITMAP_CACHE = new LruCache<>(BITMAP_CACHE_SIZE_BYTES) {
        @Override
        protected int sizeOf(@NonNull String key, @NonNull Bitmap value) {
            return value.getByteCount();
        }
    };
    private static final Map<String, byte[]> ARTWORK_DATA_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<BitmapLoadCallback>> PENDING_CALLBACKS =
            new ConcurrentHashMap<>();

    private StationArtworkBitmapLoader() {
    }

    public interface BitmapLoadCallback {
        void onBitmapLoaded(@Nullable Bitmap bitmap);
    }

    public static void loadSvgBitmap(@NonNull String url, @NonNull BitmapLoadCallback callback) {
        Bitmap cachedBitmap = getCachedBitmap(url);
        if (cachedBitmap != null) {
            postBitmap(callback, cachedBitmap);
            return;
        }

        synchronized (PENDING_CALLBACKS) {
            List<BitmapLoadCallback> callbacks = PENDING_CALLBACKS.get(url);
            if (callbacks != null) {
                callbacks.add(callback);
                return;
            }

            callbacks = new ArrayList<>();
            callbacks.add(callback);
            PENDING_CALLBACKS.put(url, callbacks);
        }

        ArtworkLoaderSupport.networkExecutor().execute(() -> loadBitmapInBackground(url));
    }

    @Nullable
    public static Bitmap getCachedBitmap(@Nullable String url) {
        if (url == null) {
            return null;
        }
        synchronized (BITMAP_CACHE) {
            return BITMAP_CACHE.get(url);
        }
    }

    @Nullable
    public static byte[] getCachedArtworkData(@Nullable String url) {
        if (url == null) {
            return null;
        }
        byte[] artworkData = ARTWORK_DATA_CACHE.get(url);
        return artworkData == null ? null : artworkData.clone();
    }

    public static void cacheBitmap(@NonNull String url, @NonNull Bitmap bitmap) {
        synchronized (BITMAP_CACHE) {
            BITMAP_CACHE.put(url, bitmap);
        }
        byte[] artworkData = encodeBitmap(bitmap);
        if (artworkData != null) {
            ARTWORK_DATA_CACHE.put(url, artworkData);
        }
    }

    private static void loadBitmapInBackground(@NonNull String url) {
        Bitmap bitmap = fetchAndRenderBitmap(url);
        if (bitmap != null) {
            cacheBitmap(url, bitmap);
        }

        List<BitmapLoadCallback> callbacks;
        synchronized (PENDING_CALLBACKS) {
            callbacks = PENDING_CALLBACKS.remove(url);
        }
        if (callbacks == null) {
            return;
        }

        for (BitmapLoadCallback callback : callbacks) {
            postBitmap(callback, bitmap);
        }
    }

    @Nullable
    private static Bitmap fetchAndRenderBitmap(@NonNull String url) {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ArtworkLoaderSupport.userAgent())
                .header("Accept", "image/svg+xml,image/*,*/*")
                .build();

        try (Response response = ArtworkLoaderSupport.httpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return null;
            }

            try (InputStream inputStream = responseBody.byteStream()) {
                SVG svg = SVG.getFromInputStream(inputStream);
                return renderBitmap(svg);
            }
        } catch (IOException | SVGParseException ignored) {
            return null;
        }
    }

    @Nullable
    private static Bitmap renderBitmap(@NonNull SVG svg) {
        Size size = resolveTargetSize(svg);
        Bitmap bitmap;
        try {
            bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
        } catch (IllegalArgumentException ignored) {
            return null;
        }

        Canvas canvas = new Canvas(bitmap);
        RectF viewBox = svg.getDocumentViewBox();
        float documentWidth = size.sourceWidth;
        float documentHeight = size.sourceHeight;
        float scaleX = size.width / documentWidth;
        float scaleY = size.height / documentHeight;
        canvas.scale(scaleX, scaleY);
        if (viewBox != null) {
            canvas.translate(-viewBox.left, -viewBox.top);
        }
        svg.renderToCanvas(canvas);
        return TransparentBorderTrimTransformation.trimTransparentBorder(bitmap);
    }

    @Nullable
    private static byte[] encodeBitmap(@NonNull Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
            return null;
        }
        return outputStream.toByteArray();
    }

    private static void postBitmap(@NonNull BitmapLoadCallback callback, @Nullable Bitmap bitmap) {
        ArtworkLoaderSupport.postToMain(() -> callback.onBitmapLoaded(bitmap));
    }

    @NonNull
    private static Size resolveTargetSize(@NonNull SVG svg) {
        RectF viewBox = svg.getDocumentViewBox();
        float sourceWidth = sanitizeDimension(svg.getDocumentWidth());
        float sourceHeight = sanitizeDimension(svg.getDocumentHeight());

        if ((sourceWidth <= 0f || sourceHeight <= 0f) && viewBox != null) {
            sourceWidth = Math.max(viewBox.width(), 1f);
            sourceHeight = Math.max(viewBox.height(), 1f);
        }

        if (sourceWidth <= 0f || sourceHeight <= 0f) {
            sourceWidth = DEFAULT_BITMAP_EDGE_PX;
            sourceHeight = DEFAULT_BITMAP_EDGE_PX;
        }

        float aspectRatio = sourceWidth / sourceHeight;
        int width;
        int height;
        if (aspectRatio >= 1f) {
            width = DEFAULT_BITMAP_EDGE_PX;
            height = Math.max(1, Math.round(DEFAULT_BITMAP_EDGE_PX / aspectRatio));
        } else {
            height = DEFAULT_BITMAP_EDGE_PX;
            width = Math.max(1, Math.round(DEFAULT_BITMAP_EDGE_PX * aspectRatio));
        }

        return new Size(width, height, sourceWidth, sourceHeight);
    }

    private static float sanitizeDimension(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value) || value <= 0f) {
            return -1f;
        }
        return value;
    }

    private static final class Size {
        private final int width;
        private final int height;
        private final float sourceWidth;
        private final float sourceHeight;

        private Size(int width, int height, float sourceWidth, float sourceHeight) {
            this.width = width;
            this.height = height;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
        }
    }
}
