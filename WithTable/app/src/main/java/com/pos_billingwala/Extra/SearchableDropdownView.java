package com.pos_billingwala.Extra;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Extra.BottomSheetUi;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pos_billingwala.Adapter.SearchableDropdownAdapter;
import com.pos_billingwala.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchableDropdownView extends FrameLayout {

    public interface OnItemSelectedListener {
        void onItemSelected(int position, String label);
    }

    private TextInputLayout dropdownInputLayout;
    private TextInputEditText dropdownValue;
    private final List<String> items = new ArrayList<>();
    private int selectedIndex = -1;
    private OnItemSelectedListener listener;
    private String dialogTitle;
    private BottomSheetDialog activeDialog;

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
        dropdownInputLayout = findViewById(R.id.dropdownInputLayout);
        dropdownValue = findViewById(R.id.dropdownValue);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SearchableDropdownView);
            String hint = typedArray.getString(R.styleable.SearchableDropdownView_sdd_hint);
            dialogTitle = typedArray.getString(R.styleable.SearchableDropdownView_sdd_dialog_title);
            typedArray.recycle();
            if (!TextUtils.isEmpty(hint)) {
                dropdownInputLayout.setHint(hint);
            }
        }

        if (TextUtils.isEmpty(dialogTitle)) {
            dialogTitle = dropdownInputLayout.getHint() != null
                    ? dropdownInputLayout.getHint().toString()
                    : getContext().getString(R.string.ui_select_item);
        }

        View.OnClickListener openPickerListener = v -> {
            if (isEnabled() && !items.isEmpty()) {
                showPickerDialog();
            }
        };
        setOnClickListener(openPickerListener);
        dropdownInputLayout.setOnClickListener(openPickerListener);
        dropdownValue.setOnClickListener(openPickerListener);
        dropdownInputLayout.setEndIconOnClickListener(openPickerListener);
    }

    public void setHint(CharSequence hint) {
        dropdownInputLayout.setHint(hint);
        if (TextUtils.isEmpty(dialogTitle) || dialogTitle.equals(getContext().getString(R.string.ui_select_item))) {
            dialogTitle = hint != null ? hint.toString() : dialogTitle;
        }
    }

    public void setDialogTitle(CharSequence title) {
        dialogTitle = title != null ? title.toString() : dialogTitle;
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
        dropdownInputLayout.setEnabled(enabled);
        dropdownValue.setEnabled(enabled);
        dropdownInputLayout.setAlpha(enabled ? 1f : 0.6f);
    }

    private void updateSelectedDisplay() {
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            dropdownValue.setText(items.get(selectedIndex));
        } else {
            dropdownValue.setText("");
        }
    }

    private void showPickerDialog() {
        if (activeDialog != null && activeDialog.isShowing()) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getRootView().getWindowToken(), 0);
        }

        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View sheetView = LayoutInflater.from(getContext())
                .inflate(R.layout.bottom_sheet_searchable_dropdown, null, false);
        dialog.setContentView(sheetView);
        activeDialog = dialog;

        TextView sheetTitle = sheetView.findViewById(R.id.sheetTitle);
        TextInputEditText searchInput = sheetView.findViewById(R.id.searchInput);
        TextView noResultsText = sheetView.findViewById(R.id.noResultsText);
        RecyclerView optionsList = sheetView.findViewById(R.id.optionsList);

        sheetTitle.setText(dialogTitle);
        optionsList.setLayoutManager(new LinearLayoutManager(getContext()));

        SearchableDropdownAdapter adapter = new SearchableDropdownAdapter(items, (originalIndex, label) -> {
            selectItem(originalIndex, label, true);
            dialog.dismiss();
        });
        optionsList.setAdapter(adapter);

        Runnable refreshEmptyState = () -> {
            boolean hasResults = adapter.getFilteredCount() > 0;
            noResultsText.setVisibility(hasResults ? View.GONE : View.VISIBLE);
            optionsList.setVisibility(hasResults ? View.VISIBLE : View.GONE);
        };
        refreshEmptyState.run();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s != null ? s.toString() : "");
                refreshEmptyState.run();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).setSkipCollapsed(true);
            }
            BottomSheetUi.applyFullWidth(dialog);
            searchInput.requestFocus();
        });
        dialog.setOnDismissListener(d -> activeDialog = null);
        dialog.show();
    }

    private void selectItem(int index, String label, boolean notifyListener) {
        selectedIndex = index;
        dropdownValue.setText(label);
        if (notifyListener && listener != null) {
            listener.onItemSelected(index, label);
        }
    }
}
