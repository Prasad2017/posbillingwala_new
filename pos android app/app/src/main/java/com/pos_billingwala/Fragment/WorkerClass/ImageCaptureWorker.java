package com.pos_billingwala.Fragment.WorkerClass;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ImageCaptureWorker extends Worker {

    public ImageCaptureWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // Code to capture image from the camera and save to URI
            Uri imageUri = captureImageFromCamera();
            if (imageUri != null) {
                Data outputData = new Data.Builder()
                        .putString("imageUri", String.valueOf(imageUri))  // Store Base64 string in Data
                        .build();
                return Result.success(outputData);
            } else {
                return Result.failure();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }

    private Uri captureImageFromCamera() throws IOException {
        Uri imageUri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use ContentValues to create URI for camera
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "CapturedImage");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
            imageUri = getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } else {
            // Use file path for older Android versions
            imageUri = Uri.fromFile(new File(getApplicationContext().getExternalFilesDir(null), "captured_image.jpg"));
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cameraIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(cameraIntent);

        return imageUri;
    }

    private String convertImageToBase64(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), imageUri);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
