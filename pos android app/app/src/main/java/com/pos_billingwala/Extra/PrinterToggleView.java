package com.pos_billingwala.Extra;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.pos_billingwala.R;

public class PrinterToggleView extends FrameLayout {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(PrinterToggleView view, boolean isChecked);
    }

    private FrameLayout track;
    private boolean checked;
    private boolean suppressListener;
    private OnCheckedChangeListener listener;

    public PrinterToggleView(Context context) {
        super(context);
        init(context);
    }

    public PrinterToggleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PrinterToggleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_printer_toggle, this, true);
        track = findViewById(R.id.toggleTrack);
        setClickable(true);
        setFocusable(true);
        setOnClickListener(v -> setChecked(!checked, true));
        applyCheckedState(false);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        setChecked(checked, false);
    }

    public void setChecked(boolean checked, boolean fromUser) {
        if (this.checked == checked) {
            applyCheckedState(checked);
            return;
        }
        this.checked = checked;
        applyCheckedState(checked);
        if (!suppressListener && listener != null) {
            listener.onCheckedChanged(this, checked);
        }
    }

    public void setCheckedSilently(boolean checked) {
        suppressListener = true;
        setChecked(checked, false);
        suppressListener = false;
    }

    private void applyCheckedState(boolean on) {
        track.setBackgroundResource(on
                ? R.drawable.bg_printer_toggle_track_on
                : R.drawable.bg_printer_toggle_track_off);
        android.view.View thumb = findViewById(R.id.toggleThumb);
        LayoutParams lp = (LayoutParams) thumb.getLayoutParams();
        lp.gravity = on ? Gravity.END | Gravity.CENTER_VERTICAL : Gravity.START | Gravity.CENTER_VERTICAL;
        thumb.setLayoutParams(lp);
    }
}
