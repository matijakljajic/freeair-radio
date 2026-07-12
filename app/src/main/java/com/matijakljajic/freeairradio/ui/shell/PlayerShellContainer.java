package com.matijakljajic.freeairradio.ui.shell;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

@SuppressWarnings("unused")
public class PlayerShellContainer extends LinearLayout {

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
        setOrientation(VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(true);
        setFocusable(true);
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
        int desiredBottomMarginPx = UiDimensions.px(getContext(), R.dimen.player_bottom_safe_space) + systemBarBottomInset;
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
}
