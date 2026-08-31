package com.pos_billingwala.Extra;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.pos_billingwala.R;

/**
 * CardView with a consistent 1dp border. Supports dynamic background colors
 * (e.g. selected category chips) while keeping the border visible.
 */
public class PosCardView extends CardView {

    private GradientDrawable borderDrawable;
    private final int strokeColor;
    private final int strokeWidthPx;

    public PosCardView(@NonNull Context context) {
        super(context);
        strokeWidthPx = getResources().getDimensionPixelSize(R.dimen.card_stroke_width);
        strokeColor = ContextCompat.getColor(context, R.color.colorBorder);
        removeShadow();
        applyBorder();
    }

    public PosCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        strokeWidthPx = getResources().getDimensionPixelSize(R.dimen.card_stroke_width);
        strokeColor = ContextCompat.getColor(context, R.color.colorBorder);
        removeShadow();
        applyBorder();
    }

    public PosCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        strokeWidthPx = getResources().getDimensionPixelSize(R.dimen.card_stroke_width);
        strokeColor = ContextCompat.getColor(context, R.color.colorBorder);
        removeShadow();
        applyBorder();
    }

    @Override
    public void setCardElevation(float elevation) {
        super.setCardElevation(0f);
    }

    @Override
    public void setMaxCardElevation(float maxElevation) {
        super.setMaxCardElevation(0f);
    }

    private void removeShadow() {
        super.setCardElevation(0f);
        super.setMaxCardElevation(0f);
        setElevation(0f);
    }

    @Override
    public void setRadius(float radius) {
        super.setRadius(radius);
        applyBorder();
    }

    @Override
    public void setCardBackgroundColor(int color) {
        super.setCardBackgroundColor(color);
        applyBorder();
    }

    @Override
    public void setCardBackgroundColor(@Nullable ColorStateList colors) {
        super.setCardBackgroundColor(colors);
        applyBorder();
    }

    private void applyBorder() {
        if (borderDrawable == null) {
            borderDrawable = new GradientDrawable();
            borderDrawable.setShape(GradientDrawable.RECTANGLE);
        }
        borderDrawable.setCornerRadius(getRadius());
        borderDrawable.setColor(Color.TRANSPARENT);
        borderDrawable.setStroke(strokeWidthPx, strokeColor);
        setForeground(borderDrawable);
    }
}
