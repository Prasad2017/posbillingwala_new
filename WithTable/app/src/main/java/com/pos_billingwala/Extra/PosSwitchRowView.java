package com.pos_billingwala.Extra;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.pos_billingwala.R;

public class PosSwitchRowView extends FrameLayout {

    private TextView switchLabel;
    private SwitchCompat switchControl;

    public PosSwitchRowView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public PosSwitchRowView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PosSwitchRowView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_pos_switch_row, this, true);
        switchLabel = findViewById(R.id.switchLabel);
        switchControl = findViewById(R.id.switchControl);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PosSwitchRowView);
            CharSequence label = typedArray.getText(R.styleable.PosSwitchRowView_psr_label);
            boolean checked = typedArray.getBoolean(R.styleable.PosSwitchRowView_psr_checked, false);
            typedArray.recycle();

            if (label != null) {
                switchLabel.setText(label);
            }
            switchControl.setChecked(checked);
        }
    }

    public void setLabel(CharSequence label) {
        switchLabel.setText(label);
    }

    public void setChecked(boolean checked) {
        switchControl.setChecked(checked);
    }

    public boolean isChecked() {
        return switchControl.isChecked();
    }

    public void setOnCheckedChangeListener(@Nullable CompoundButton.OnCheckedChangeListener listener) {
        switchControl.setOnCheckedChangeListener(listener);
    }

    public SwitchCompat getSwitchControl() {
        return switchControl;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        switchLabel.setEnabled(enabled);
        switchControl.setEnabled(enabled);
        setAlpha(enabled ? 1f : 0.6f);
    }
}
