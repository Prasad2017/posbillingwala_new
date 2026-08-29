package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.work.WorkManager;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.AppExecutors;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.NetworkToOffline.CloudSyncTracker;
import com.pos_billingwala.NetworkToOffline.UserSynchronizeData;
import com.pos_billingwala.NetworkToOffline.WorkerClass.UserSynchronizeWorker;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentCloudSyncStatusBinding;

public class CloudSyncStatus extends Fragment {

    private Activity activity;
    private FragmentCloudSyncStatusBinding binding;
    private CloudSyncTracker.Snapshot latest;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCloudSyncStatusBinding.inflate(inflater, container, false);
        activity = getActivity();
        binding.toolbar.toolbarTitle.setText(getString(R.string.setting_synchronize));
        binding.toolbar.backButton.setOnClickListener(v -> ((MainActivity) activity).navigateBack());
        binding.actionButton.setOnClickListener(v -> onActionClicked());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CloudSyncTracker.invalidateSticky();
        CloudSyncTracker.live().observe(getViewLifecycleOwner(), snapshot -> {
            if (snapshot != null) {
                bindSnapshot(snapshot);
            }
        });
        WorkManager.getInstance(requireContext())
                .getWorkInfosForUniqueWorkLiveData(UserSynchronizeWorker.UNIQUE_NAME)
                .observe(getViewLifecycleOwner(), infos -> reloadCounts());
        reloadCounts();
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadCounts();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).lockUnlockDrawer(1);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void reloadCounts() {
        if (activity == null) {
            return;
        }
        AppExecutors.get().runDbThenMain(this,
                () -> latest = CloudSyncTracker.refresh(activity),
                () -> {
                    if (latest != null) {
                        bindSnapshot(latest);
                    }
                });
    }

    private void bindSnapshot(@Nullable CloudSyncTracker.Snapshot snapshot) {
        if (binding == null || snapshot == null || !isAdded()) {
            return;
        }
        latest = snapshot;
        if (snapshot.running) {
            String title = snapshot.currentTitle;
            if (title == null || title.trim().isEmpty()) {
                title = getString(R.string.sync_notification_uploading);
            }
            binding.summaryTitle.setText(title);
            binding.summarySubtitle.setText(getString(R.string.sync_summary_uploading,
                    snapshot.uploadedThisRun, snapshot.pendingTotal));
        } else if (snapshot.pendingTotal == 0) {
            binding.summaryTitle.setText(getString(R.string.sync_summary_all_complete));
            if (snapshot.uploadedThisRun > 0) {
                binding.summarySubtitle.setText(getString(R.string.sync_summary_uploaded_count, snapshot.uploadedThisRun));
            } else {
                binding.summarySubtitle.setText(getString(R.string.sync_summary_nothing_pending));
            }
        } else {
            binding.summaryTitle.setText(getString(R.string.sync_summary_pending_count, snapshot.pendingTotal));
            if (snapshot.uploadedThisRun > 0) {
                binding.summarySubtitle.setText(getString(R.string.sync_summary_uploaded_remaining,
                        snapshot.uploadedThisRun, snapshot.pendingTotal));
            } else if (!snapshot.lastSuccess) {
                binding.summarySubtitle.setText(getString(R.string.sync_summary_failed));
            } else {
                binding.summarySubtitle.setText(getString(R.string.sync_summary_ready));
            }
        }
        renderTables(snapshot);
        updateActionButton(snapshot);
    }

    private void renderTables(@NonNull CloudSyncTracker.Snapshot snapshot) {
        LinearLayout list = binding.tableList;
        LayoutInflater inflater = LayoutInflater.from(activity);
        int needed = snapshot.tables.size();
        while (list.getChildCount() > needed) {
            list.removeViewAt(list.getChildCount() - 1);
        }
        for (int i = 0; i < needed; i++) {
            CloudSyncTracker.TableStatus table = snapshot.tables.get(i);
            View row = i < list.getChildCount()
                    ? list.getChildAt(i)
                    : inflater.inflate(R.layout.item_cloud_sync_table, list, false);
            if (i >= list.getChildCount()) {
                list.addView(row);
            }
            ImageView icon = row.findViewById(R.id.statusIcon);
            TextView name = row.findViewById(R.id.tableName);
            TextView status = row.findViewById(R.id.tableStatus);
            View divider = row.findViewById(R.id.rowDivider);
            if (divider != null) {
                divider.setVisibility(i == 0 ? View.GONE : View.VISIBLE);
            }
            name.setText(getString(table.labelRes));
            if (table.complete) {
                icon.setBackgroundResource(R.drawable.bg_sync_status_complete);
                icon.setImageResource(R.drawable.ic_check_white);
                status.setText(getString(R.string.sync_status_complete));
                status.setTextColor(ContextCompat.getColor(activity, R.color.statusActive));
            } else if (table.inProgress) {
                icon.setBackgroundResource(R.drawable.bg_sync_status_pending);
                icon.setImageResource(R.drawable.ic_sync_pending_dot);
                status.setText(getString(R.string.sync_status_syncing));
                status.setTextColor(ContextCompat.getColor(activity, R.color.statusTrial));
            } else {
                icon.setBackgroundResource(R.drawable.bg_sync_status_pending);
                icon.setImageResource(R.drawable.ic_sync_pending_dot);
                status.setText(getString(R.string.sync_status_pending_count, table.pending));
                status.setTextColor(ContextCompat.getColor(activity, R.color.statusTrial));
            }
        }
    }

    private void updateActionButton(@NonNull CloudSyncTracker.Snapshot snapshot) {
        if (snapshot.running) {
            binding.actionButton.setEnabled(false);
            binding.actionButton.setAlpha(0.6f);
            binding.actionButton.setText(R.string.sync_action_syncing);
            return;
        }
        binding.actionButton.setEnabled(true);
        binding.actionButton.setAlpha(1f);
        if (snapshot.pendingTotal > 0) {
            binding.actionButton.setText(R.string.sync_action_sync);
        } else {
            binding.actionButton.setText(R.string.sync_action_done);
        }
    }

    private void onActionClicked() {
        CloudSyncTracker.Snapshot snapshot = latest;
        if (snapshot != null && snapshot.running) {
            return;
        }
        if (snapshot == null || snapshot.pendingTotal > 0) {
            if (!DetectConnection.checkInternetConnection(activity)) {
                DetectConnection.noInternetConnection(activity);
                return;
            }
            UserSynchronizeData.start(activity, false);
            reloadCounts();
            return;
        }
        ((MainActivity) activity).navigateBack();
    }
}
