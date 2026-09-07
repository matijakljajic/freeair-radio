package com.matijakljajic.freeairradio.ui.util;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.R;

public final class DialogWindowHelper {

    private DialogWindowHelper() {
    }

    public static void applyWideCenteredLayout(@Nullable Dialog dialog) {
        if (dialog == null) {
            return;
        }

        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }

        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        int windowWidthPx = window.getDecorView().getRootView().getWidth();
        if (windowWidthPx <= 0) {
            windowWidthPx = dialog.getContext().getResources().getDisplayMetrics().widthPixels;
        }
        int horizontalMarginPx = dialog.getContext().getResources()
                .getDimensionPixelSize(R.dimen.dialog_horizontal_margin);
        int maximumWidthPx = dialog.getContext().getResources()
                .getDimensionPixelSize(R.dimen.dialog_max_width);
        int availableWidthPx = Math.max(0, windowWidthPx - (horizontalMarginPx * 2));

        window.setGravity(Gravity.CENTER);
        window.setLayout(
                Math.min(availableWidthPx, maximumWidthPx),
                WindowManager.LayoutParams.WRAP_CONTENT
        );
    }
}
