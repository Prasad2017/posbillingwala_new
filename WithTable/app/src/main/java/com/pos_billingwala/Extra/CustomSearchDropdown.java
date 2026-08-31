package com.pos_billingwala.Extra;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.pos_billingwala.R;

import java.util.ArrayList;
import java.util.List;

public class CustomSearchDropdown {

    public interface OnItemSelectedListener {
        void onItemSelected(String item, int position);
    }

    private static final int SEARCH_LIMIT = 6;
    private static final int MAX_LIST_HEIGHT_DP = 280;
    private static final int EMPTY_STATE_HEIGHT_DP = 72;

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

    private final Context context;
    private final View anchorView;

    private final List<String> originalItems = new ArrayList<>();
    private final List<String> filteredItems = new ArrayList<>();

    private PopupWindow popupWindow;
    private EditText searchEditText;
    private LinearLayout listContainer;
    private ScrollView scrollView;
    private LinearLayout root;

    private String selectedItem = "";
    private boolean showIcons;
    private OnItemSelectedListener listener;

    public CustomSearchDropdown(Context context, View anchorView) {
        this.context = context;
        this.anchorView = anchorView;
    }

    public void setItems(List<String> items) {
        originalItems.clear();
        if (items != null) {
            originalItems.addAll(items);
        }
        filteredItems.clear();
        filteredItems.addAll(originalItems);
    }

    public void setSelectedItem(String item) {
        selectedItem = item != null ? item : "";
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.listener = listener;
    }

    public void setShowIcons(boolean showIcons) {
        this.showIcons = showIcons;
    }

