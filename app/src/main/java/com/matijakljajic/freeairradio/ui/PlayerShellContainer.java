package com.matijakljajic.freeairradio.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PlayerShellContainer extends FrameLayout {

    private static final int PLAYER_BOTTOM_SAFE_SPACE_DP = 16;
    private int lastAppliedBottomMarginPx = Integer.MIN_VALUE;

    public PlayerShellContainer(@NonNull Context context) {
        super(context);
        init();
    }

    public PlayerShellContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PlayerShellContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);
        ViewCompat.setOnApplyWindowInsetsListener(this, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            applyBottomMargin(systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewCompat.requestApplyInsets(this);
    }

    private void applyBottomMargin(int systemBarBottomInset) {
        int desiredBottomMarginPx = dpToPx(PLAYER_BOTTOM_SAFE_SPACE_DP) + systemBarBottomInset;
        if (desiredBottomMarginPx == lastAppliedBottomMarginPx) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        marginLayoutParams.bottomMargin = desiredBottomMarginPx;
        lastAppliedBottomMarginPx = desiredBottomMarginPx;
        setLayoutParams(marginLayoutParams);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
