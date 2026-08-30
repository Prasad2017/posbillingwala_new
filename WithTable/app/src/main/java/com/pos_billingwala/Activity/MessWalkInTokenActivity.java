package com.pos_billingwala.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.MessTokenQrHelper;
import com.pos_billingwala.Extra.TabletPrintUi;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivityMessWalkInTokenBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class MessWalkInTokenActivity extends BaseActivity implements View.OnClickListener {

    private ActivityMessWalkInTokenBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMessWalkInTokenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.messTypeHint.setText("Meal: " + MessTokenQrHelper.resolveMessType());
        binding.issueTokenCardView.setOnClickListener(this);
        TabletPrintUi.applyWalkInFormTablet(this, binding.walkInFormContainer);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() != R.id.issueTokenCardView) {
            return;
        }

        String name = binding.walkInName.getText() != null
                ? binding.walkInName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(name)) {
            binding.walkInName.setError("Enter customer name");
            binding.walkInName.requestFocus();
            return;
        }

        String mobile = binding.walkInMobile.getText() != null
                ? binding.walkInMobile.getText().toString().trim() : "";
        String amount = binding.walkInAmount.getText() != null
                ? binding.walkInAmount.getText().toString().trim() : "0";
        if (TextUtils.isEmpty(amount)) {
            amount = "0";
        }

        String tokenCode = MessTokenQrHelper.generateTokenCode();
        String messType = MessTokenQrHelper.resolveMessType();
        String networkStatus = getRandomString(10);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String tokenDate = df.format(Calendar.getInstance().getTime());

        POSBillingWalaDatabase db = new POSBillingWalaDatabase(this);
        db.saveMessToken(
                tokenCode,
                "",
                name,
                mobile,
                MessTokenQrHelper.MEMBER_TYPE_WALK_IN,
                messType,
                amount,
                tokenDate,
                networkStatus,
                MessTokenQrHelper.TOKEN_STATE_ACTIVE,
                0,
                0
        );

        Intent intent = new Intent(this, MessTokenBluetoothPrint.class);
        intent.putExtra("tokenCode", tokenCode);
        intent.putExtra("memberId", "");
        intent.putExtra("memberName", name);
        intent.putExtra("memberMobile", mobile);
        intent.putExtra("memberType", MessTokenQrHelper.MEMBER_TYPE_WALK_IN);
        intent.putExtra("messType", messType);
        intent.putExtra("tokenAmount", amount);
        intent.putExtra("tokenDate", tokenDate);
        intent.putExtra("tokenNetworkStatus", networkStatus);
        startActivity(intent);
    }

    private String getRandomString(final int sizeOfRandomString) {
        String allowed = "0123456789qwertyuiopasdfghjklzxcvbnm";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; i++) {
            sb.append(allowed.charAt(random.nextInt(allowed.length())));
        }
        return sb.toString();
    }
}
