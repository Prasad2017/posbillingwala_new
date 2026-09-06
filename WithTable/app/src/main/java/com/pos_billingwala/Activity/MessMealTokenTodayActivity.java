package com.pos_billingwala.Activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.MessMealTokenPrintWorker;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.MessMealTokenItem;
import com.pos_billingwala.Model.MessSessionCount;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessMealTokenTodayActivity extends BaseActivity {

    private TextView countsText;
    private RecyclerView recyclerView;
    private final List<MessMealTokenItem> items = new ArrayList<>();
    private TokenAdapter adapter;
    private String today;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mess_meal_token_today);
        countsText = findViewById(R.id.countsText);
        recyclerView = findViewById(R.id.recyclerView);
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        adapter = new TokenAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MessMealTokenPrintWorker.recoverPendingFromServer(this);
        loadToday();
    }

    private void loadToday() {
        Api.getClient(this).getMessMealTokenToday(MainActivity.userId, today)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                        items.clear();
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().messMealTokens != null) {
                                items.addAll(response.body().messMealTokens);
                                POSBillingWalaDatabase db = new POSBillingWalaDatabase(MessMealTokenTodayActivity.this);
                                for (MessMealTokenItem t : response.body().messMealTokens) {
                                    if (t == null || t.tokenId == null) continue;
                                    String localStatus = t.printStatus;
                                    if ("PRINT_PENDING".equalsIgnoreCase(localStatus)
                                            || "PRINT_FAILED".equalsIgnoreCase(localStatus)
                                            || "CREATED".equalsIgnoreCase(localStatus)) {
                                        db.upsertMessMealTokenQueue(t.tokenId, t.tokenNumber, t.registrationNo,
                                                t.mealSession, t.date, t.memberName, t.createdAt, "PRINT_PENDING");
                                    }
                                }
                            }
                            StringBuilder sb = new StringBuilder("TODAY\n");
                            if (response.body().messSessionCounts != null) {
                                for (MessSessionCount c : response.body().messSessionCounts) {
                                    sb.append(c.sessionName)
                                            .append(" — Generated: ").append(c.generated)
                                            .append("  Printed: ").append(c.printed)
                                            .append("  Pending: ").append(c.pending)
                                            .append("  Failed: ").append(c.failed)
                                            .append("\n");
                                }
                            }
                            countsText.setText(sb.toString().trim());
                        }
                        adapter.notifyDataSetChanged();
                        MessMealTokenPrintWorker.kick(MessMealTokenTodayActivity.this);
                    }

                    @Override
                    public void onFailure(Call<AllApiResponse> call, Throwable t) {
                        POSBillingWalaDatabase db = new POSBillingWalaDatabase(MessMealTokenTodayActivity.this);
                        items.clear();
                        items.addAll(db.getMessMealTokenQueueToday(today));
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private class TokenAdapter extends RecyclerView.Adapter<TokenAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mess_meal_token, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            MessMealTokenItem item = items.get(position);
            holder.tokenLine.setText((item.tokenNumber != null ? item.tokenNumber : "") + "    "
                    + (item.printStatus != null ? item.printStatus : ""));

            String name = item.memberName != null ? item.memberName.trim() : "";
            String mobile = item.memberMobile != null ? item.memberMobile.trim() : "";
            if (mobile.isEmpty() && item.registrationNo != null) {
                mobile = item.registrationNo.trim();
            }
            StringBuilder memberSb = new StringBuilder();
            if (!name.isEmpty()) {
                memberSb.append(name);
            }
            if (!mobile.isEmpty()) {
                if (memberSb.length() > 0) {
                    memberSb.append("  ·  ");
                }
                memberSb.append(mobile);
            }
            if (memberSb.length() == 0 && item.registrationNo != null) {
                memberSb.append(item.registrationNo);
            }
            holder.memberLine.setText(memberSb.toString());
            holder.memberLine.setVisibility(memberSb.length() > 0 ? View.VISIBLE : View.GONE);

            holder.metaLine.setText((item.mealSession != null ? item.mealSession : "") + "  "
                    + (item.createdAt != null ? item.createdAt : ""));
            boolean canRetry = item.printStatus != null
                    && !"PRINTED".equalsIgnoreCase(item.printStatus)
                    && !"CANCELLED".equalsIgnoreCase(item.printStatus);
            holder.btnRetry.setEnabled(canRetry);
            holder.btnCancel.setEnabled(canRetry);
            holder.btnRetry.setOnClickListener(v -> {
                POSBillingWalaDatabase db = new POSBillingWalaDatabase(MessMealTokenTodayActivity.this);
                db.upsertMessMealTokenQueue(item.tokenId, item.tokenNumber, item.registrationNo,
                        item.mealSession, item.date, item.memberName, item.createdAt, "PRINT_PENDING");
                MessMealTokenPrintWorker.kick(MessMealTokenTodayActivity.this);
                Toast.makeText(MessMealTokenTodayActivity.this, R.string.ui_retry_print, Toast.LENGTH_SHORT).show();
            });
            holder.btnCancel.setOnClickListener(v -> Api.getClient(MessMealTokenTodayActivity.this)
                    .cancelMessMealToken(MainActivity.userId, item.tokenId)
                    .enqueue(new Callback<AllApiResponse>() {
                        @Override
                        public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                            loadToday();
                        }

                        @Override
                        public void onFailure(Call<AllApiResponse> call, Throwable t) {
                        }
                    }));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tokenLine, memberLine, metaLine;
            Button btnRetry, btnCancel;

            VH(@NonNull View itemView) {
                super(itemView);
                tokenLine = itemView.findViewById(R.id.tokenLine);
                memberLine = itemView.findViewById(R.id.memberLine);
                metaLine = itemView.findViewById(R.id.metaLine);
                btnRetry = itemView.findViewById(R.id.btnRetry);
                btnCancel = itemView.findViewById(R.id.btnCancel);
            }
        }
    }
}
