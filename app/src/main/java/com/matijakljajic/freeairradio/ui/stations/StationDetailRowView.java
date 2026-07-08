package com.matijakljajic.freeairradio.ui.stations;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.color.MaterialColors;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.ui.util.MarqueeTextView;

public class StationDetailRowView extends LinearLayout {

    private final LinearLayout contentContainer;
    private final TextView labelView;
    private final MarqueeTextView valueView;
    private final View dividerView;
    private final int defaultValueTextColor;

    public StationDetailRowView(@NonNull Context context) {
        this(context, null);
    }

    public StationDetailRowView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StationDetailRowView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);

        contentContainer = new LinearLayout(context);
        labelView = new TextView(context);
        valueView = new MarqueeTextView(context);
        dividerView = new View(context);

        initViews();
        defaultValueTextColor = valueView.getCurrentTextColor();
        useCompactStyle();
    }

    private void initViews() {
        contentContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(contentContainer);

        dividerView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(1)));
        dividerView.setBackgroundColor(MaterialColors.getColor(this, android.R.attr.textColorSecondary));
        addView(dividerView);

        labelView.setEllipsize(TextUtils.TruncateAt.END);
        labelView.setMaxLines(1);
        labelView.setTextIsSelectable(false);
        labelView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium);

        valueView.setTextIsSelectable(false);
        valueView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        valueView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        valueView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        valueView.setMarqueeRepeatLimit(1);
        valueView.setTextAppearance(getContext(), com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
    }

    public void useCompactStyle() {
        contentContainer.removeAllViews();
        contentContainer.setOrientation(HORIZONTAL);
        contentContainer.setGravity(Gravity.CENTER_VERTICAL);
        contentContainer.setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(12));

        LayoutParams labelParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        labelParams.rightMargin = dpToPx(12);
        contentContainer.addView(labelView, labelParams);
        contentContainer.addView(valueView, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
    }

    public void useTallStyle() {
        contentContainer.removeAllViews();
        contentContainer.setOrientation(VERTICAL);
        contentContainer.setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(10));

        LayoutParams labelParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        LayoutParams valueParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        valueParams.topMargin = dpToPx(4);
        contentContainer.addView(labelView, labelParams);
        contentContainer.addView(valueView, valueParams);
    }

    public boolean bindText(@StringRes int labelResId, @NonNull String value) {
        if (isUnknown(value)) {
            setVisibility(GONE);
            return false;
        }

        return bindStaticValue(labelResId, value);
    }

    public boolean bindBitrate(@StringRes int labelResId, int bitrate) {
        if (bitrate <= 0) {
            setVisibility(GONE);
            return false;
        }

        String bitrateText = getResources().getString(com.matijakljajic.freeairradio.R.string.station_details_bitrate_value, bitrate);
        return bindStaticValue(labelResId, bitrateText);
    }

    public boolean bindLink(@StringRes int labelResId, @NonNull String value, @Nullable OnClickListener onClickListener) {
        if (isUnknown(value)) {
            setVisibility(GONE);
            return false;
        }

        setVisibility(VISIBLE);
        labelView.setText(labelResId);
        setLinkValueDisplay();
        valueView.setText(value);
        valueView.setContentDescription(value);
        valueView.setPaintFlags(valueView.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        valueView.setTextColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary));
        valueView.setClickable(false);
        valueView.setLongClickable(false);
        setOnClickListener(onClickListener);
        setClickable(onClickListener != null);
        return true;
    }

    public void restartMarquee() {
        valueView.setSelected(false);
        valueView.setSelected(true);
    }

    public void setMarqueeRestartOnLongClickListener() {
        setOnLongClickListener(v -> {
            if (!isValueOverflowing()) {
                return true;
            }
            setMarqueeDisplay();
            valueView.post(this::restartMarquee);
            return true;
        });
    }

    private void clearValueClickState() {
        valueView.setPaintFlags(valueView.getPaintFlags() & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        valueView.setTextColor(defaultValueTextColor);
        valueView.setOnClickListener(null);
        valueView.setClickable(false);
        valueView.setLongClickable(false);
        setOnClickListener(null);
        setOnLongClickListener(null);
        setClickable(false);
        setLongClickable(false);
    }

    private boolean bindStaticValue(@StringRes int labelResId, @NonNull String value) {
        setVisibility(VISIBLE);
        labelView.setText(labelResId);
        setStaticValueDisplay();
        valueView.setText(value);
        valueView.setContentDescription(value);
        clearValueClickState();
        return true;
    }

    private void setStaticValueDisplay() {
        valueView.setEllipsize(TextUtils.TruncateAt.END);
        valueView.setHorizontallyScrolling(false);
        valueView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        valueView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        valueView.setSelected(false);
    }

    private void setMarqueeDisplay() {
        valueView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        valueView.setHorizontallyScrolling(true);
        valueView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        valueView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
    }

    private void setLinkValueDisplay() {
        valueView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        valueView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        valueView.setHorizontallyScrolling(true);
        valueView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        valueView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        valueView.setTextDirection(View.TEXT_DIRECTION_LTR);
        valueView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        valueView.setSelected(false);
    }

    private boolean isValueOverflowing() {
        CharSequence text = valueView.getText();
        if (text == null || text.length() == 0) {
            return false;
        }

        return valueView.getPaint().measureText(text.toString()) > valueView.getWidth();
    }

    private boolean isUnknown(@Nullable String value) {
        return value == null || value.isEmpty() || Station.UNKNOWN.equals(value);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
