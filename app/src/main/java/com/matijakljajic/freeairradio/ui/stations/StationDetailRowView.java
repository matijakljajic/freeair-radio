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
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.ui.util.MarqueeTextView;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

@SuppressWarnings("unused")
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

        dividerView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, UiDimensions.px(getContext(), R.dimen.detail_row_divider_thickness)));
        dividerView.setBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline));
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
        applyStyle(true);
    }

    public void useTallStyle() {
        applyStyle(false);
    }

    private void applyStyle(boolean compact) {
        contentContainer.removeAllViews();
        contentContainer.setOrientation(compact ? HORIZONTAL : VERTICAL);
        contentContainer.setGravity(compact ? Gravity.CENTER_VERTICAL : Gravity.NO_GRAVITY);
        contentContainer.setPadding(
                UiDimensions.px(getContext(), R.dimen.detail_row_horizontal_padding),
                UiDimensions.px(getContext(), R.dimen.detail_row_vertical_padding),
                UiDimensions.px(getContext(), R.dimen.detail_row_horizontal_padding),
                UiDimensions.px(getContext(), compact
                        ? R.dimen.detail_row_vertical_padding
                        : R.dimen.detail_row_vertical_padding_tall_bottom));

        LayoutParams labelParams = new LayoutParams(
                compact ? LayoutParams.WRAP_CONTENT : LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        LayoutParams valueParams = new LayoutParams(
                compact ? 0 : LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
                compact ? 1f : 0f
        );
        if (compact) {
            labelParams.rightMargin = UiDimensions.px(getContext(), R.dimen.detail_row_label_value_spacing);
        } else {
            valueParams.topMargin = UiDimensions.px(getContext(), R.dimen.detail_row_tall_value_spacing);
        }
        contentContainer.addView(labelView, labelParams);
        contentContainer.addView(valueView, valueParams);
    }

    public void bindCompactText(@StringRes int labelResId, @NonNull String value) {
        useCompactStyle();
        bindText(labelResId, value);
    }

    public void bindTallText(@StringRes int labelResId, @NonNull String value) {
        useTallStyle();
        bindText(labelResId, value);
    }

    public void bindCompactBitrate(@StringRes int labelResId, int bitrate) {
        useCompactStyle();
        bindBitrate(labelResId, bitrate);
    }

    public void bindTallLink(@StringRes int labelResId,
                             @Nullable String value,
                             @Nullable OnClickListener onClickListener) {
        useTallStyle();
        bindLink(labelResId, value, onClickListener);
    }

    private void bindText(@StringRes int labelResId, @NonNull String value) {
        if (isUnknown(value)) {
            hide();
            return;
        }

        bindStaticValue(labelResId, value);
    }

    private void bindBitrate(@StringRes int labelResId, int bitrate) {
        if (bitrate <= 0) {
            hide();
            return;
        }

        String bitrateText = getResources().getString(com.matijakljajic.freeairradio.R.string.station_details_bitrate_value, bitrate);
        bindStaticValue(labelResId, bitrateText);
    }

    private void bindLink(@StringRes int labelResId,
                          @Nullable String value,
                          @Nullable OnClickListener onClickListener) {
        if (isUnknown(value)) {
            hide();
            return;
        }

        resetBoundState();
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
    }

    public void restartMarquee() {
        valueView.setSelected(false);
        valueView.setSelected(true);
    }

    public void hide() {
        setVisibility(GONE);
    }

    private void bindOverflowMarqueeLongClick() {
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

    private void bindStaticValue(@StringRes int labelResId, @NonNull String value) {
        resetBoundState();
        setVisibility(VISIBLE);
        labelView.setText(labelResId);
        setStaticValueDisplay();
        valueView.setText(value);
        valueView.setContentDescription(value);
        bindOverflowMarqueeLongClick();
    }

    private void resetBoundState() {
        clearValueClickState();
        valueView.setSelected(false);
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
        bindOverflowMarqueeLongClick();
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

}
