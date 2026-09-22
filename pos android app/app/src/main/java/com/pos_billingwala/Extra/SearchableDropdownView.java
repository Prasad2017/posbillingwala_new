package com.pos_billingwala.Extra;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.pos_billingwala.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchableDropdownView extends FrameLayout {

    public interface OnItemSelectedListener {
        void onItemSelected(int position, String label);
    }

    private static final int[] ITEM_ICON_BACKGROUNDS = {
            R.drawable.bg_dropdown_icon_green,
            R.drawable.bg_dropdown_icon_blue,
            R.drawable.bg_dropdown_icon_orange,
            R.drawable.bg_dropdown_icon_pink
    };

    private static final int[] ITEM_ICONS = {
            R.drawable.ic_receipt,
            R.drawable.ic_store,
            R.drawable.ic_accounting,
            R.drawable.ic_layers
    };

    private static final int[] ITEM_ICON_TINTS = {
            R.color.statusActive,
            R.color.colorPrimary,
            R.color.statusTrial,
            R.color.deepPurple
    };

    private TextView dropdownLabel;
    private View dropdownTrigger;
    private FrameLayout dropdownLeadingIconContainer;
    private ImageView dropdownLeadingIcon;
    private TextView dropdownValue;
    private final List<String> items = new ArrayList<>();
    private int selectedIndex = -1;
    private OnItemSelectedListener listener;
    private String placeholder;
    private boolean showIcons;
    private CustomSearchDropdown activeDropdown;

    public SearchableDropdownView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public SearchableDropdownView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SearchableDropdownView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_searchable_dropdown, this, true);
        dropdownLabel = findViewById(R.id.dropdownLabel);
        dropdownTrigger = findViewById(R.id.dropdownTrigger);
        dropdownLeadingIconContainer = findViewById(R.id.dropdownLeadingIconContainer);
        dropdownLeadingIcon = findViewById(R.id.dropdownLeadingIcon);
        dropdownValue = findViewById(R.id.dropdownValue);

        boolean showLabel = false;
        showIcons = false;
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SearchableDropdownView);
            String hint = typedArray.getString(R.styleable.SearchableDropdownView_sdd_hint);
            showLabel = typedArray.getBoolean(R.styleable.SearchableDropdownView_sdd_show_label, false);
            showIcons = typedArray.getBoolean(R.styleable.SearchableDropdownView_sdd_show_icon, false);
            typedArray.recycle();

            placeholder = !TextUtils.isEmpty(hint)
                    ? hint
                    : context.getString(R.string.ui_select_item);

            if (showLabel && !TextUtils.isEmpty(hint)) {
                dropdownLabel.setText(hint);
                dropdownLabel.setVisibility(VISIBLE);
            }
        }

        if (TextUtils.isEmpty(placeholder)) {
            placeholder = context.getString(R.string.ui_select_item);
        }

        View.OnClickListener openPickerListener = v -> {
            if (isEnabled() && !items.isEmpty()) {
                showPicker();
            }
        };
        dropdownTrigger.setOnClickListener(openPickerListener);
        setOnClickListener(openPickerListener);
        applyIconVisibility();
        updateSelectedDisplay();
    }

    public void setShowIcons(boolean showIcons) {
        this.showIcons = showIcons;
        applyIconVisibility();
        updateSelectedDisplay();
    }

    public boolean isShowIcons() {
        return showIcons;
    }

    private void applyIconVisibility() {
        dropdownLeadingIconContainer.setVisibility(showIcons ? VISIBLE : GONE);
        ViewGroup.MarginLayoutParams valueParams =
                (ViewGroup.MarginLayoutParams) dropdownValue.getLayoutParams();
        valueParams.setMarginStart(showIcons ? TabletUi.dpToPx(getContext(), 12) : 0);
        dropdownValue.setLayoutParams(valueParams);
    }

    public void setHint(CharSequence hint) {
        placeholder = hint != null ? hint.toString() : getContext().getString(R.string.ui_select_item);
        updateSelectedDisplay();
    }

    public void setDialogTitle(CharSequence title) {
        // Kept for API compatibility.
    }

    public void setShowLabel(boolean showLabel) {
        if (showLabel && !TextUtils.isEmpty(placeholder)) {
            dropdownLabel.setText(placeholder);
            dropdownLabel.setVisibility(VISIBLE);
        } else {
            dropdownLabel.setVisibility(GONE);
        }
    }

    public void setItems(List<String> labels) {
        items.clear();
        if (labels != null) {
            items.addAll(labels);
        }
        if (selectedIndex >= items.size()) {
            selectedIndex = items.isEmpty() ? -1 : 0;
        }
        updateSelectedDisplay();
    }

    public void setItems(String[] labels) {
        if (labels == null) {
            setItems(new ArrayList<>());
        } else {
            setItems(Arrays.asList(labels));
        }
    }

    public void setSelectedIndex(int index) {
        if (items.isEmpty()) {
            selectedIndex = -1;
        } else {
            selectedIndex = Math.max(0, Math.min(index, items.size() - 1));
        }
        updateSelectedDisplay();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    @Nullable
    public String getSelectedLabel() {
        if (selectedIndex < 0 || selectedIndex >= items.size()) {
            return null;
        }
        return items.get(selectedIndex);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        dropdownTrigger.setEnabled(enabled);
        dropdownTrigger.setAlpha(enabled ? 1f : 0.6f);
    }

    @Override
    protected void onDetachedFromWindow() {
        dismissPicker();
        super.onDetachedFromWindow();
    }

    private void updateSelectedDisplay() {
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            dropdownValue.setText(items.get(selectedIndex));
            dropdownValue.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextPrimary));
            if (showIcons) {
                applyLeadingIcon(selectedIndex);
            }
        } else {
            dropdownValue.setText(placeholder);
            dropdownValue.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextHint));
            if (showIcons) {
                dropdownLeadingIconContainer.setBackground(
                        ContextCompat.getDrawable(getContext(), R.drawable.bg_dropdown_leading_icon));
                dropdownLeadingIcon.setImageResource(R.drawable.ic_layers);
                dropdownLeadingIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            }
        }
    }

    private void applyLeadingIcon(int index) {
        int styleIndex = index % ITEM_ICON_BACKGROUNDS.length;
        dropdownLeadingIconContainer.setBackground(
                ContextCompat.getDrawable(getContext(), ITEM_ICON_BACKGROUNDS[styleIndex]));
        dropdownLeadingIcon.setImageResource(ITEM_ICONS[styleIndex]);
        dropdownLeadingIcon.setColorFilter(ContextCompat.getColor(getContext(), ITEM_ICON_TINTS[styleIndex]));
    }

    private void showPicker() {
        dismissPicker();

        CustomSearchDropdown dropdown = new CustomSearchDropdown(getContext(), dropdownTrigger);
        dropdown.setShowIcons(showIcons);
        dropdown.setItems(items);
        String selected = getSelectedLabel();
        if (!TextUtils.isEmpty(selected)) {
            dropdown.setSelectedItem(selected);
        }
        dropdown.setOnItemSelectedListener((item, position) -> selectItem(position, item, true));
        activeDropdown = dropdown;
        dropdown.show();
    }

    private void dismissPicker() {
        if (activeDropdown != null) {
            activeDropdown.dismiss();
            activeDropdown = null;
        }
    }

    private void selectItem(int index, String label, boolean notifyListener) {
        selectedIndex = index;
        dropdownValue.setText(label);
        dropdownValue.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextPrimary));
        if (showIcons) {
            applyLeadingIcon(index);
        }
        if (notifyListener && listener != null) {
            listener.onItemSelected(index, label);
        }
    }
}
