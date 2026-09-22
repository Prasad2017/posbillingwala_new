package com.pos_billingwala.Extra;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Adapter.ProductPortionDraftAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.PortionMasterResponse;
import com.pos_billingwala.Model.ProductPortionDraft;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Inline optional portions on Add/Edit Product — separate from Category/Subcategory.
 */
public class ProductPortionSectionHelper {

    private final Activity activity;
    private final POSBillingWalaDatabase database;
    private final View root;
    private final TextView managePortionMasterLink;
    private final TextView portionSectionHint;
    private final View portionMasterPickerSection;
    private final TextView noPortionMasterHint;
    private final SearchableDropdownView portionMasterDropdown;
    private final TextInputEditText inlinePortionPrice;
    private final TextView addInlinePortion;
    private final View inlinePortionListCard;
    private final RecyclerView inlinePortionRecyclerview;

    private final List<ProductPortionDraft> drafts = new ArrayList<>();
    private ProductPortionDraftAdapter adapter;
    private List<PortionMasterResponse> portionMasterList = new ArrayList<>();
    private String[] portionMasterIdList;
    private String[] portionMasterNameList;
    private String selectedPortionMasterId;
    private boolean portionMasterPicked;
    private Runnable onPortionMasterLinkClick;
    private Runnable onPortionsChanged;

    public ProductPortionSectionHelper(Activity activity, POSBillingWalaDatabase database, View root) {
        this.activity = activity;
        this.database = database;
        this.root = root;
        this.managePortionMasterLink = root.findViewById(R.id.managePortionMasterLink);
        this.portionSectionHint = root.findViewById(R.id.portionSectionHint);
        this.portionMasterPickerSection = root.findViewById(R.id.portionMasterPickerSection);
        this.noPortionMasterHint = root.findViewById(R.id.noPortionMasterHint);
        this.portionMasterDropdown = root.findViewById(R.id.portionMasterDropdown);
        this.inlinePortionPrice = root.findViewById(R.id.inlinePortionPrice);
        this.addInlinePortion = root.findViewById(R.id.addInlinePortion);
        this.inlinePortionListCard = root.findViewById(R.id.inlinePortionListCard);
        this.inlinePortionRecyclerview = root.findViewById(R.id.inlinePortionRecyclerview);

        adapter = new ProductPortionDraftAdapter(activity, drafts, this::removeDraftAt);
        inlinePortionRecyclerview.setLayoutManager(new GridLayoutManager(activity, 1));
        inlinePortionRecyclerview.setAdapter(adapter);

        addInlinePortion.setOnClickListener(v -> addDraftFromForm());
        managePortionMasterLink.setOnClickListener(v -> {
            if (onPortionMasterLinkClick != null) {
                onPortionMasterLinkClick.run();
            }
        });
    }

    public void setOnPortionMasterLinkClick(Runnable onPortionMasterLinkClick) {
        this.onPortionMasterLinkClick = onPortionMasterLinkClick;
    }

    public void setOnPortionsChanged(Runnable onPortionsChanged) {
        this.onPortionsChanged = onPortionsChanged;
    }

    public void refresh() {
        setupPortionMasterSpinner();
        refreshDraftList();
        updatePriceHint();
    }

    public void loadExistingForProduct(String productId) {
        drafts.clear();
        if (productId != null && !productId.trim().isEmpty()) {
            List<ProductPortionResponse> existing = database.getProductPortionList(productId);
            for (ProductPortionResponse row : existing) {
                drafts.add(ProductPortionDraft.fromResponse(row));
            }
        }
        refresh();
    }

    public boolean hasPortions() {
        return !drafts.isEmpty();
    }

    public boolean shouldHideProductCost() {
        return hasPortions() || portionMasterPicked;
    }

    public List<ProductPortionDraft> getDrafts() {
        return drafts;
    }

    public void updatePriceHint() {
        if (portionSectionHint != null) {
            if (hasPortions()) {
                portionSectionHint.setText(R.string.product_price_optional_with_portions);
            } else {
                portionSectionHint.setText(R.string.product_price_required_no_portion);
            }
        }
        notifyPortionsChanged();
    }

    private void notifyPortionsChanged() {
        applyProductCostVisibility();
        if (onPortionsChanged != null) {
            onPortionsChanged.run();
        }
    }

    private void applyProductCostVisibility() {
        if (root == null) {
            return;
        }
        int visibility = shouldHideProductCost() ? View.GONE : View.VISIBLE;
        setViewVisibility(R.id.productPriceSection, visibility);
        setViewVisibility(R.id.productPriceLayout, visibility);
        View productPrice = root.findViewById(R.id.productPrice);
        if (productPrice != null) {
            productPrice.clearFocus();
            productPrice.setVisibility(visibility);
        }
        setViewVisibility(R.id.productGstSection, visibility);
        setViewVisibility(R.id.productCGSTLayout, visibility);
        setViewVisibility(R.id.productSGSTLayout, visibility);
    }

    private void setViewVisibility(int viewId, int visibility) {
        View target = root.findViewById(viewId);
        if (target != null) {
            target.setVisibility(visibility);
        }
    }

