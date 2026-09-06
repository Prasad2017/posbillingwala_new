package com.pos_billingwala.Activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.MessMealSessionItem;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessMealSessionsActivity extends BaseActivity {

    private final List<MessMealSessionItem> items = new ArrayList<>();
    private SessionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mess_meal_sessions);
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        RecyclerView rv = findViewById(R.id.recyclerView);
        adapter = new SessionAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        load();
    }

    private void load() {
        Api.getClient(this).getMessMealSessions(MainActivity.userId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                items.clear();
                if (response.isSuccessful() && response.body() != null && response.body().messSessions != null) {
                    items.addAll(response.body().messSessions);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Toast.makeText(MessMealSessionsActivity.this, "Unable to load sessions", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openClockPicker(TextView target, String title) {
        int[] hm = parseHm(target.getText() != null ? target.getText().toString() : "12:00 AM");
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(hm[0])
                .setMinute(hm[1])
                .setTitleText(title)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build();
        picker.addOnPositiveButtonClickListener(v ->
                target.setText(formatAmPm(picker.getHour(), picker.getMinute())));
        picker.show(getSupportFragmentManager(), "mess_session_time");
    }

    /** Parses "HH:mm", "H:mm", or "h:mm AM/PM" into 24h hour/minute. */
    private static int[] parseHm(String value) {
        int hour = 0;
        int minute = 0;
        if (TextUtils.isEmpty(value)) {
            return new int[]{0, 0};
        }
        String raw = value.trim().toUpperCase(Locale.US);
        boolean hasAm = raw.contains("AM");
        boolean hasPm = raw.contains("PM");
        raw = raw.replace("AM", "").replace("PM", "").trim();

        if (raw.contains(":")) {
            try {
                String[] parts = raw.split(":");
                hour = Integer.parseInt(parts[0].trim());
                minute = Integer.parseInt(parts[1].trim().replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {
            }
        }

        if (hasAm || hasPm) {
            if (hasPm && hour < 12) {
                hour += 12;
            } else if (hasAm && hour == 12) {
                hour = 0;
            }
        }

        hour = Math.max(0, Math.min(23, hour));
        minute = Math.max(0, Math.min(59, minute));
        return new int[]{hour, minute};
    }

    /** Display format e.g. 11:30 AM, 3:00 PM */
    private static String formatAmPm(int hour24, int minute) {
        hour24 = Math.max(0, Math.min(23, hour24));
        minute = Math.max(0, Math.min(59, minute));
        String amPm = hour24 < 12 ? "AM" : "PM";
        int hour12 = hour24 % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm);
    }

    /** API / DB format HH:mm (24-hour). */
    private static String toApiTime(String displayOrApi) {
        int[] hm = parseHm(displayOrApi);
        return String.format(Locale.US, "%02d:%02d", hm[0], hm[1]);
    }

    private class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mess_meal_session, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            MessMealSessionItem item = items.get(position);
            h.sessionName.setText(item.sessionName != null ? item.sessionName : "");
            h.startTime.setText(toDisplayTime(item.startTime));
            h.endTime.setText(toDisplayTime(item.endTime));
            h.tokenPrefix.setText(item.tokenPrefix != null ? item.tokenPrefix : "");
            h.isActive.setChecked("1".equals(item.isActive));

            h.startTime.setOnClickListener(v -> openClockPicker(h.startTime, "Start time"));
            h.endTime.setOnClickListener(v -> openClockPicker(h.endTime, "End time"));

            h.btnSave.setOnClickListener(v -> Api.getClient(MessMealSessionsActivity.this).saveMessMealSession(
                    MainActivity.userId,
                    item.sessionId != null ? item.sessionId : "",
                    h.sessionName.getText().toString().trim(),
                    toApiTime(h.startTime.getText().toString()),
                    toApiTime(h.endTime.getText().toString()),
                    h.tokenPrefix.getText().toString().trim(),
                    h.isActive.isChecked() ? "1" : "0",
                    item.menuNotes != null ? item.menuNotes : "",
                    item.sortOrder != null ? item.sortOrder : "0"
            ).enqueue(new Callback<AllApiResponse>() {
                @Override
                public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                    Toast.makeText(MessMealSessionsActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                    load();
                }

                @Override
                public void onFailure(Call<AllApiResponse> call, Throwable t) {
                    Toast.makeText(MessMealSessionsActivity.this, "Save failed", Toast.LENGTH_SHORT).show();
                }
            }));
        }

        private String toDisplayTime(String value) {
            if (TextUtils.isEmpty(value)) {
                return formatAmPm(0, 0);
            }
            int[] hm = parseHm(value);
            return formatAmPm(hm[0], hm[1]);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            EditText sessionName, tokenPrefix;
            TextView startTime, endTime;
            CheckBox isActive;
            Button btnSave;

            VH(@NonNull View itemView) {
                super(itemView);
                sessionName = itemView.findViewById(R.id.sessionName);
                startTime = itemView.findViewById(R.id.startTime);
                endTime = itemView.findViewById(R.id.endTime);
                tokenPrefix = itemView.findViewById(R.id.tokenPrefix);
                isActive = itemView.findViewById(R.id.isActive);
                btnSave = itemView.findViewById(R.id.btnSave);
            }
        }
    }
}
