package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Activity.BluetoothPrint;
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
        holder.binding.productQuantity.setText(productCartResponse.getProductQuantity());

        float productPrice = Float.parseFloat(productCartResponse.getResolvedLinePrice());
        float productQuantity = Float.parseFloat(productCartResponse.getProductQuantity());
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
                    productPriceUnit = MainActivity.currencyName + " " + (productPrice * productQuantity);
                }
            } else {
                productPriceUnit = MainActivity.currencyName + " " + (productPrice * productQuantity);
            }
        } else {
            productPriceUnit = MainActivity.currencyName + " " + (productPrice * productQuantity);
        }

        holder.binding.productPrice.setText(MainActivity.currencyName + " " + productPriceUnit);

        holder.binding.productRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!holder.binding.productQuantity.getText().toString().isEmpty()) {
                    float quantity = Float.parseFloat(holder.binding.productQuantity.getText().toString());
                    float totalQuantity = quantity - 1;
                    if (totalQuantity > 0) {
                        updateCart(productCartResponse.getCartId(), String.valueOf(totalQuantity), productCartResponse.getResolvedLinePrice());
                    } else {
                        deleteCartProduct(productCartResponse.getCartId());
                    }
                } else {
                    deleteCartProduct(productCartResponse.getCartId());
                }
            }
        });

        holder.binding.productAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!holder.binding.productQuantity.getText().toString().isEmpty()) {
                    float quantity = Float.parseFloat(holder.
                            binding.productQuantity.getText().toString());
                    float totalQuantity = quantity + 1;
                    Log.e("totalQuantity", String.valueOf(totalQuantity));
                    updateCart(productCartResponse.getCartId(), String.valueOf(totalQuantity), productCartResponse.getResolvedLinePrice());
                } else {
                    deleteCartProduct(productCartResponse.getCartId());
                }
            }
        });

        holder.binding.productDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(context)
                        .setTitle(context.getString(R.string.toast_delete_product))
                        .setMessage(context.getString(R.string.toast_do_you_want_to_delete_from_bill))
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                deleteCartProduct(productCartResponse.getCartId());
                            }
                        })
                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .show();

            }
        });

        holder.binding.productQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUpdateQuantity(productCartResponse);
            }
        });

        holder.binding.productPrice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUpdateProductPrice(productCartResponse);
            }
        });

    }

    public void setUpdateProductPrice(ProductCartResponse productCartResponse) {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.update_quantity_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        TextView continueToQuantity = dialog.findViewById(R.id.continueToQuantity);
        TextView dismissQuantity = dialog.findViewById(R.id.dismissQuantity);
        TextInputEditText quantityTxt = dialog.findViewById(R.id.quantity);
        TextView detailsTxt = dialog.findViewById(R.id.details);
        detailsTxt.setText("Enter Amount");

        quantityTxt.setText(productCartResponse.getResolvedLinePrice());
        quantityTxt.setSelection(quantityTxt.getText().toString().length());

        dismissQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        continueToQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!quantityTxt.getText().toString().isEmpty()) {
                    float totalQuantity = Float.parseFloat(productCartResponse.getProductQuantity());
                    updateCart(productCartResponse.getCartId(), String.valueOf(totalQuantity), quantityTxt.getText().toString());
                    dialog.dismiss();
                } else {
                    quantityTxt.setError("Enter Amount");
                    quantityTxt.requestFocus();
                }
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);

    }

    public void setUpdateQuantity(ProductCartResponse productCartResponse) {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.update_quantity_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        TextView continueToQuantity = dialog.findViewById(R.id.continueToQuantity);
        TextView dismissQuantity = dialog.findViewById(R.id.dismissQuantity);
        TextInputEditText quantityTxt = dialog.findViewById(R.id.quantity);
        TextView detailsTxt = dialog.findViewById(R.id.details);
        detailsTxt.setText("Enter Quantity");

        quantityTxt.setText(productCartResponse.getProductQuantity());
        quantityTxt.setSelection(quantityTxt.getText().toString().length());

        dismissQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        continueToQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!quantityTxt.getText().toString().isEmpty()) {
                    if (Float.parseFloat(quantityTxt.getText().toString()) > 0) {
                        float totalQuantity = Float.parseFloat(quantityTxt.getText().toString());
                        updateCart(productCartResponse.getCartId(), String.valueOf(totalQuantity), productCartResponse.getResolvedLinePrice());
                        dialog.dismiss();
                    } else {
                        quantityTxt.setError("Enter Quantity");
                        quantityTxt.requestFocus();
                    }
                } else {
                    quantityTxt.setError("Enter Quantity");
                    quantityTxt.requestFocus();
                }
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);

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
