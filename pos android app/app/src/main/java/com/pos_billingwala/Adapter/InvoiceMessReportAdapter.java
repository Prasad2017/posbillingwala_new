package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Model.MessReportItem;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.InvoiceMessReportListBinding;
import com.pos_billingwala.databinding.ItemMessReportSectionHeaderBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


@SuppressLint("SetTextI18n")
public class InvoiceMessReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final Context context;
    private final List<Row> rows;

    public InvoiceMessReportAdapter(Context context, List<MessReportItem> items) {
        this.context = context;
        this.rows = buildRows(items);
    }

    /**
     * Top-level: Paper Coupons vs QR Tokens.
     * Within each: Lunch, Dinner, Other.
     */
    public static List<Row> buildRows(List<MessReportItem> source) {
        List<Row> out = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            return out;
        }
        List<MessReportItem> coupons = new ArrayList<>();
        List<MessReportItem> qrTokens = new ArrayList<>();
        for (MessReportItem item : source) {
            if (item == null) continue;
            if (item.isQr()) {
                qrTokens.add(item);
            } else {
                coupons.add(item);
            }
        }
        appendSourceSection(out, "Paper Coupons", coupons);
        appendSourceSection(out, "QR Tokens", qrTokens);
        return out;
    }

    private static void appendSourceSection(List<Row> out, String sourceTitle, List<MessReportItem> items) {
        if (items.isEmpty()) {
            return;
        }
        out.add(Row.header(sourceTitle, items.size()));
        List<MessReportItem> lunch = new ArrayList<>();
        List<MessReportItem> dinner = new ArrayList<>();
        List<MessReportItem> other = new ArrayList<>();
        for (MessReportItem item : items) {
            String type = normalizeMeal(item.getMessType());
            if ("lunch".equals(type)) {
                lunch.add(item);
            } else if ("dinner".equals(type)) {
                dinner.add(item);
            } else {
                other.add(item);
            }
        }
        appendMealSection(out, "Lunch", lunch);
        appendMealSection(out, "Dinner", dinner);
        if (!other.isEmpty()) {
            appendMealSection(out, "Other", other);
        }
    }

    private static void appendMealSection(List<Row> out, String title, List<MessReportItem> items) {
        if (items.isEmpty()) {
            return;
        }
        out.add(Row.header(title, items.size()));
        int index = 1;
        for (MessReportItem item : items) {
            out.add(Row.item(item, index++));
        }
    }

    public static String normalizeMeal(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.US);
    }

    public static int countSource(List<MessReportItem> source, boolean qr) {
        if (source == null || source.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (MessReportItem item : source) {
            if (item == null) continue;
            if (qr == item.isQr()) {
                count++;
            }
        }
        return count;
    }

    public static int countMeal(List<MessReportItem> source, String meal) {
        if (source == null || source.isEmpty()) {
            return 0;
        }
        String target = normalizeMeal(meal);
        int count = 0;
        for (MessReportItem item : source) {
            if (target.equals(normalizeMeal(item != null ? item.getMessType() : null))) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).isHeader ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            return new HeaderHolder(ItemMessReportSectionHeaderBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }
        return new ItemHolder(InvoiceMessReportListBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            HeaderHolder h = (HeaderHolder) holder;
            String title = row.headerTitle;
            if ("Lunch".equalsIgnoreCase(title)) {
                title = context.getString(R.string.ui_lunch);
            } else if ("Dinner".equalsIgnoreCase(title)) {
                title = context.getString(R.string.ui_dinner);
            } else if ("Paper Coupons".equalsIgnoreCase(title)) {
                title = context.getString(R.string.ui_mess_coupons_section);
            } else if ("QR Tokens".equalsIgnoreCase(title)) {
                title = context.getString(R.string.ui_mess_qr_tokens_section);
            }
            h.binding.sectionHeaderTitle.setText(title);
            boolean isSource = "Paper Coupons".equalsIgnoreCase(row.headerTitle)
                    || "QR Tokens".equalsIgnoreCase(row.headerTitle);
            h.binding.sectionHeaderCount.setText(isSource
                    ? context.getString(R.string.ui_mess_items_count, row.headerCount)
                    : context.getString(R.string.ui_mess_coupons_count, row.headerCount));
        } else if (holder instanceof ItemHolder) {
            ItemHolder h = (ItemHolder) holder;
            MessReportItem item = row.item;
            h.binding.srNo.setText("" + row.itemIndex);
            bindDateTime(h, item.getDateTime());
            h.binding.memberName.setText(item.getMemberName());
            h.binding.messType.setText(item.displayType());
            boolean lastInSection = position + 1 >= rows.size() || rows.get(position + 1).isHeader;
            h.binding.rowDivider.setVisibility(lastInSection ? View.GONE : View.VISIBLE);
        }
    }

    private static void bindDateTime(ItemHolder holder, String raw) {
        String[] parts = formatDateTimeParts(raw);
        holder.binding.invoiceDate.setText(parts[0]);
        if (parts[1].isEmpty()) {
            holder.binding.invoiceTime.setVisibility(View.GONE);
            holder.binding.invoiceTime.setText("");
        } else {
            holder.binding.invoiceTime.setVisibility(View.VISIBLE);
            holder.binding.invoiceTime.setText(parts[1]);
        }
    }

    public static String[] formatDateTimeParts(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new String[]{"", ""};
        }
        String value = raw.trim();
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                Date date = new SimpleDateFormat(pattern, Locale.US).parse(value);
                if (date == null) {
                    continue;
                }
                String datePart = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date);
                if ("yyyy-MM-dd".equals(pattern)) {
                    return new String[]{datePart, ""};
                }
                String timePart = new SimpleDateFormat("hh:mm a", Locale.US).format(date);
                return new String[]{datePart, timePart};
            } catch (ParseException ignored) {
            }
        }
        int space = value.indexOf(' ');
        if (space > 0 && space < value.length() - 1) {
            return new String[]{value.substring(0, space), value.substring(space + 1)};
        }
        return new String[]{value, ""};
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    public static final class Row {
        final boolean isHeader;
        final String headerTitle;
        final int headerCount;
        final MessReportItem item;
        final int itemIndex;

        private Row(boolean isHeader, String headerTitle, int headerCount,
                    MessReportItem item, int itemIndex) {
            this.isHeader = isHeader;
            this.headerTitle = headerTitle;
            this.headerCount = headerCount;
            this.item = item;
            this.itemIndex = itemIndex;
        }

        static Row header(String title, int count) {
            return new Row(true, title, count, null, 0);
        }

        static Row item(MessReportItem item, int index) {
            return new Row(false, null, 0, item, index);
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final ItemMessReportSectionHeaderBinding binding;

        HeaderHolder(ItemMessReportSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        final InvoiceMessReportListBinding binding;

        ItemHolder(InvoiceMessReportListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
