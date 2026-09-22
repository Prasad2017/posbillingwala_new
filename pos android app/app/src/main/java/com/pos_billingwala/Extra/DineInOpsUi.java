package com.pos_billingwala.Extra;

import android.app.Activity;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.DiningSessionResponse;
import com.pos_billingwala.Model.PosTableResponse;
import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.Model.TableStatus;
import com.pos_billingwala.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Contextual dine-in table operations UI (join / split / move / transfer / split bill).
 */
public final class DineInOpsUi {

    public interface OpsCallback {
        void onOpsCompleted(@Nullable String navigateToTable);
    }

    private DineInOpsUi() {
    }

    public static void showTableActionsMenu(Activity activity, POSBillingWalaDatabase db,
                                            String currentTable, OpsCallback callback) {
        if (activity == null || db == null || currentTable == null) {
            return;
        }
        DiningSessionResponse session = db.getOpenDiningSessionForTable(currentTable);
        boolean joined = session != null
                && session.getJoinedTableNumbers() != null
                && !session.getJoinedTableNumbers().trim().isEmpty();

        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        labels.add("Join Table");
        actions.add(() -> showJoinPicker(activity, db, currentTable, callback));

        labels.add("Request Bill");
        actions.add(() -> {
            AppExecutors.get().db().execute(() -> {
                com.pos_billingwala.Extra.DineInSettlementHelper.markBillRequested(db, currentTable);
                AppExecutors.get().main(() -> {
                    Toast.makeText(activity, "Bill requested", Toast.LENGTH_SHORT).show();
                    if (callback != null) {
                        callback.onOpsCompleted(currentTable);
                    }
                });
            });
        });

        if (joined) {
            labels.add("Split Table");
            actions.add(() -> confirmSplitTable(activity, db, currentTable, callback));
        }

        labels.add("Move Items");
        actions.add(() -> showMoveItems(activity, db, currentTable, callback));

        labels.add("Transfer Table");
        actions.add(() -> showTransferPicker(activity, db, currentTable, callback));

        labels.add("Split Bill");
        actions.add(() -> showSplitBillMenu(activity, db, currentTable, callback));

        labels.add("Print Bill");
        actions.add(() -> {
            if (callback != null) {
                callback.onOpsCompleted("PRINT:" + currentTable);
            }
        });

        labels.add("Hold / Save");
        actions.add(() -> {
            DineInTableHelper.openOrGetSession(db, currentTable, 0);
            Toast.makeText(activity, "Table held", Toast.LENGTH_SHORT).show();
            if (callback != null) {
                callback.onOpsCompleted("HOLD");
            }
        });

        BottomSheetUi.showSingleChoice(activity, "Table Actions • T" + currentTable,
                labels.toArray(new String[0]), -1, true, index -> {
                    if (index >= 0 && index < actions.size()) {
                        actions.get(index).run();
                    }
                });
    }

    private static void showJoinPicker(Activity activity, POSBillingWalaDatabase db,
                                       String currentTable, OpsCallback callback) {
        List<PosTableResponse> tables = DineInTableHelper.buildFloorTableList(db);
        List<String> labels = new ArrayList<>();
        List<String> numbers = new ArrayList<>();
        for (PosTableResponse t : tables) {
            if (t.getTableNumber() == null || t.getTableNumber().equals(currentTable)) {
                continue;
            }
            // Do not join blocked tables
            if (TableStatus.BLOCKED.equals(t.getDisplayStatus())) {
                continue;
            }
            String status = TableStatus.displayLabel(t.getDisplayStatus());
            labels.add(t.getDisplayCode() + " — " + status
                    + (t.getCurrentAmount() > 0 ? "  " + DineInTableHelper.formatAmount(t.getCurrentAmount()) : ""));
            numbers.add(t.getTableNumber());
        }
        if (labels.isEmpty()) {
            Toast.makeText(activity, "No tables available to join", Toast.LENGTH_SHORT).show();
            return;
        }
        BottomSheetUi.showSingleChoice(activity, "Join with", labels.toArray(new String[0]), -1, true, index -> {
            if (index < 0 || index >= numbers.size()) {
                return;
            }
            String secondary = numbers.get(index);
            DiningSessionResponse session = db.getOpenDiningSessionForTable(currentTable);
            final String expectedVersion = session != null ? session.getSessionVersion() : null;
            AppExecutors.get().db().execute(() -> {
                String err = db.joinTables(currentTable, secondary, expectedVersion);
                AppExecutors.get().main(() -> {
                    if (err != null) {
                        Toast.makeText(activity, err, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, "Joined T" + currentTable + " + T" + secondary,
                                Toast.LENGTH_SHORT).show();
                        if (callback != null) {
                            callback.onOpsCompleted(currentTable);
                        }
                    }
                });
            });
        });
    }

