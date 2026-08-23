package com.posbillingwala.owner.Extra;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Adapter.ProductPortionDraftAdapter;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.PortionMasterResponse;
import com.posbillingwala.owner.Model.ProductPortionDraft;
import com.posbillingwala.owner.Model.ProductPortionResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Inline optional portions on Add/Edit Product — select Portion Master + price.
 */
public class ProductPortionSectionHelper {

    private final Activity activity;
    private final View root;
    private final TextView managePortionMasterLink;
    private final View portionMasterPickerSection;
    private final TextView noPortionMasterHint;
    private final AutoCompleteTextView portionMasterSpinner;
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
    private Runnable onPortionMasterLinkClick;

    public ProductPortionSectionHelper(Activity activity, View root) {
        this.activity = activity;
        this.root = root;
        this.managePortionMasterLink = root.findViewById(R.id.managePortionMasterLink);
        this.portionMasterPickerSection = root.findViewById(R.id.portionMasterPickerSection);
        this.noPortionMasterHint = root.findViewById(R.id.noPortionMasterHint);
        this.portionMasterSpinner = root.findViewById(R.id.portionMasterSpinner);
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

    public void refresh() {
        loadPortionMasters();
    }

    public void loadExistingForProduct(String productId) {
        drafts.clear();
        if (productId == null || productId.trim().isEmpty()) {
            refreshDraftList();
            loadPortionMasters();
            return;
        }
        Call<AllApiResponse> call = Api.getClient().getPortionList(MainActivity.userId, productId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ProductPortionResponse> existing = response.body().getPortionResponseList();
                    if (existing != null) {
                        for (ProductPortionResponse row : existing) {
                            drafts.add(ProductPortionDraft.fromResponse(row));
                        }
                    }
                }
                refreshDraftList();
                loadPortionMasters();
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("portionLoad", "" + t.getMessage());
                loadPortionMasters();
            }
        });
    }

    public boolean hasPortions() {
        return !drafts.isEmpty();
    }

    public List<ProductPortionDraft> getDrafts() {
        return drafts;
    }

    private void loadPortionMasters() {
        Call<AllApiResponse> call = Api.getClient().getPortionMasterList(MainActivity.userId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PortionMasterResponse> list = response.body().getPortionMasterResponseList();
                    portionMasterList = list != null ? list : new ArrayList<>();
                } else {
                    portionMasterList = new ArrayList<>();
                }
                setupPortionMasterSpinner();
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                portionMasterList = new ArrayList<>();
                setupPortionMasterSpinner();
            }
        });
    }

    private void setupPortionMasterSpinner() {
        if (portionMasterList.isEmpty()) {
            portionMasterPickerSection.setVisibility(View.GONE);
            noPortionMasterHint.setVisibility(View.VISIBLE);
            selectedPortionMasterId = null;
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

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                activity, R.layout.spinner_item_layout, portionMasterNameList);
        portionMasterSpinner.setAdapter(spinnerAdapter);
        selectedPortionMasterId = portionMasterIdList[0];
        portionMasterSpinner.setText(portionMasterNameList[0], false);
        portionMasterSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedPortionMasterId = portionMasterIdList[position];
            }
        });
    }

    private void addDraftFromForm() {
        if (selectedPortionMasterId == null || selectedPortionMasterId.isEmpty()) {
            Toast.makeText(activity, "Please create Portion Master first", Toast.LENGTH_SHORT).show();
            return;
        }
        String price = inlinePortionPrice.getText() != null
                ? inlinePortionPrice.getText().toString().trim() : "";
        if (price.isEmpty()) {
            Toast.makeText(activity, "Please enter portion price", Toast.LENGTH_SHORT).show();
            return;
        }

        String masterName = null;
        for (PortionMasterResponse master : portionMasterList) {
            if (selectedPortionMasterId.equals(master.getPortionMasterId())) {
                masterName = master.getPortionName();
                break;
            }
        }
        if (masterName == null) {
            Toast.makeText(activity, "Invalid portion", Toast.LENGTH_SHORT).show();
            return;
        }

        for (ProductPortionDraft draft : drafts) {
            if (selectedPortionMasterId.equals(draft.getPortionMasterId())) {
                draft.setPortionPrice(price);
                draft.setPortionName(masterName);
                Toast.makeText(activity, "Portion price updated", Toast.LENGTH_SHORT).show();
                inlinePortionPrice.setText("");
                refreshDraftList();
                return;
            }
        }

        ProductPortionDraft draft = new ProductPortionDraft(
                selectedPortionMasterId,
                masterName,
                price,
                drafts.size());
        draft.setPortionNetworkStatus(randomKey(10));
        drafts.add(draft);
        inlinePortionPrice.setText("");
        Toast.makeText(activity, "Portion added", Toast.LENGTH_SHORT).show();
        refreshDraftList();
    }

    private void removeDraftAt(int position) {
        if (position >= 0 && position < drafts.size()) {
            drafts.remove(position);
            for (int i = 0; i < drafts.size(); i++) {
                drafts.get(i).setSortOrder(i);
            }
            refreshDraftList();
        }
    }

    private void refreshDraftList() {
        adapter.notifyDataSetChanged();
        inlinePortionListCard.setVisibility(drafts.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /**
     * Persist drafts after product save/update via Owner insertCustomerPortion API.
     */
    public void savePortionsForProduct(String productId, Runnable onComplete) {
        if (productId == null || productId.trim().isEmpty() || drafts.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        saveDraftAt(productId, 0, onComplete);
    }

    private void saveDraftAt(String productId, int index, Runnable onComplete) {
        if (index >= drafts.size()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        ProductPortionDraft draft = drafts.get(index);
        String network = draft.getPortionNetworkStatus();
        if (network == null || network.trim().isEmpty()) {
            network = randomKey(10);
            draft.setPortionNetworkStatus(network);
        }
        Call<AllApiResponse> call = Api.getClient().savePortion(
                MainActivity.userId,
                productId,
                draft.getPortionMasterId(),
                draft.getPortionName(),
                draft.getPortionPrice(),
                String.valueOf(draft.getSortOrder()),
                network);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                saveDraftAt(productId, index + 1, onComplete);
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("portionSave", "" + t.getMessage());
                saveDraftAt(productId, index + 1, onComplete);
            }
        });
    }

    private static String randomKey(int length) {
        String allowed = "0123456789qwertyuiopasdfghjklzxcvbnm";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            sb.append(allowed.charAt(random.nextInt(allowed.length())));
        }
        return sb.toString();
    }
}
