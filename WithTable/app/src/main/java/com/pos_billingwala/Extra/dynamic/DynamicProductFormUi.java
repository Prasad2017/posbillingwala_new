package com.pos_billingwala.Extra.dynamic;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.pos_billingwala.Extra.FeatureEngine;
import com.pos_billingwala.Extra.FeatureFlags;
import com.pos_billingwala.Extra.LicenseModules;
import com.pos_billingwala.Extra.dynamicui.UiCodes;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.IncludeProductFormBodyBinding;

import org.json.JSONObject;

/**
 * Applies Dynamic product-form field visibility and ensures optional business fields
 * (serial / warranty / flavour) exist on the shared form include (Phase 04/05 close-out).
 */
public final class DynamicProductFormUi {

    private DynamicProductFormUi() {
    }

    public static void apply(Context context, @Nullable IncludeProductFormBodyBinding body) {
        if (context == null || body == null) {
            return;
        }
        set(body.subcategorySection, FormRegistry.isProductFieldVisible(context, UiCodes.PF_SUBCATEGORY));
        boolean code = FormRegistry.isProductFieldVisible(context, UiCodes.PF_SKU)
                || FormRegistry.isProductFieldVisible(context, UiCodes.PF_BARCODE);
        set(body.productCodeLayout, code);
        set(body.unitDropdown, true);
        boolean openPrice = FeatureEngine.currentTemplate(context) != null
                && FeatureEngine.currentTemplate(context).supports(FeatureFlags.OPEN_PRICE);
        set(body.openPriceSwitch, openPrice);
        set(body.productGstSection, FeatureEngine.isEnabled(context, FeatureFlags.GST)
                && FormRegistry.isProductFieldVisible(context, UiCodes.PF_TAX));
        set(body.productPriceSection, FormRegistry.isProductFieldVisible(context, UiCodes.PF_PRICE));
        View portion = body.getRoot().findViewById(R.id.productPortionSectionInclude);
        if (portion != null) {
            LicenseModules.setVisible(portion,
                    FeatureEngine.isEnabled(context, FeatureFlags.PORTIONS)
                            && FormRegistry.isProductFieldVisible(context, UiCodes.PF_PORTION));
        }

        LinearLayout extras = ensureExtraSection(context, body.getRoot());
        boolean showSerial = FormRegistry.isProductFieldVisible(context, UiCodes.PF_SERIAL);
        boolean showWarranty = FormRegistry.isProductFieldVisible(context, UiCodes.PF_WARRANTY);
        boolean showFlavour = FormRegistry.isProductFieldVisible(context, UiCodes.PF_FLAVOUR)
                || FormRegistry.isProductFieldVisible(context, UiCodes.PF_CUSTOM_ORDER);
        View serial = extras.findViewById(R.id.productSerial);
        View warranty = extras.findViewById(R.id.productWarranty);
        View flavour = extras.findViewById(R.id.productFlavour);
        set(serial, showSerial);
        set(warranty, showWarranty);
        set(flavour, showFlavour);
        LicenseModules.setVisible(extras, showSerial || showWarranty || showFlavour);
    }

    public static String readExtraAttrsJson(Context context, @Nullable IncludeProductFormBodyBinding body) {
        if (body == null) {
            return "";
        }
        View root = body.getRoot();
        EditText serial = root.findViewById(R.id.productSerial);
        EditText warranty = root.findViewById(R.id.productWarranty);
        EditText flavour = root.findViewById(R.id.productFlavour);
        try {
            JSONObject o = new JSONObject();
            put(o, "serial", text(serial));
            put(o, "warranty", text(warranty));
            put(o, "flavour", text(flavour));
            if (o.length() == 0) {
                return "";
            }
            return o.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static void bindExtraAttrs(@Nullable IncludeProductFormBodyBinding body, @Nullable String json) {
        if (body == null || TextUtils.isEmpty(json)) {
            return;
        }
        ensureExtraSection(body.getRoot().getContext(), body.getRoot());
        try {
            JSONObject o = new JSONObject(json);
            EditText serial = body.getRoot().findViewById(R.id.productSerial);
            EditText warranty = body.getRoot().findViewById(R.id.productWarranty);
            EditText flavour = body.getRoot().findViewById(R.id.productFlavour);
            if (serial != null) {
                serial.setText(o.optString("serial", ""));
            }
            if (warranty != null) {
                warranty.setText(o.optString("warranty", ""));
            }
            if (flavour != null) {
                flavour.setText(o.optString("flavour", ""));
            }
        } catch (Exception ignored) {
        }
    }

    private static void put(JSONObject o, String key, String value) throws Exception {
        if (value != null && !value.trim().isEmpty()) {
            o.put(key, value.trim());
        }
    }

    private static String text(@Nullable EditText edit) {
        return edit != null && edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private static LinearLayout ensureExtraSection(Context context, ViewGroup root) {
        View existing = root.findViewById(R.id.productExtraSection);
        if (existing instanceof LinearLayout) {
            return (LinearLayout) existing;
        }
        float density = context.getResources().getDisplayMetrics().density;
        LinearLayout section = new LinearLayout(context);
        section.setId(R.id.productExtraSection);
        section.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * density);
        section.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (12 * density);
        section.setLayoutParams(lp);
        section.setBackgroundResource(R.drawable.bg_button_outline);

        TextView title = new TextView(context);
        title.setText(R.string.product_extra_section_title);
        title.setTextColor(ContextCompat.getColor(context, R.color.colorTextSecondary));
        title.setTextSize(12);
        try {
            title.setTypeface(ResourcesCompat.getFont(context, R.font.poppinsbold));
        } catch (Exception ignored) {
        }
        section.addView(title);

        section.addView(field(context, R.id.productSerial, R.string.electronics_serial_hint));
        section.addView(field(context, R.id.productWarranty, R.string.electronics_warranty_hint));
        section.addView(field(context, R.id.productFlavour, R.string.product_flavour_hint));
        root.addView(section);
        return section;
    }

    private static EditText field(Context context, int id, int hintRes) {
        float density = context.getResources().getDisplayMetrics().density;
        EditText edit = new EditText(context);
        edit.setId(id);
        edit.setHint(hintRes);
        edit.setSingleLine(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (8 * density);
        edit.setLayoutParams(lp);
        return edit;
    }

    private static void set(@Nullable View view, boolean visible) {
        if (view == null) {
            return;
        }
        LicenseModules.setVisible(view, visible);
    }
}
