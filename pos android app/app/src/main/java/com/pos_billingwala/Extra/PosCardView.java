package com.pos_billingwala.Extra;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.pos_billingwala.R;

/**
 * CardView with a consistent 1dp border. Supports dynamic background colors
 * (e.g. selected category chips) while keeping the border visible.
 * XML app:strokeWidth / app:strokeColor override the defaults when set.
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
        int width = getResources().getDimensionPixelSize(R.dimen.card_stroke_width);
        int color = ContextCompat.getColor(context, R.color.colorCardStroke);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(
                    attrs, com.google.android.material.R.styleable.MaterialCardView, defStyleAttr, 0);
            try {
                if (a.hasValue(com.google.android.material.R.styleable.MaterialCardView_strokeWidth)) {
                    width = a.getDimensionPixelSize(
                            com.google.android.material.R.styleable.MaterialCardView_strokeWidth, width);
                }
                if (a.hasValue(com.google.android.material.R.styleable.MaterialCardView_strokeColor)) {
                    ColorStateList csl = a.getColorStateList(
                            com.google.android.material.R.styleable.MaterialCardView_strokeColor);
                    if (csl != null) {
                        color = csl.getDefaultColor();
                    }
                }
            } finally {
                a.recycle();
            }
        }

        strokeWidthPx = width;
        strokeColor = color;
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
