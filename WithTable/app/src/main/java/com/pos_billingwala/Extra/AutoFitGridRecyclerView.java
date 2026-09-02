package com.pos_billingwala.Extra;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Extra.ResponsiveUi;

@SuppressLint("ResourceType")
public class AutoFitGridRecyclerView extends RecyclerView {
    public GridLayoutManager manager;
    public int columnWidth = -1;

    public AutoFitGridRecyclerView(Context context) {
        super(context);
        initialization(context, null);
    }

    public AutoFitGridRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialization(context, attrs);
    }

    public AutoFitGridRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialization(context, attrs);
    }

    public void initialization(Context context, AttributeSet attrs) {
        if (attrs != null) {
            // list the attributes we want to fetch
            int[] attrsArray = {
                    android.R.attr.columnWidth
            };
            TypedArray array = context.obtainStyledAttributes(attrs, attrsArray);
            //retrieve the value of the 0 index, which is columnWidth
            columnWidth = array.getDimensionPixelSize(0, -1);
            array.recycle();
        }
        manager = new GridLayoutManager(context, 1);
        setLayoutManager(manager);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (columnWidth > 0) {
        int minCardDp = (int) (columnWidth / getContext().getResources().getDisplayMetrics().density);
            int spanCount = ResponsiveUi.gridColumnCountForWidthPx(getContext(), getMeasuredWidth(), minCardDp);
            manager.setSpanCount(spanCount);
        }
    }
}
