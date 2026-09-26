package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Model.MessInvoiceResponse;
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

    public InvoiceMessReportAdapter(Context context, List<MessInvoiceResponse> messInvoiceResponseList) {
        this.context = context;
        this.rows = buildRows(messInvoiceResponseList);
    }

    /** Lunch first, then Dinner, then any other meal type — each with its own section header. */
    public static List<Row> buildRows(List<MessInvoiceResponse> source) {
        List<Row> out = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            return out;
        }
        List<MessInvoiceResponse> lunch = new ArrayList<>();
        List<MessInvoiceResponse> dinner = new ArrayList<>();
        List<MessInvoiceResponse> other = new ArrayList<>();
        for (MessInvoiceResponse item : source) {
            String type = normalizeMeal(item != null ? item.getMessType() : null);
            if ("lunch".equals(type)) {
                lunch.add(item);
            } else if ("dinner".equals(type)) {
                dinner.add(item);
            } else {
                other.add(item);
            }
        }
        appendSection(out, "Lunch", lunch);
        appendSection(out, "Dinner", dinner);
        if (!other.isEmpty()) {
            appendSection(out, "Other", other);
        }
        return out;
    }

    private static void appendSection(List<Row> out, String title, List<MessInvoiceResponse> items) {
        if (items.isEmpty()) {
            return;
        }
        out.add(Row.header(title, items.size()));
        int index = 1;
        for (MessInvoiceResponse item : items) {
            out.add(Row.item(item, index++));
        }
    }

    public static String normalizeMeal(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.US);
    }

    public static int countMeal(List<MessInvoiceResponse> source, String meal) {
        if (source == null || source.isEmpty()) {
            return 0;
        }
        String target = normalizeMeal(meal);
        int count = 0;
        for (MessInvoiceResponse item : source) {
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
            }
            h.binding.sectionHeaderTitle.setText(title);
            h.binding.sectionHeaderCount.setText(
                    context.getString(R.string.ui_mess_coupons_count, row.headerCount));
        } else if (holder instanceof ItemHolder) {
            ItemHolder h = (ItemHolder) holder;
            MessInvoiceResponse item = row.item;
            h.binding.srNo.setText("" + row.itemIndex);
            bindDateTime(h, item.getMessInvoiceDate());
            h.binding.memberName.setText(item.getMemberName());
            h.binding.messType.setText(item.getMessType());
            boolean lastInSection = position + 1 >= rows.size() || rows.get(position + 1).isHeader;
            h.binding.rowDivider.setVisibility(lastInSection ? View.GONE : View.VISIBLE);
        }
    }

    /** Date on first line, 12-hour AM/PM time on second line. */
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

    /**
     * @return [date, timeAmPm] — date as yyyy-MM-dd (or original if unparseable),
     *         time as hh:mm a
     */
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
        // Fallback: split on first space if present.
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
        final MessInvoiceResponse item;
        final int itemIndex;

        private Row(boolean isHeader, String headerTitle, int headerCount,
                    MessInvoiceResponse item, int itemIndex) {
            this.isHeader = isHeader;
            this.headerTitle = headerTitle;
            this.headerCount = headerCount;
            this.item = item;
            this.itemIndex = itemIndex;
        }

        static Row header(String title, int count) {
            return new Row(true, title, count, null, 0);
        }

        static Row item(MessInvoiceResponse item, int index) {
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
