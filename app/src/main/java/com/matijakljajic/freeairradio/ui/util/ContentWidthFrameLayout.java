package com.matijakljajic.freeairradio.ui.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.R;

public final class ContentWidthFrameLayout extends FrameLayout {

    public ContentWidthFrameLayout(@NonNull Context context) {
        super(context);
    }

    public ContentWidthFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ContentWidthFrameLayout(@NonNull Context context,
                                   @Nullable AttributeSet attrs,
                                   int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maximumWidth = getResources().getDimensionPixelSize(R.dimen.content_max_width);

        if (mode != MeasureSpec.UNSPECIFIED && maximumWidth > 0 && availableWidth > maximumWidth) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(maximumWidth, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