    public void show() {
        if (originalItems.isEmpty()) {
            return;
        }

        LinearLayout root = new LinearLayout(context);
        this.root = root;
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = TabletUi.dpToPx(context, 8);
        root.setPadding(padding, padding, padding, padding);
        root.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_dropdown_popup));

        boolean showSearch = originalItems.size() >= SEARCH_LIMIT;
        if (showSearch) {
            root.addView(buildSearchBar(), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams listSectionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        listSectionParams.topMargin = TabletUi.dpToPx(context, 6);

        if (showSearch) {
            scrollView = new ScrollView(context);
            scrollView.setVerticalScrollBarEnabled(true);
            scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            scrollView.addView(listContainer, new ScrollView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            root.addView(scrollView, listSectionParams);
        } else {
            scrollView = null;
            root.addView(listContainer, listSectionParams);
        }

        renderItems();

        int popupWidth = getAnchorWidth() > 0
                ? getAnchorWidth()
                : ViewGroup.LayoutParams.MATCH_PARENT;

        popupWindow = new PopupWindow(
                root,
                popupWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(TabletUi.dpToPx(context, 10));
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_dropdown_popup));
        popupWindow.showAsDropDown(anchorView, 0, TabletUi.dpToPx(context, 6));

        if (searchEditText != null) {
            searchEditText.requestFocus();
            searchEditText.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 150);
        }
    }

    private View buildSearchBar() {
        LinearLayout searchContainer = new LinearLayout(context);
        searchContainer.setGravity(Gravity.CENTER_VERTICAL);
        int searchPadH = TabletUi.dpToPx(context, 12);
        searchContainer.setPadding(searchPadH, TabletUi.dpToPx(context, 8), searchPadH, TabletUi.dpToPx(context, 8));
        searchContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_dropdown_search));

        ImageView searchIcon = new ImageView(context);
        searchIcon.setImageResource(R.drawable.ic_search);
        searchIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorTextSecondary));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                TabletUi.dpToPx(context, 18),
                TabletUi.dpToPx(context, 18));
        iconParams.gravity = Gravity.CENTER_VERTICAL;
        iconParams.setMarginEnd(TabletUi.dpToPx(context, 8));
        searchContainer.addView(searchIcon, iconParams);

        searchEditText = new EditText(context);
        searchEditText.setHint(context.getString(R.string.ui_search_dropdown));
        searchEditText.setTextSize(14);
        searchEditText.setSingleLine(true);
        searchEditText.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        Typeface typeface = ResourcesCompat.getFont(context, R.font.poppinsregular);
        if (typeface != null) {
            searchEditText.setTypeface(typeface);
        }
        searchEditText.setHintTextColor(ContextCompat.getColor(context, R.color.colorTextHint));
        searchEditText.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary));

        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1);
        searchContainer.addView(searchEditText, searchParams);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return searchContainer;
    }

    private void filter(String query) {
        filteredItems.clear();
        String search = query.trim().toLowerCase();
        for (String item : originalItems) {
            if (item.toLowerCase().contains(search)) {
                filteredItems.add(item);
            }
        }
        renderItems();
    }

    private int getAnchorWidth() {
        int width = anchorView.getWidth();
        if (width <= 0) {
            width = anchorView.getMeasuredWidth();
        }
        return width;
    }

    private int getMeasureWidth() {
        int width = getAnchorWidth();
        return width > 0 ? width : TabletUi.dpToPx(context, 280);
    }

    private void updateScrollHeight() {
        if (scrollView == null || listContainer == null) {
            return;
        }

        int widthSpec = View.MeasureSpec.makeMeasureSpec(getMeasureWidth(), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        listContainer.measure(widthSpec, heightSpec);

        int contentHeight = listContainer.getMeasuredHeight();
        if (contentHeight <= 0) {
            contentHeight = TabletUi.dpToPx(context, EMPTY_STATE_HEIGHT_DP);
        }

        int maxHeight = TabletUi.dpToPx(context, MAX_LIST_HEIGHT_DP);
        int height = Math.min(contentHeight, maxHeight);

        ViewGroup.LayoutParams layoutParams = scrollView.getLayoutParams();
        layoutParams.height = height;
        scrollView.setLayoutParams(layoutParams);
        scrollView.requestLayout();
        if (root != null) {
            root.requestLayout();
        }
        if (popupWindow != null && popupWindow.isShowing()) {
            int popupWidth = getAnchorWidth() > 0
                    ? getAnchorWidth()
                    : ViewGroup.LayoutParams.MATCH_PARENT;
            popupWindow.update(popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void renderItems() {
        if (listContainer == null) {
            return;
        }

        listContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);

        if (filteredItems.isEmpty()) {
            TextView emptyView = new TextView(context);
            emptyView.setText(context.getString(R.string.ui_no_results_found));
            emptyView.setTextSize(14);
            emptyView.setTextColor(ContextCompat.getColor(context, R.color.colorTextSecondary));
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(
                    TabletUi.dpToPx(context, 10),
                    TabletUi.dpToPx(context, 24),
                    TabletUi.dpToPx(context, 10),
                    TabletUi.dpToPx(context, 24));
            listContainer.addView(emptyView);
            updateScrollHeight();
            return;
        }

        for (int i = 0; i < filteredItems.size(); i++) {
            String item = filteredItems.get(i);
            int originalIndex = originalItems.indexOf(item);
            View row = inflater.inflate(R.layout.item_custom_dropdown_option, listContainer, false);

            FrameLayout iconContainer = row.findViewById(R.id.optionIconContainer);
            ImageView icon = row.findViewById(R.id.optionIcon);
            TextView label = row.findViewById(R.id.optionLabel);
            ImageView check = row.findViewById(R.id.optionCheck);

            if (showIcons) {
                int styleIndex = Math.max(0, originalIndex) % ITEM_ICON_BACKGROUNDS.length;
                iconContainer.setVisibility(View.VISIBLE);
                iconContainer.setBackground(ContextCompat.getDrawable(context, ITEM_ICON_BACKGROUNDS[styleIndex]));
                icon.setImageResource(ITEM_ICONS[styleIndex]);
                icon.setColorFilter(ContextCompat.getColor(context, ITEM_ICON_TINTS[styleIndex]));

                ViewGroup.MarginLayoutParams labelParams =
                        (ViewGroup.MarginLayoutParams) label.getLayoutParams();
                labelParams.setMarginStart(TabletUi.dpToPx(context, 12));
                label.setLayoutParams(labelParams);
            } else {
                iconContainer.setVisibility(View.GONE);

                ViewGroup.MarginLayoutParams labelParams =
                        (ViewGroup.MarginLayoutParams) label.getLayoutParams();
                labelParams.setMarginStart(0);
                label.setLayoutParams(labelParams);
            }

            label.setText(item);

            boolean selected = item.equals(selectedItem);
            if (selected) {
                row.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_dropdown_item_selected));
                label.setTypeface(label.getTypeface(), Typeface.BOLD);
                check.setVisibility(View.VISIBLE);
            } else {
                row.setBackgroundResource(android.R.color.transparent);
                check.setVisibility(View.GONE);
            }

            row.setOnClickListener(v -> {
                selectedItem = item;
                if (listener != null) {
                    listener.onItemSelected(item, originalIndex);
                }
                dismiss();
            });

            listContainer.addView(row);
        }

        updateScrollHeight();
    }

    public void dismiss() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    public boolean isShowing() {
        return popupWindow != null && popupWindow.isShowing();
    }
}
