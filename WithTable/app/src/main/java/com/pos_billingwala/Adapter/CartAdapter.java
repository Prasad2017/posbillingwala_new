package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Activity.BluetoothPrint;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.CartProductListBinding;

import java.util.List;
import java.util.Locale;


@SuppressLint("SetTextI18n, NotifyDataSetChanged")
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.MyViewHolder> {

    Context context;
    List<ProductCartResponse> productCartResponseList;
    POSBillingWalaDatabase posBillingWalaDatabase;
    String productPriceUnit;

    public CartAdapter(Context context, List<ProductCartResponse> productCartResponseList) {
        this.context = context;
        this.productCartResponseList = productCartResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(CartProductListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        ProductCartResponse productCartResponse = productCartResponseList.get(position);
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);

        holder.binding.productName.setText(productCartResponse.getDisplayLineName());
        float productQuantity = parseQty(productCartResponse.getProductQuantity());
        holder.binding.productQuantity.setText(formatQty(productQuantity));

        float productPrice = Float.parseFloat(productCartResponse.getResolvedLinePrice());
        if (!CreatePos.companyResponseList.isEmpty()) {
            if (CreatePos.companyResponseList.get(0).getGstStatus() != null) {
                if (CreatePos.companyResponseList.get(0).getGstStatus().equalsIgnoreCase("On")) {
                    float totalCGST = 0f, totalSGST = 0f;
                    if (!productCartResponse.getProductCGST().equalsIgnoreCase("")) {
                        totalCGST += Float.parseFloat(productCartResponse.getProductCGST());
                    }
                    if (!productCartResponse.getProductSGST().equalsIgnoreCase("")) {
                        totalSGST += Float.parseFloat(productCartResponse.getProductSGST());
                    }
                    float totalPerProductGST = (productPrice + (productPrice * ((totalCGST + totalSGST) / 100))) * productQuantity;
                    productPriceUnit = MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalPerProductGST);
                } else {
                    productPriceUnit = MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", productPrice * productQuantity);
                }
            } else {
                productPriceUnit = MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", productPrice * productQuantity);
            }
        } else {
            productPriceUnit = MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", productPrice * productQuantity);
        }

        holder.binding.productPrice.setText(productPriceUnit);

        // Qty bottom sheet only from the quantity number — not − / + / price / row.
        holder.binding.getRoot().setOnClickListener(null);
        holder.binding.productName.setOnClickListener(null);
        holder.binding.productName.setClickable(false);

        holder.binding.productRemove.setOnClickListener(v -> {
            float quantity = parseQty(holder.binding.productQuantity.getText().toString());
            float totalQuantity = quantity - 1;
            if (totalQuantity > 0) {
                updateCart(productCartResponse.getCartId(), formatQty(totalQuantity), productCartResponse.getResolvedLinePrice());
            } else {
                deleteCartProduct(productCartResponse.getCartId());
            }
        });

        holder.binding.productAdd.setOnClickListener(v -> {
            float quantity = parseQty(holder.binding.productQuantity.getText().toString());
            float totalQuantity = quantity + 1;
            updateCart(productCartResponse.getCartId(), formatQty(totalQuantity), productCartResponse.getResolvedLinePrice());
        });

        holder.binding.productDelete.setOnClickListener(v -> BottomSheetUi.showConfirm(
                context,
                context.getString(R.string.toast_delete_product),
                context.getString(R.string.toast_do_you_want_to_delete_from_bill),
                "YES",
                "NO",
                true,
                () -> deleteCartProduct(productCartResponse.getCartId())));

        holder.binding.productQuantity.setOnClickListener(v -> setUpdateQuantity(productCartResponse));

        // Amount must not open the qty bottom sheet (same dialog was reused before).
        holder.binding.productPrice.setOnClickListener(null);
        holder.binding.productPrice.setClickable(false);
        holder.binding.productPrice.setFocusable(false);

    }

    public void setUpdateQuantity(ProductCartResponse productCartResponse) {
        Activity activity = (Activity) context;
        View content = LayoutInflater.from(activity).inflate(R.layout.update_quantity_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        TextView continueToQuantity = content.findViewById(R.id.continueToQuantity);
        TextView dismissQuantity = content.findViewById(R.id.dismissQuantity);
        TextInputEditText quantityTxt = content.findViewById(R.id.quantity);
        TextView detailsTxt = content.findViewById(R.id.details);
        detailsTxt.setText("Enter Quantity");

        quantityTxt.setText(formatQty(parseQty(productCartResponse.getProductQuantity())));
        quantityTxt.setSelection(quantityTxt.getText().toString().length());

        dismissQuantity.setOnClickListener(v -> sheet.dismiss());

        continueToQuantity.setOnClickListener(v -> {
            if (!quantityTxt.getText().toString().isEmpty()) {
                float entered = parseQty(quantityTxt.getText().toString());
                if (entered > 0) {
                    updateCart(productCartResponse.getCartId(), formatQty(entered), productCartResponse.getResolvedLinePrice());
                    sheet.dismiss();
                } else {
                    quantityTxt.setError("Enter Quantity");
                    quantityTxt.requestFocus();
                }
            } else {
                quantityTxt.setError("Enter Quantity");
                quantityTxt.requestFocus();
            }
        });
    }

    private static float parseQty(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0f;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private static String formatQty(float qty) {
        if (qty == (long) qty) {
            return String.valueOf((long) qty);
        }
        return String.format(Locale.US, "%.2f", qty);
    }

    public void updateCart(String cartId, String productQuantity, String productAmount) {
        posBillingWalaDatabase.updateCart(cartId, productQuantity, productAmount);
        notifyDataSetChanged();
        BluetoothPrint.getCartProductList();
    }

    public void deleteCartProduct(String cartId) {
        posBillingWalaDatabase.deleteCartProduct(cartId);
        Toast.makeText(context, context.getString(R.string.toast_product_deleted_from_cart), Toast.LENGTH_SHORT).show();
        notifyDataSetChanged();
        BluetoothPrint.getCartProductList();
    }

    @Override
    public int getItemCount() {
        return productCartResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        CartProductListBinding binding;

        public MyViewHolder(CartProductListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

        }

    }
}
