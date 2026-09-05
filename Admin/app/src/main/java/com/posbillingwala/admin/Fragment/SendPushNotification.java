package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.f0ris.sweetalert.SweetAlertDialog;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Broadcast promotional push to POS / Owner / Dealer / Admin / All apps.
 */
public class SendPushNotification extends Fragment {

    private SweetAlertDialog pDialog;
    private LinearLayout posFilterWrap;
    private LinearLayout licenseIdsWrap;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Activity activity = getActivity();
        ((MainActivity) activity).setScreenTitle("Send Push");

        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = SettingsProfile.form(activity);
        root.setPadding(32, 24, 32, 40);
        root.setBackgroundColor(Color.parseColor("#F7F9FC"));

        EditText title = field(activity, root, "Title", "Special offer this week");
        EditText message = field(activity, root, "Message", "Renew now and get 10% off");
        message.setMinLines(3);
        message.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        Spinner audience = new Spinner(activity);
        ArrayAdapter<String> audienceAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"POS app", "Owner app", "Dealer app", "Admin app", "All apps"});
        audience.setAdapter(audienceAdapter);
        addLabeled(root, activity, "App audience", audience);

        posFilterWrap = new LinearLayout(activity);
        posFilterWrap.setOrientation(LinearLayout.VERTICAL);
        root.addView(posFilterWrap);

        Spinner target = new Spinner(activity);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Active licences", "All licences", "Specific licence IDs"});
        target.setAdapter(adapter);
        addLabeled(posFilterWrap, activity, "POS licence filter", target);

        licenseIdsWrap = new LinearLayout(activity);
        licenseIdsWrap.setOrientation(LinearLayout.VERTICAL);
        posFilterWrap.addView(licenseIdsWrap);
        EditText licenseIds = field(activity, licenseIdsWrap, "Licence IDs (comma separated)", "12,45,78");

        audience.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean showPos = position == 0 || position == 4;
                posFilterWrap.setVisibility(showPos ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        target.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                licenseIdsWrap.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        licenseIdsWrap.setVisibility(View.GONE);

        EditText url = field(activity, root, "Link URL (optional)", "https://posbillingwala.com");
        url.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

        Button send = new Button(activity);
        send.setText("Send notification");
        send.setBackgroundResource(R.drawable.bg_button_primary);
        send.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 24;
        send.setLayoutParams(lp);
        send.setOnClickListener(v -> submit(activity, title, message, audience, target, licenseIds, url));
        root.addView(send);

        scroll.addView(root);
        return scroll;
    }

    private void submit(Activity activity, EditText title, EditText message, Spinner audience,
                        Spinner target, EditText licenseIds, EditText url) {
        String titleVal = title.getText().toString().trim();
        String msgVal = message.getText().toString().trim();
        if (titleVal.isEmpty() || msgVal.isEmpty()) {
            Toast.makeText(activity, "Title and message are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String audienceVal = "pos";
        switch (audience.getSelectedItemPosition()) {
            case 1: audienceVal = "owner"; break;
            case 2: audienceVal = "dealer"; break;
            case 3: audienceVal = "admin"; break;
            case 4: audienceVal = "all"; break;
            default: audienceVal = "pos"; break;
        }

        String targetVal = "active";
        int pos = target.getSelectedItemPosition();
        if (pos == 1) {
            targetVal = "all";
        } else if (pos == 2) {
            targetVal = "license_ids";
        }

        pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.setTitleText("Sending…");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().sendPushNotification(
                titleVal,
                msgVal,
                audienceVal,
                targetVal,
                licenseIds.getText().toString().trim(),
                url.getText().toString().trim(),
                "");
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                pDialog.dismissWithAnimation();
                if (response.isSuccessful() && response.body() != null
                        && "1".equals(response.body().getStatus())) {
                    Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : null;
                    Toast.makeText(activity,
                            msg != null && !msg.isEmpty() ? msg : "Unable to send notification",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismissWithAnimation();
                Toast.makeText(activity, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private EditText field(Activity activity, LinearLayout root, String label, String hint) {
        EditText input = new EditText(activity);
        input.setHint(hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        addLabeled(root, activity, label, input);
        return input;
    }

    private void addLabeled(LinearLayout root, Activity activity, String label, View input) {
        android.widget.TextView tv = new android.widget.TextView(activity);
        tv.setText(label);
        tv.setPadding(0, 20, 0, 8);
        root.addView(tv);
        root.addView(input);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).lockUnlockDrawer(1);
    }
}
