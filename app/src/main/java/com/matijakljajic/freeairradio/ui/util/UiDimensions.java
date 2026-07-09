package com.matijakljajic.freeairradio.ui.util;

import android.content.Context;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;

public final class UiDimensions {

    private UiDimensions() {
    }

    public static int px(@NonNull Context context, @DimenRes int dimenResId) {
        return context.getResources().getDimensionPixelSize(dimenResId);
    }
}
