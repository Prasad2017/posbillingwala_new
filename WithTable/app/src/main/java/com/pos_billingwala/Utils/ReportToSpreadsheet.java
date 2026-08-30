package com.pos_billingwala.Utils;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Exports tabular report data as a formatted HTML spreadsheet (.xls).
 * Opens clearly in Excel and Google Sheets without Apache POI.
 */
public class ReportToSpreadsheet {

    public static final Handler handler = new Handler(Looper.getMainLooper());

    private final String exportPath;
    private final String reportTitle;
    private List<List<String>> rows;

    public ReportToSpreadsheet(String reportTitle, String exportPath) {
        this.reportTitle = reportTitle != null ? reportTitle : "Report";
        this.exportPath = exportPath;
    }

    public void exportReport(final List<List<String>> data, final String fileName,
                             final ExportListener listener) {
        exportReport(data, fileName, null, listener);
    }

    public void exportReport(final List<List<String>> data, final String fileName,
                             final String subtitle, final ExportListener listener) {
        rows = data;
        if (listener != null) {
            listener.onStart();
        }
        new Thread(() -> {
            try {
                writeHtmlSpreadsheet(fileName, subtitle);
                String fullPath = exportPath + fileName;
                if (listener != null) {
                    handler.post(() -> listener.onCompleted(fullPath));
                }
            } catch (final Exception e) {
                if (listener != null) {
                    handler.post(() -> listener.onError(e));
                }
            }
        }).start();
    }

    private void writeHtmlSpreadsheet(String fileName, String subtitle) throws Exception {
        File file = new File(exportPath + fileName);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        String html = buildHtml(subtitle);
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(html);
        }
    }

    private String buildHtml(String subtitle) {
        StringBuilder html = new StringBuilder(2048);
        html.append("<html xmlns:o=\"urn:schemas-microsoft-com:office:office\" ")
                .append("xmlns:x=\"urn:schemas-microsoft-com:office:excel\" ")
                .append("xmlns=\"http://www.w3.org/TR/REC-html40\">")
                .append("<head><meta charset=\"UTF-8\">")
                .append("<style>")
                .append("body{font-family:Arial,sans-serif;color:#222;margin:16px;}")
                .append("h2{margin:0 0 4px 0;font-size:18px;}")
                .append(".meta{margin:0 0 12px 0;color:#555;font-size:13px;}")
                .append("table{border-collapse:collapse;width:100%;}")
                .append("th,td{border:1px solid #CCCCCC;padding:6px 8px;font-size:13px;}")
                .append("th{background:#E8F0FE;font-weight:bold;text-align:left;}")
                .append("tr.data:nth-child(even){background:#FAFAFA;}")
                .append("tr.total td{font-weight:bold;background:#F0F0F0;}")
                .append("</style></head><body>");

        html.append("<h2>").append(escapeHtml(reportTitle)).append("</h2>");
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            html.append("<p class=\"meta\">").append(escapeHtml(subtitle.trim())).append("</p>");
        }

        html.append("<table><thead><tr>");
        if (rows != null && !rows.isEmpty()) {
            for (String header : rows.get(0)) {
                html.append("<th>").append(escapeHtml(header)).append("</th>");
            }
            html.append("</tr></thead><tbody>");

            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                boolean totalRow = isTotalRow(row);
                html.append("<tr class=\"").append(totalRow ? "total" : "data").append("\">");
                int columnCount = rows.get(0).size();
                for (int j = 0; j < columnCount; j++) {
                    String cell = j < row.size() ? row.get(j) : "";
                    html.append("<td>").append(escapeHtml(cell)).append("</td>");
                }
                html.append("</tr>");
            }
        }
        html.append("</tbody></table></body></html>");
        return html.toString();
    }

    private static boolean isTotalRow(List<String> row) {
        if (row == null) {
            return false;
        }
        for (String cell : row) {
            if (cell != null && cell.trim().equalsIgnoreCase("Total Amount")) {
                return true;
            }
        }
        return false;
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public interface ExportListener {
        void onStart();

        void onCompleted(String filePath);

        void onError(Exception e);
    }
}
