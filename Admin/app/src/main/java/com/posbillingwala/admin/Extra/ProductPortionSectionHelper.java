package com.posbillingwala.admin.Extra;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.posbillingwala.admin.Adapter.ProductPortionDraftAdapter;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.PortionMasterResponse;
import com.posbillingwala.admin.Model.ProductPortionDraft;
import com.posbillingwala.admin.Model.ProductPortionResponse;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.IncludeProductPortionSectionBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Inline optional portions on Add/Edit Product.
 */
public class ProductPortionSectionHelper {

    public interface SaveCallback {
        void onComplete(boolean allOk);
    }

    private final Activity activity;
    private final IncludeProductPortionSectionBinding binding;

    private final List<ProductPortionDraft> drafts = new ArrayList<>();
    private ProductPortionDraftAdapter adapter;
    private List<PortionMasterResponse> portionMasterList = new ArrayList<>();
    private String[] portionMasterIdList;
    private String[] portionMasterNameList;
    private String selectedPortionMasterId;
    private String selectedPortionMasterName;
    private String customerId;
    private Runnable onPortionMasterLinkClick;

    public ProductPortionSectionHelper(Activity activity, View root) {
        this.activity = activity;
        this.binding = IncludeProductPortionSectionBinding.bind(root);

        adapter = new ProductPortionDraftAdapter(activity, drafts, this::removeDraftAt);
        binding.inlinePortionRecyclerview.setLayoutManager(new GridLayoutManager(activity, 1));
        binding.inlinePortionRecyclerview.setAdapter(adapter);

        binding.addInlinePortion.setOnClickListener(v -> addDraftFromForm());
        binding.managePortionMasterLink.setOnClickListener(v -> {
            if (onPortionMasterLinkClick != null) {
                onPortionMasterLinkClick.run();
            }
        });

        binding.portionMasterSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                if (portionMasterIdList != null && position >= 0 && position < portionMasterIdList.length) {
                    selectedPortionMasterId = portionMasterIdList[position];
                    selectedPortionMasterName = portionMasterNameList[position];
                }
            }
        });
    }

    public void setOnPortionMasterLinkClick(Runnable onPortionMasterLinkClick) {
        this.onPortionMasterLinkClick = onPortionMasterLinkClick;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void refresh() {
        if (customerId == null || customerId.isEmpty()) {
            return;
        }
        loadPortionMasters();
        refreshDraftList();
        updatePriceHint();
    }

    public void loadExistingPortions(String productId) {
        drafts.clear();
        if (customerId == null || productId == null || productId.isEmpty()) {
            refreshDraftList();
            updatePriceHint();
            return;
        }
        Call<AllApiResponse> call = Api.getClient().getPortionList(customerId, productId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ProductPortionResponse> list = response.body().getPortionResponseList();
                    if (list != null) {
                        for (ProductPortionResponse row : list) {
                            drafts.add(ProductPortionDraft.fromResponse(row));
                        }
                    }
                }
                refreshDraftList();
                updatePriceHint();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Log.e("portionDraftLoad", "" + t.getMessage());
                refreshDraftList();
                updatePriceHint();
            }
        });
    }

    public boolean hasPortions() {
        return !drafts.isEmpty();
    }

    public List<ProductPortionDraft> getDrafts() {
        return drafts;
    }

    public void updatePriceHint() {
        if (hasPortions()) {
            binding.portionSectionHint.setText("Product price optional when portions exist (empty/0 allowed)");
        } else {
            binding.portionSectionHint.setText("Product price required if no portions");
        }
    }

    private void loadPortionMasters() {
        Call<AllApiResponse> call = Api.getClient().getPortionMasterList(customerId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PortionMasterResponse> list = response.body().getPortionMasterResponse();
                    portionMasterList = list != null ? list : new ArrayList<>();
                } else {
                    portionMasterList = new ArrayList<>();
                }
                setupPortionMasterSpinner();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Log.e("portionMasterLoad", "" + t.getMessage());
                portionMasterList = new ArrayList<>();
                setupPortionMasterSpinner();
            }
        });
    }

    private void setupPortionMasterSpinner() {
        if (portionMasterList.isEmpty()) {
            binding.portionMasterPickerSection.setVisibility(View.GONE);
            binding.noPortionMasterHint.setVisibility(View.VISIBLE);
            selectedPortionMasterId = null;
            selectedPortionMasterName = null;
            return;
        }

        binding.noPortionMasterHint.setVisibility(View.GONE);
        binding.portionMasterPickerSection.setVisibility(View.VISIBLE);
        portionMasterIdList = new String[portionMasterList.size()];
        portionMasterNameList = new String[portionMasterList.size()];
        for (int i = 0; i < portionMasterList.size(); i++) {
            portionMasterIdList[i] = portionMasterList.get(i).getPortionMasterId();
            portionMasterNameList[i] = portionMasterList.get(i).getPortionName();
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_spinner_item, portionMasterNameList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        binding.portionMasterSpinner.setAdapter(spinnerAdapter);
        selectedPortionMasterId = portionMasterIdList[0];
        selectedPortionMasterName = portionMasterNameList[0];
    }

    private void addDraftFromForm() {
        if (selectedPortionMasterId == null || selectedPortionMasterId.isEmpty()) {
            Toast.makeText(activity, "Please add portion masters first", Toast.LENGTH_SHORT).show();
            return;
        }
        String price = binding.inlinePortionPrice.getText() != null
                ? binding.inlinePortionPrice.getText().toString().trim() : "";
        if (price.isEmpty()) {
            Toast.makeText(activity, "Please enter portion price", Toast.LENGTH_SHORT).show();
            return;
        }

        for (ProductPortionDraft draft : drafts) {
            if (selectedPortionMasterId.equals(draft.getPortionMasterId())) {
                draft.setPortionPrice(price);
                draft.setPortionName(selectedPortionMasterName);
                Toast.makeText(activity, "Portion price updated", Toast.LENGTH_SHORT).show();
                binding.inlinePortionPrice.setText("");
                refreshDraftList();
                updatePriceHint();
                return;
            }
        }

        ProductPortionDraft draft = new ProductPortionDraft(
                selectedPortionMasterId,
                selectedPortionMasterName,
                price,
                drafts.size() + 1);
        draft.setPortionNetworkStatus(randomKey(10));
        drafts.add(draft);
        binding.inlinePortionPrice.setText("");
        Toast.makeText(activity, "Portion added", Toast.LENGTH_SHORT).show();
        refreshDraftList();
        updatePriceHint();
    }

    private void removeDraftAt(int position) {
        if (position >= 0 && position < drafts.size()) {
            drafts.remove(position);
            for (int i = 0; i < drafts.size(); i++) {
                drafts.get(i).setSortOrder(i + 1);
            }
            refreshDraftList();
            updatePriceHint();
        }
    }

    private void refreshDraftList() {
        adapter.notifyDataSetChanged();
        binding.inlinePortionListCard.setVisibility(drafts.isEmpty() ? View.GONE : View.VISIBLE);
    }

    public void savePortionsForProduct(String productId, SaveCallback callback) {
        if (productId == null || productId.trim().isEmpty() || drafts.isEmpty()) {
            if (callback != null) {
                callback.onComplete(true);
            }
            return;
        }
        saveDraftAt(productId, 0, true, callback);
    }

    private void saveDraftAt(String productId, int index, boolean allOk, SaveCallback callback) {
        if (index >= drafts.size()) {
            if (callback != null) {
                callback.onComplete(allOk);
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
                customerId,
                productId,
                draft.getPortionName(),
                draft.getPortionPrice(),
                String.valueOf(draft.getSortOrder()),
                network,
                draft.getPortionMasterId() != null ? draft.getPortionMasterId() : "");
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                boolean ok = response.isSuccessful() && response.body() != null
                        && "1".equalsIgnoreCase(response.body().getStatus());
                saveDraftAt(productId, index + 1, allOk && ok, callback);
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Log.e("portionSave", "" + t.getMessage());
                saveDraftAt(productId, index + 1, false, callback);
            }
        });
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
