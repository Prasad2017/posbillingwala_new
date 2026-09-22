package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.TabletFormUi;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentShareAppBinding;

public class ShareApp extends Fragment {

    public static Activity activity;
    private FragmentShareAppBinding binding;
    private String appStoreLink;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentShareAppBinding.inflate(inflater, container, false);
        activity = getActivity();

        appStoreLink = "https://play.google.com/store/apps/details?id=" + requireActivity().getPackageName();
        binding.txtLink.setText(appStoreLink);

        View view = binding.getRoot();
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                ((MainActivity) activity).navigateBack();
                return true;
            }
            return false;
        });

        binding.backToSetting.setOnClickListener(v -> ((MainActivity) activity).navigateBack());
        binding.btnCopyIcon.setOnClickListener(v -> copyAppLink());
        binding.btnQR.setOnClickListener(v -> showQrCodeDialog());
        binding.btnShare.setOnClickListener(v -> shareApp());

        TabletFormUi.applyAboutLayout(activity, binding.shareContentContainer);

        return view;
    }

    private void copyAppLink() {
        ClipboardManager clipboard = (ClipboardManager) requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.share_link_title), appStoreLink));
            Toast.makeText(activity, R.string.share_link_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void showQrCodeDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.Theme_Pos_BottomSheetDialog);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_share_qr, null);
        sheetView.findViewById(R.id.closeQrDialog).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void shareApp() {
        String shareMessage = getString(R.string.share_app_message, appStoreLink);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_now)));
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
    }
}
