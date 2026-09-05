package com.pos_billingwala.Activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.MessMealSessionItem;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;

import java.util.ArrayList;
import java.util.List;

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

    private class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mess_meal_session, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            MessMealSessionItem item = items.get(position);
            h.sessionName.setText(item.sessionName);
            h.startTime.setText(item.startTime);
            h.endTime.setText(item.endTime);
            h.tokenPrefix.setText(item.tokenPrefix);
            h.isActive.setChecked("1".equals(item.isActive));
            h.btnSave.setOnClickListener(v -> Api.getClient(MessMealSessionsActivity.this).saveMessMealSession(
                    MainActivity.userId,
                    item.sessionId != null ? item.sessionId : "",
                    h.sessionName.getText().toString().trim(),
                    h.startTime.getText().toString().trim(),
                    h.endTime.getText().toString().trim(),
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

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            EditText sessionName, startTime, endTime, tokenPrefix;
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
