package com.matijakljajic.freeairradio.ui.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class TransparentBorderTrimTransformation extends BitmapTransformation {

    private static final byte[] ID_BYTES =
            "com.matijakljajic.freeairradio.ui.util.TransparentBorderTrimTransformation"
                    .getBytes(StandardCharsets.UTF_8);
    private static final int ALPHA_THRESHOLD = 12;

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool,
                               @NonNull Bitmap toTransform,
                               int outWidth,
                               int outHeight) {
        return trimBitmap(toTransform, pool);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof TransparentBorderTrimTransformation;
    }

    @Override
    public int hashCode() {
        return TransparentBorderTrimTransformation.class.getName().hashCode();
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
    }

    private static boolean coversWholeBitmap(@NonNull Bitmap bitmap, @NonNull Rect visibleBounds) {
        return visibleBounds.left == 0
                && visibleBounds.top == 0
                && visibleBounds.right == bitmap.getWidth()
                && visibleBounds.bottom == bitmap.getHeight();
    }

    @NonNull
    public static Bitmap trimTransparentBorder(@NonNull Bitmap bitmap) {
        return trimBitmap(bitmap, null);
    }

    @NonNull
    private static Bitmap trimBitmap(@NonNull Bitmap bitmap, @Nullable BitmapPool pool) {
        if (!bitmap.hasAlpha()) {
            return bitmap;
        }

        Rect visibleBounds = findVisibleBounds(bitmap);
        if (visibleBounds == null || coversWholeBitmap(bitmap, visibleBounds)) {
            return bitmap;
        }

        Bitmap trimmedBitmap;
        if (pool != null) {
            Bitmap.Config config = bitmap.getConfig() != null
                    ? bitmap.getConfig()
                    : Bitmap.Config.ARGB_8888;
            trimmedBitmap = pool.get(visibleBounds.width(), visibleBounds.height(), config);
        } else {
            trimmedBitmap = Bitmap.createBitmap(
                    visibleBounds.width(),
                    visibleBounds.height(),
                    bitmap.getConfig() != null ? bitmap.getConfig() : Bitmap.Config.ARGB_8888
            );
        }
        trimmedBitmap.setHasAlpha(true);

        Canvas canvas = new Canvas(trimmedBitmap);
        canvas.drawBitmap(
                bitmap,
                visibleBounds,
                new Rect(0, 0, visibleBounds.width(), visibleBounds.height()),
                null
        );
        return trimmedBitmap;
    }

    private static Rect findVisibleBounds(@NonNull Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int top = findTop(bitmap, width, height);
        if (top == -1) {
            return null;
        }

        int bottom = findBottom(bitmap, width, height);
        int left = findLeft(bitmap, width, top, bottom);
        int right = findRight(bitmap, width, top, bottom);
        return new Rect(left, top, right + 1, bottom + 1);
    }

    private static int findTop(@NonNull Bitmap bitmap, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (Color.alpha(bitmap.getPixel(x, y)) > ALPHA_THRESHOLD) {
                    return y;
                }
            }
        }
        return -1;
    }

    private static int findBottom(@NonNull Bitmap bitmap, int width, int height) {
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                if (Color.alpha(bitmap.getPixel(x, y)) > ALPHA_THRESHOLD) {
                    return y;
                }
            }
        }
        return 0;
    }

    private static int findLeft(@NonNull Bitmap bitmap, int width, int top, int bottom) {
        for (int x = 0; x < width; x++) {
            for (int y = top; y <= bottom; y++) {
                if (Color.alpha(bitmap.getPixel(x, y)) > ALPHA_THRESHOLD) {
                    return x;
                }
            }
        }
        return 0;
    }

    private static int findRight(@NonNull Bitmap bitmap, int width, int top, int bottom) {
        for (int x = width - 1; x >= 0; x--) {
            for (int y = top; y <= bottom; y++) {
                if (Color.alpha(bitmap.getPixel(x, y)) > ALPHA_THRESHOLD) {
                    return x;
                }
            }
        }
        return width - 1;
    }
}
