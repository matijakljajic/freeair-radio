package com.matijakljajic.freeairradio.ui.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import com.matijakljajic.freeairradio.R;

public final class DialogWindowHelper {

    private DialogWindowHelper() {
    }

    public static void applyResponsiveLayout(@Nullable Dialog dialog) {
        if (dialog == null) {
            return;
        }

        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }

        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View rootView = window.getDecorView().getRootView();
        int windowWidthPx = rootView.getWidth();
        if (windowWidthPx <= 0) {
            windowWidthPx = dialog.getContext().getResources().getDisplayMetrics().widthPixels;
        }
        int horizontalMarginPx = dialog.getContext().getResources()
                .getDimensionPixelSize(R.dimen.dialog_horizontal_margin);
        int maximumWidthPx = dialog.getContext().getResources()
                .getDimensionPixelSize(R.dimen.dialog_max_width);
        int availableWidthPx = Math.max(0, windowWidthPx - (horizontalMarginPx * 2));

        window.setGravity(Gravity.CENTER);
        int dialogWidthPx = Math.min(availableWidthPx, maximumWidthPx);
        window.setLayout(dialogWidthPx, WindowManager.LayoutParams.WRAP_CONTENT);
        if (dialog.getContext().getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_LANDSCAPE) {
            return;
        }
        window.getDecorView().post(() -> constrainContentHeight(dialog, window, dialogWidthPx));
    }

    private static void constrainContentHeight(@NonNull Dialog dialog,
                                               @NonNull Window window,
                                               int dialogWidthPx) {
        NestedScrollView scrollContainer = dialog.findViewById(R.id.dialog_scroll_container);
        if (scrollContainer == null || scrollContainer.getChildCount() == 0) {
            return;
        }

        View dialogContentView = scrollContainer.getChildAt(0);
        int contentHeightPx = dialogContentView.getHeight();
        if (contentHeightPx <= 0) {
            return;
        }

        int windowHeightPx = getAvailableWindowHeight(dialog);
        int verticalMarginPx = dialog.getContext().getResources()
                .getDimensionPixelSize(R.dimen.dialog_vertical_margin);
        int maximumHeightPx = Math.max(0, windowHeightPx - (verticalMarginPx * 2));
        if (contentHeightPx <= maximumHeightPx) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = scrollContainer.getLayoutParams();
        if (layoutParams != null && layoutParams.height != maximumHeightPx) {
            layoutParams.height = maximumHeightPx;
            scrollContainer.setLayoutParams(layoutParams);
        }
        window.setLayout(dialogWidthPx, maximumHeightPx);
    }

    private static int getAvailableWindowHeight(@NonNull Dialog dialog) {
        Activity ownerActivity = dialog.getOwnerActivity();
        if (ownerActivity != null) {
            View contentView = ownerActivity.findViewById(android.R.id.content);
            if (contentView != null && contentView.getHeight() > 0) {
                return contentView.getHeight();
            }
        }
        return dialog.getContext().getResources().getDisplayMetrics().heightPixels;
    }
}
