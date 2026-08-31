package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.TabletFormUi;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentAboutUsBinding;


public class AboutUs extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public AdView adView;
    View view;
    FragmentAboutUsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAboutUsBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("tag", "onKey Back listener is working!!!");
                ((MainActivity) activity).navigateBack();
                return true;
            }
            return false;
        });

        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            binding.appVersion.setText("V " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String supportPhone = getString(R.string.support_phone_display);
        binding.technicalSupport.setText(
                getString(R.string.technical_support_91_8983149299_91_9130188584, supportPhone));

        initAds();

        binding.backToSetting.setOnClickListener(this);
        binding.websiteRow.setOnClickListener(this);
        binding.emailRow.setOnClickListener(this);
        binding.phoneRow.setOnClickListener(this);
        binding.googlePlayBadge.setOnClickListener(this);
        binding.headerMenuIcon.setOnClickListener(this);

        TabletFormUi.applyAboutLayout(activity, binding.aboutContentContainer);

        return view;
    }

    public void initAds() {
        adView = view.findViewById(R.id.ad_view);
        MobileAds.initialize(activity, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                Log.e("initializationStatus", "" + initializationStatus);
            }
        });
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.websiteRow) {
            openUrl("https://www.posbillingwala.com");
        } else if (id == R.id.emailRow) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + getString(R.string.about_email)));
            startActivity(Intent.createChooser(intent, getString(R.string.about_label_email)));
        } else if (id == R.id.phoneRow) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + getString(R.string.support_phone_dial)));
            startActivity(intent);
        } else if (id == R.id.googlePlayBadge || id == R.id.headerMenuIcon) {
            openPlayStore();
        }
    }

    private void openPlayStore() {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getPackageName()));
        startActivity(intent);
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (adView != null) {
            adView.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adView != null) {
            adView.destroy();
        }
    }
}
