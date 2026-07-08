package com.matijakljajic.freeairradio.ui.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class MarqueeTextView extends AppCompatTextView {

    private static final long START_DELAY_MS = 2200L;

    private final Runnable startMarqueeRunnable = this::startMarquee;
    private long selectionToken;
    private long scheduledSelectionToken;

    public MarqueeTextView(@NonNull Context context) {
        super(context);
        init();
    }

    public MarqueeTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MarqueeTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSingleLine(true);
        setMaxLines(1);
        setHorizontallyScrolling(true);
        setEllipsize(TextUtils.TruncateAt.MARQUEE);
        setMarqueeRepeatLimit(-1);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    public void setSelected(boolean selected) {
        if (!selected) {
            selectionToken++;
            removeCallbacks(startMarqueeRunnable);
            super.setSelected(false);
            clearFocus();
            return;
        }

        super.setSelected(true);
        scheduleMarquee();
    }

    @Override
    public void setText(@Nullable CharSequence text, BufferType type) {
        super.setText(text, type);
        if (isSelected()) {
            scheduleMarquee();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        selectionToken++;
        removeCallbacks(startMarqueeRunnable);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (isSelected()) {
            scheduleMarquee();
        }
    }

    @Override
    public boolean isFocused() {
        return isSelected() || super.isFocused();
    }

    private void scheduleMarquee() {
        removeCallbacks(startMarqueeRunnable);
        if (TextUtils.isEmpty(getText())) {
            return;
        }

        selectionToken++;
        scheduledSelectionToken = selectionToken;
        postDelayed(startMarqueeRunnable, START_DELAY_MS);
    }

    private void startMarquee() {
        if (!isSelected() || scheduledSelectionToken != selectionToken) {
            return;
        }
        requestFocus();
        invalidate();
    }
}
