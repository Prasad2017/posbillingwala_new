package com.posbillingwala.admin.Activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.posbillingwala.admin.R;

import butterknife.BindView;
import butterknife.ButterKnife;


@SuppressLint("SetTextI18n, NonConstantResourceId, ResourceType")
public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 2000;
    @BindView(R.id.relativeLayout)
    RelativeLayout relativeLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        ButterKnife.bind(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);


    }

    @Override
    protected void onStart() {
        super.onStart();
        moveNext();
    }

    private void moveNext() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent i = new Intent(SplashScreen.this, Login.class);
                startActivity(i);
                finish();

            }
        }, SPLASH_TIME_OUT);

    }


}