    private static void confirmSplitTable(Activity activity, POSBillingWalaDatabase db,
                                          String currentTable, OpsCallback callback) {
        BottomSheetUi.showConfirm(activity,
                "Split Table",
                "Unjoin tables? Items stay on T" + currentTable + ". Use Move Items to reassign.",
                "Split",
                activity.getString(android.R.string.cancel),
                true,
                () -> AppExecutors.get().db().execute(() -> {
                    String err = db.splitJoinedTables(currentTable);
                    AppExecutors.get().main(() -> {
                        if (err != null) {
                            Toast.makeText(activity, err, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(activity, "Tables split", Toast.LENGTH_SHORT).show();
                            if (callback != null) {
                                callback.onOpsCompleted(currentTable);
                            }
                        }
                    });
                }));
    }

    private static void showTransferPicker(Activity activity, POSBillingWalaDatabase db,
                                           String currentTable, OpsCallback callback) {
        List<PosTableResponse> tables = DineInTableHelper.buildFloorTableList(db);
        List<String> labels = new ArrayList<>();
        List<String> numbers = new ArrayList<>();
        for (PosTableResponse t : tables) {
            if (t.getTableNumber() == null || t.getTableNumber().equals(currentTable)) {
                continue;
            }
            if (!TableStatus.AVAILABLE.equals(t.getDisplayStatus())) {
                continue;
            }
            labels.add(t.getDisplayCode() + " — Available");
            numbers.add(t.getTableNumber());
        }
        if (labels.isEmpty()) {
            Toast.makeText(activity, "No available table to transfer", Toast.LENGTH_SHORT).show();
            return;
        }
        BottomSheetUi.showSingleChoice(activity, "Transfer to", labels.toArray(new String[0]), -1, true, index -> {
            if (index < 0 || index >= numbers.size()) {
                return;
            }
            String target = numbers.get(index);
            DiningSessionResponse session = db.getOpenDiningSessionForTable(currentTable);
            final String expectedVersion = session != null ? session.getSessionVersion() : null;
            AppExecutors.get().db().execute(() -> {
                String err = db.transferTableSession(currentTable, target, expectedVersion);
                AppExecutors.get().main(() -> {
                    if (err != null) {
                        Toast.makeText(activity, err, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, "Transferred to T" + target, Toast.LENGTH_SHORT).show();
                        if (callback != null) {
                            callback.onOpsCompleted(target);
                        }
                    }
                });
            });
        });
    }

    private static void showMoveItems(Activity activity, POSBillingWalaDatabase db,
                                      String currentTable, OpsCallback callback) {
        List<ProductCartResponse> cart = db.getCartProductList(currentTable, DineInTableHelper.CART_ORDER_TABLE);
        if (cart == null || cart.isEmpty()) {
            Toast.makeText(activity, activity.getString(R.string.toast_cart_is_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        View content = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_move_items, null);
        LinearLayout list = content.findViewById(R.id.moveItemsList);
        TextView btnContinue = content.findViewById(R.id.moveItemsContinue);
        TextView btnCancel = content.findViewById(R.id.moveItemsCancel);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, true);

        List<CheckBox> checks = new ArrayList<>();
        List<ProductCartResponse> lines = new ArrayList<>(cart);
        for (ProductCartResponse line : lines) {
            CheckBox cb = new CheckBox(activity);
            cb.setText(line.getDisplayLineName() + "  ×" + line.getProductQuantity());
            cb.setTag(line.getCartId());
            list.addView(cb);
            checks.add(cb);
        }

        btnCancel.setOnClickListener(v -> sheet.dismiss());
        btnContinue.setOnClickListener(v -> {
            List<String> selectedIds = new ArrayList<>();
            for (CheckBox cb : checks) {
                if (cb.isChecked() && cb.getTag() != null) {
                    selectedIds.add(String.valueOf(cb.getTag()));
                }
            }
            if (selectedIds.isEmpty()) {
                Toast.makeText(activity, "Select at least one item", Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            pickMoveTarget(activity, db, currentTable, selectedIds, callback);
        });
    }

    private static void pickMoveTarget(Activity activity, POSBillingWalaDatabase db,
                                       String currentTable, List<String> cartIds, OpsCallback callback) {
        List<PosTableResponse> tables = DineInTableHelper.buildFloorTableList(db);
        List<String> labels = new ArrayList<>();
        List<String> numbers = new ArrayList<>();
        for (PosTableResponse t : tables) {
            if (t.getTableNumber() == null || t.getTableNumber().equals(currentTable)) {
                continue;
            }
            if (TableStatus.BLOCKED.equals(t.getDisplayStatus())) {
                continue;
            }
            labels.add(t.getDisplayCode() + " — " + TableStatus.displayLabel(t.getDisplayStatus()));
            numbers.add(t.getTableNumber());
        }
        if (labels.isEmpty()) {
            Toast.makeText(activity, "No target table", Toast.LENGTH_SHORT).show();
            return;
        }
        BottomSheetUi.showSingleChoice(activity, "Move items to", labels.toArray(new String[0]), -1, true, index -> {
            if (index < 0 || index >= numbers.size()) {
                return;
            }
            String target = numbers.get(index);
            AppExecutors.get().db().execute(() -> {
                boolean ok = db.moveCartItemsToTable(cartIds, target);
                if (ok) {
                    DineInTableHelper.openOrGetSession(db, target, 0);
                }
                final boolean success = ok;
                AppExecutors.get().main(() -> {
                    if (!success) {
                        Toast.makeText(activity, "Move failed", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, "Items moved to T" + target, Toast.LENGTH_SHORT).show();
                        if (callback != null) {
                            callback.onOpsCompleted(currentTable);
                        }
                    }
                });
            });
        });
    }

    private static void showSplitBillMenu(Activity activity, POSBillingWalaDatabase db,
                                          String currentTable, OpsCallback callback) {
        float total = DineInTableHelper.computeCartTotal(db, currentTable);
        if (total <= 0f) {
            Toast.makeText(activity, activity.getString(R.string.toast_cart_is_empty), Toast.LENGTH_SHORT).show();
            return;
        }
        String[] options = new String[]{"Split Equally", "Split by Item", "Split by Amount"};
        BottomSheetUi.showSingleChoice(activity, "Split Bill • " + DineInTableHelper.formatAmount(total),
                options, -1, true, index -> {
                    if (index == 0) {
                        showSplitEqually(activity, db, currentTable, total, callback);
                    } else if (index == 1) {
                        showSplitByItem(activity, db, currentTable, callback);
                    } else if (index == 2) {
                        showSplitByAmount(activity, db, currentTable, total, callback);
                    }
                });
    }

    private static void showSplitEqually(Activity activity, POSBillingWalaDatabase db,
                                         String currentTable, float total, OpsCallback callback) {
        View content = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_split_input, null);
        TextView title = content.findViewById(R.id.splitInputTitle);
        TextView hint = content.findViewById(R.id.splitInputHint);
        EditText input = content.findViewById(R.id.splitInputField);
        TextView btnOk = content.findViewById(R.id.splitInputConfirm);
        TextView btnCancel = content.findViewById(R.id.splitInputCancel);
        title.setText("Split Equally");
        hint.setText("Total " + DineInTableHelper.formatAmount(total) + "\nEnter number of shares");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("e.g. 2");
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, true);
        btnCancel.setOnClickListener(v -> sheet.dismiss());
        btnOk.setOnClickListener(v -> {
            int n;
            try {
                n = Integer.parseInt(input.getText().toString().trim());
            } catch (Exception e) {
                n = 0;
            }
            if (n < 2 || n > 20) {
                Toast.makeText(activity, "Enter shares between 2 and 20", Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            float[] shares = equalShares(total, n);
            showPayShares(activity, db, currentTable, total, shares, callback);
        });
    }

    private static float[] equalShares(float total, int n) {
        float[] shares = new float[n];
        // Distribute paise carefully so sum equals total
        int totalPaise = Math.round(total * 100f);
        int base = totalPaise / n;
        int rem = totalPaise % n;
        for (int i = 0; i < n; i++) {
            int paise = base + (i < rem ? 1 : 0);
            shares[i] = paise / 100f;
        }
        return shares;
    }

    private static void showSplitByAmount(Activity activity, POSBillingWalaDatabase db,
                                          String currentTable, float total, OpsCallback callback) {
        View content = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_split_input, null);
        TextView title = content.findViewById(R.id.splitInputTitle);
        TextView hint = content.findViewById(R.id.splitInputHint);
        EditText input = content.findViewById(R.id.splitInputField);
        TextView btnOk = content.findViewById(R.id.splitInputConfirm);
        TextView btnCancel = content.findViewById(R.id.splitInputCancel);
        title.setText("Split by Amount");
        hint.setText("Total " + DineInTableHelper.formatAmount(total)
                + "\nEnter amounts separated by comma\nExample: 1000,600,400");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("1000,600,400");
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, true);
        btnCancel.setOnClickListener(v -> sheet.dismiss());
        btnOk.setOnClickListener(v -> {
            String raw = input.getText() != null ? input.getText().toString().trim() : "";
            String[] parts = raw.split(",");
            List<Float> amounts = new ArrayList<>();
            float sum = 0f;
            try {
                for (String p : parts) {
                    String t = p.trim();
                    if (t.isEmpty()) {
                        continue;
                    }
                    float a = Float.parseFloat(t);
                    if (a <= 0f) {
                        throw new IllegalArgumentException();
                    }
                    amounts.add(a);
                    sum += a;
                }
            } catch (Exception e) {
                Toast.makeText(activity, "Invalid amounts", Toast.LENGTH_SHORT).show();
                return;
            }
            if (amounts.size() < 2) {
                Toast.makeText(activity, "Enter at least 2 amounts", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Math.abs(sum - total) > 0.05f) {
                Toast.makeText(activity, "Amounts must equal "
                        + DineInTableHelper.formatAmount(total), Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            float[] shares = new float[amounts.size()];
            for (int i = 0; i < amounts.size(); i++) {
                shares[i] = amounts.get(i);
            }
            showPayShares(activity, db, currentTable, total, shares, callback);
        });
    }

    private static void showSplitByItem(Activity activity, POSBillingWalaDatabase db,
                                        String currentTable, OpsCallback callback) {
        List<ProductCartResponse> cart = db.getCartProductList(currentTable, DineInTableHelper.CART_ORDER_TABLE);
        if (cart == null || cart.size() < 2) {
            Toast.makeText(activity, "Need at least 2 items to split by item", Toast.LENGTH_SHORT).show();
            return;
        }
        View content = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_move_items, null);
        TextView title = content.findViewById(R.id.moveItemsTitle);
        LinearLayout list = content.findViewById(R.id.moveItemsList);
        TextView btnContinue = content.findViewById(R.id.moveItemsContinue);
        TextView btnCancel = content.findViewById(R.id.moveItemsCancel);
        title.setText("Bill 1 items (rest → Bill 2)");
        btnContinue.setText("Continue");
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, true);

        List<CheckBox> checks = new ArrayList<>();
        for (ProductCartResponse line : cart) {
            CheckBox cb = new CheckBox(activity);
            cb.setText(line.getDisplayLineName() + "  ×" + line.getProductQuantity());
            cb.setTag(line);
            list.addView(cb);
            checks.add(cb);
        }
        btnCancel.setOnClickListener(v -> sheet.dismiss());
        btnContinue.setOnClickListener(v -> {
            List<ProductCartResponse> bill1 = new ArrayList<>();
            List<ProductCartResponse> bill2 = new ArrayList<>();
            for (CheckBox cb : checks) {
                ProductCartResponse line = (ProductCartResponse) cb.getTag();
                if (cb.isChecked()) {
                    bill1.add(line);
                } else {
                    bill2.add(line);
                }
            }
            if (bill1.isEmpty() || bill2.isEmpty()) {
                Toast.makeText(activity, "Assign items to both bills", Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            float a1 = estimateLinesTotal(db, bill1);
            float a2 = estimateLinesTotal(db, bill2);
            // Normalize so sum equals table total
            float tableTotal = DineInTableHelper.computeCartTotal(db, currentTable);
            float raw = a1 + a2;
            if (raw > 0f && Math.abs(raw - tableTotal) > 0.05f) {
                float scale = tableTotal / raw;
                a1 = Math.round(a1 * scale * 100f) / 100f;
                a2 = Math.round((tableTotal - a1) * 100f) / 100f;
            }
            showPayShares(activity, db, currentTable, tableTotal, new float[]{a1, a2}, callback);
        });
    }

    private static float estimateLinesTotal(POSBillingWalaDatabase db, List<ProductCartResponse> lines) {
        float sum = 0f;
        for (ProductCartResponse line : lines) {
            float price = ReportCursorHelper.parseAmount(line.getResolvedLinePrice());
            float qty = ReportCursorHelper.parseAmount(line.getProductQuantity());
            sum += price * qty;
        }
        return (float) Math.ceil(sum);
    }

    private static void showPayShares(Activity activity, POSBillingWalaDatabase db,
                                      String currentTable, float tableTotal,
                                      float[] shares, OpsCallback callback) {
        DiningSessionResponse session = DineInTableHelper.openOrGetSession(db, currentTable, 0);
        if (session == null) {
            Toast.makeText(activity, "No dining session", Toast.LENGTH_SHORT).show();
            return;
        }
        float alreadyPaid = 0f;
        try {
            if (session.getPaidAmount() != null && !session.getPaidAmount().isEmpty()) {
                alreadyPaid = Float.parseFloat(session.getPaidAmount());
            }
        } catch (Exception ignored) {
        }

        List<String> labels = new ArrayList<>();
        for (int i = 0; i < shares.length; i++) {
            labels.add("Bill " + (i + 1) + "  " + DineInTableHelper.formatAmount(shares[i]));
        }
        float remaining = Math.max(0f, tableTotal - alreadyPaid);
        BottomSheetUi.showSingleChoice(activity,
                "Pay share (remaining " + DineInTableHelper.formatAmount(remaining) + ")",
                labels.toArray(new String[0]), -1, true, index -> {
                    if (index < 0 || index >= shares.length) {
                        return;
                    }
                    float share = shares[index];
                    View payContent = LayoutInflater.from(activity).inflate(R.layout.set_payment_mode_dialog, null);
                    BottomSheetDialog paySheet = BottomSheetUi.showContent(activity, payContent, false);
                    PaymentSettlementBinder.bind(payContent, share, MainActivity.currencyName, null,
                            new PaymentSettlementBinder.Callback() {
                                @Override
                                public void onConfirmed(String mode, String cashAmount, String upiAmount) {
                                    if (mode == null || mode.isEmpty()) {
                                        Toast.makeText(activity,
                                                activity.getString(R.string.toast_please_select_payment_mode),
                                                Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    paySheet.dismiss();
                                    AppExecutors.get().db().execute(() -> {
                                        db.addSessionPaidAmount(session.getSessionId(), share);
                                        DiningSessionResponse updated =
                                                db.getOpenDiningSessionForTable(currentTable);
                                        float paid = 0f;
                                        try {
                                            if (updated != null && updated.getPaidAmount() != null) {
                                                paid = Float.parseFloat(updated.getPaidAmount());
                                            }
                                        } catch (Exception ignored) {
                                        }
                                        boolean fullyPaid = paid + 0.05f >= tableTotal;
                                        if (fullyPaid && updated != null) {
                                            db.updateDiningSessionStatus(updated.getSessionId(),
                                                    TableStatus.PARTIALLY_PAID.equals(updated.getSessionStatus())
                                                            ? TableStatus.PARTIALLY_PAID
                                                            : TableStatus.PAYMENT_PENDING);
                                            // Mark payment pending until final invoice settlement via Pay
                                            db.updateDiningSessionStatus(updated.getSessionId(),
                                                    TableStatus.PARTIALLY_PAID);
                                        } else if (updated != null) {
                                            db.updateDiningSessionStatus(updated.getSessionId(),
                                                    TableStatus.PARTIALLY_PAID);
                                        }
                                        final boolean done = fullyPaid;
                                        final float paidFinal = paid;
                                        AppExecutors.get().main(() -> {
                                            Toast.makeText(activity,
                                                    "Paid " + DineInTableHelper.formatAmount(share)
                                                            + " (total paid "
                                                            + DineInTableHelper.formatAmount(paidFinal) + ")",
                                                    Toast.LENGTH_SHORT).show();
                                            if (done) {
                                                Toast.makeText(activity,
                                                        "All shares paid — use PAY to print & close table",
                                                        Toast.LENGTH_LONG).show();
                                            } else {
                                                // Offer next share
                                                showPayShares(activity, db, currentTable, tableTotal, shares, callback);
                                            }
                                            if (callback != null) {
                                                callback.onOpsCompleted(currentTable);
                                            }
                                        });
                                    });
                                }

                                @Override
                                public void onDismissed() {
                                    paySheet.dismiss();
                                }
                            });
                });
    }
}
