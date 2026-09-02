package com.pos_billingwala.Extra;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.pos_billingwala.R;

/**
 * CardView with a consistent 1dp border. Supports dynamic background colors
 * (e.g. selected category chips) while keeping the border visible.
 */
public class PosCardView extends MaterialCardView {

    private final int strokeColor;
    private final int strokeWidthPx;

    public PosCardView(@NonNull Context context) {
        this(context, null);
    }

    public PosCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.materialCardViewStyle);
    }

    public PosCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        strokeWidthPx = getResources().getDimensionPixelSize(R.dimen.card_stroke_width);
        strokeColor = ContextCompat.getColor(context, R.color.colorBorder);
        applyBorderStyle();
    }

    @Override
    public void setCardElevation(float elevation) {
        super.setCardElevation(0f);
    }

    @Override
    public void setMaxCardElevation(float maxElevation) {
        super.setMaxCardElevation(0f);
    }

    private void applyBorderStyle() {
        super.setCardElevation(0f);
        super.setMaxCardElevation(0f);
        setElevation(0f);
        setStrokeWidth(strokeWidthPx);
        setStrokeColor(ColorStateList.valueOf(strokeColor));
    }
}