    private void setupPortionMasterSpinner() {
        portionMasterList = database.getPortionMasterList();
        if (portionMasterList.isEmpty()) {
            portionMasterPickerSection.setVisibility(View.GONE);
            noPortionMasterHint.setVisibility(View.VISIBLE);
            selectedPortionMasterId = null;
            portionMasterPicked = false;
            notifyPortionsChanged();
            return;
        }

        noPortionMasterHint.setVisibility(View.GONE);
        portionMasterPickerSection.setVisibility(View.VISIBLE);
        portionMasterIdList = new String[portionMasterList.size()];
        portionMasterNameList = new String[portionMasterList.size()];
        for (int i = 0; i < portionMasterList.size(); i++) {
            portionMasterIdList[i] = portionMasterList.get(i).getPortionMasterId();
            portionMasterNameList[i] = portionMasterList.get(i).getPortionName();
        }

        portionMasterDropdown.setItems(portionMasterNameList);
        portionMasterDropdown.setSelectedIndex(0);
        selectedPortionMasterId = portionMasterIdList[0];
        portionMasterDropdown.setOnItemSelectedListener((position, item) -> {
            selectedPortionMasterId = portionMasterIdList[position];
            portionMasterPicked = true;
            notifyPortionsChanged();
        });
    }

    private void addDraftFromForm() {
        if (selectedPortionMasterId == null || selectedPortionMasterId.isEmpty()) {
            Toast.makeText(activity, R.string.no_portion_master_for_product, Toast.LENGTH_SHORT).show();
            return;
        }
        String price = inlinePortionPrice.getText() != null
                ? inlinePortionPrice.getText().toString().trim() : "";
        if (price.isEmpty()) {
            Toast.makeText(activity, activity.getString(R.string.toast_please_enter_portion_price), Toast.LENGTH_SHORT).show();
            return;
        }

        PortionMasterResponse master = database.getPortionMasterById(selectedPortionMasterId);
        if (master == null) {
            Toast.makeText(activity, activity.getString(R.string.toast_invalid_portion), Toast.LENGTH_SHORT).show();
            return;
        }

        for (ProductPortionDraft draft : drafts) {
            if (selectedPortionMasterId.equals(draft.getPortionMasterId())) {
                draft.setPortionPrice(price);
                draft.setPortionName(master.getPortionName());
                Toast.makeText(activity, activity.getString(R.string.toast_portion_price_updated), Toast.LENGTH_SHORT).show();
                inlinePortionPrice.setText("");
                refreshDraftList();
                updatePriceHint();
                return;
            }
        }

        ProductPortionDraft draft = new ProductPortionDraft(
                selectedPortionMasterId,
                master.getPortionName(),
                price,
                drafts.size() + 1);
        draft.setPortionNetworkStatus(randomKey(10));
        drafts.add(draft);
        inlinePortionPrice.setText("");
        Toast.makeText(activity, activity.getString(R.string.toast_portion_added), Toast.LENGTH_SHORT).show();
        refreshDraftList();
        updatePriceHint();
    }

    private void removeDraftAt(int position) {
        if (position >= 0 && position < drafts.size()) {
            drafts.remove(position);
            for (int i = 0; i < drafts.size(); i++) {
                drafts.get(i).setSortOrder(i + 1);
            }
            if (drafts.isEmpty()) {
                portionMasterPicked = false;
            }
            refreshDraftList();
            updatePriceHint();
        }
    }

    private void refreshDraftList() {
        adapter.notifyDataSetChanged();
        inlinePortionListCard.setVisibility(drafts.isEmpty() ? View.GONE : View.VISIBLE);
        notifyPortionsChanged();
    }

    /**
     * Persist drafts after product save/update. Removes DB rows not in draft list.
     */
    public void savePortionsForProduct(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return;
        }

        Set<String> keepIds = new HashSet<>();
        for (ProductPortionDraft draft : drafts) {
            String network = draft.getPortionNetworkStatus();
            if (network == null || network.trim().isEmpty()) {
                network = randomKey(10);
                draft.setPortionNetworkStatus(network);
            }
            database.insertProductPortion(
                    productId,
                    draft.getPortionMasterId(),
                    draft.getPortionName(),
                    draft.getPortionPrice(),
                    draft.getSortOrder(),
                    "0",
                    network,
                    0);
            if (draft.getPortionId() != null) {
                keepIds.add(draft.getPortionId());
            } else {
                ProductPortionResponse saved = database.getProductPortionByMasterId(
                        productId, draft.getPortionMasterId());
                if (saved != null && saved.getPortionId() != null) {
                    keepIds.add(saved.getPortionId());
                    draft.setPortionId(saved.getPortionId());
                }
            }
        }

        List<ProductPortionResponse> existing = database.getProductPortionList(productId);
        for (ProductPortionResponse row : existing) {
            if (row.getPortionId() != null && !keepIds.contains(row.getPortionId())) {
                database.deleteProductPortion(row.getPortionId());
            }
        }
    }

    private static String randomKey(int length) {
        String allowed = "0123456789qwertyuiopasdfghjklzxcvbnm";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(allowed.charAt(random.nextInt(allowed.length())));
        }
        return sb.toString();
    }
}
