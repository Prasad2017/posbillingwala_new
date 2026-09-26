package com.pos_billingwala.Extra;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.pos_billingwala.R;

import java.io.File;

import static android.app.Activity.RESULT_OK;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

/**
 * Gallery / Camera / Remove for product master form (Flutter ProductFormPage parity).
 */
public final class ProductImageSectionHelper {

    public static final int REQ_CAMERA = 3101;
    public static final int REQ_GALLERY = 3102;

    private final Activity activity;
    private final Fragment fragment;
    private final ImageView preview;
    private final MaterialButton removeBtn;
    private String productImage;
    private Uri cameraUri;

    public ProductImageSectionHelper(Fragment fragment, View root) {
        this.fragment = fragment;
        this.activity = fragment.requireActivity();
        this.preview = root.findViewById(R.id.productImagePreview);
        MaterialButton gallery = root.findViewById(R.id.productImageGallery);
        MaterialButton camera = root.findViewById(R.id.productImageCamera);
        this.removeBtn = root.findViewById(R.id.productImageRemove);
        if (gallery != null) {
            gallery.setOnClickListener(v -> openGallery());
        }
        if (camera != null) {
            camera.setOnClickListener(v -> ensureCameraThenCapture());
        }
        if (removeBtn != null) {
            removeBtn.setOnClickListener(v -> clearImage());
        }
        refreshPreview();
    }

    @Nullable
    public String getProductImage() {
        return productImage;
    }

    public void setProductImage(@Nullable String value) {
        this.productImage = value;
        refreshPreview();
    }

    public void clearImage() {
        productImage = null;
        refreshPreview();
    }

    public boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK) {
            return false;
        }
        Uri uri = null;
        if (requestCode == REQ_CAMERA) {
            uri = cameraUri;
        } else if (requestCode == REQ_GALLERY && data != null) {
            uri = data.getData();
        } else {
            return false;
        }
        applyUri(uri);
        return true;
    }

    private void refreshPreview() {
        if (preview == null) {
            return;
        }
        if (ProductImageHelper.hasImage(productImage)) {
            ProductImageHelper.bind(preview, productImage, true);
            if (removeBtn != null) {
                removeBtn.setVisibility(View.VISIBLE);
            }
        } else {
            preview.setImageResource(R.drawable.product);
            preview.setVisibility(View.VISIBLE);
            if (removeBtn != null) {
                removeBtn.setVisibility(View.GONE);
            }
        }
    }

    private void applyUri(@Nullable Uri uri) {
        String encoded = ProductImageHelper.encodeUri(activity, uri);
        if (encoded == null) {
            Toast.makeText(activity, R.string.toast_product_image_too_large, Toast.LENGTH_SHORT).show();
            return;
        }
        productImage = encoded;
        refreshPreview();
    }

    private void openGallery() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            intent.setType("image/*");
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent = Intent.createChooser(intent, activity.getString(R.string.app_name));
        }
        intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION);
        fragment.startActivityForResult(intent, REQ_GALLERY);
    }

    private void ensureCameraThenCapture() {
        Dexter.withContext(activity)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        openCamera();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(activity, R.string.toast_canceled_by_user, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void openCamera() {
        try {
            File photoFile = new File(activity.getCacheDir(),
                    "product_capture_" + System.currentTimeMillis() + ".jpg");
            cameraUri = FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".provider",
                    photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION);
            fragment.startActivityForResult(intent, REQ_CAMERA);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, R.string.toast_canceled_by_user, Toast.LENGTH_SHORT).show();
        }
    }
}
