package com.pos_billingwala.Fragment;

import com.pos_billingwala.Extra.PopupUi;
import static com.pos_billingwala.Utils.RequestCodes.directory_path;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.dewinjm.monthyearpicker.MonthYearPickerDialog;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.ReportAdapter;
import com.pos_billingwala.BuildConfig;
import com.pos_billingwala.CalenderView.MonthPickerDialog;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.Extra.ReportUiHelper;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.ReportRankItem;
import com.pos_billingwala.R;
import com.pos_billingwala.Utils.ReportToSpreadsheet;
import com.pos_billingwala.Extra.OperationalReportCharts;
import com.pos_billingwala.databinding.FragmentOperationalReportBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;

@SuppressLint("StaticFieldLeak, NonConstantResourceId, NotifyDataSetChanged, SetTextI18n")
public class InvoiceReport extends Fragment implements View.OnClickListener {


    public static Activity activity;
    static int pageNumber = 0, totalPages, limit = 25;
    public int mYear, mMonth, mDay;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    ReportAdapter adapter;
    List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
    Calendar calender;
    DatePickerDialog datePickerDialog;
    String invoiceDate = "";
    int PERMISSION_ALL = 1;
    SweetAlertDialog pDialog;
    boolean isLoading = false, isDateMonthWise = false;
    FragmentOperationalReportBinding binding;


    public static boolean hasPermissions(Context context, String... permissions) {
        // Get current android os version.
        int currentAndroidVersion = Build.VERSION.SDK_INT;
        // Build.VERSION_CODES.M's value is 23.
        if (currentAndroidVersion >= Build.VERSION_CODES.M) {
            if (context != null && permissions != null) {
                for (String permission : permissions) {
                    if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        createFolder();
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static void createFolder() {

        File myDirectory = new File(directory_path + "/Sale");
        if (!myDirectory.exists()) {
            myDirectory.mkdirs();
        }

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOperationalReportBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        ReportUiHelper.applyOperationalReportLayout(activity, binding);
        binding.toolbar.heading.setText(getString(R.string.ui_invoice_reports));
        binding.toolbar.shareInvoice.setVisibility(View.VISIBLE);
        binding.listTitle.setText(getString(R.string.ui_invoice_sale));
        binding.donutTitle.setText(getString(R.string.ui_billing_wise_details));
        binding.barTitle.setText(getString(R.string.ui_sales_trend));

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).navigateBack();
                    return true;
                }
                return false;
            }
        });

        //Add runtime permissions
        String[] PERMISSIONS = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        if (!hasPermissions(activity, PERMISSIONS)) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSION_ALL);
        }

        binding.nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(@NonNull NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (ReportCursorHelper.isNestedScrollAtBottom(v, scrollY)) {
                    if (!isLoading && pageNumber < totalPages) {
                        new getDownloadBills().execute();
                    }
                }
            }
        });

        binding.toolbar.backToSetting.setOnClickListener(this);
        binding.toolbar.menuIcon.setOnClickListener(this);
        binding.toolbar.shareInvoice.setOnClickListener(this);

        return view;

    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.menuIcon) {
            setPopUpWindow();
        } else if (id == R.id.shareInvoice) {
            if (!invoiceResponseList.isEmpty()) {
                exportSale();
            }
        }
    }

    public void exportSale() {

        ReportToSpreadsheet reportExport;
        List<List<String>> reportList = new ArrayList<>();

        List<String> col_name = new ArrayList<>();
        col_name.add("SR No");
        col_name.add("Invoice Date");
        col_name.add("Invoice Number");
        col_name.add("Invoice Amount ( " + MainActivity.currencyName + " )");
        reportList.add(col_name);

        for (int i = 0; i < invoiceResponseList.size(); i++) {
            List<String> columnList = new ArrayList<>();
            InvoiceResponse invoiceResponse = invoiceResponseList.get(i);
            if (invoiceResponse != null) {
                columnList.add(String.valueOf(i + 1));
                columnList.add(invoiceResponse.getInvoiceDate());
                columnList.add(invoiceResponse.getInvoiceNumber());
                columnList.add(invoiceResponse.getTotalAmount());
                reportList.add(columnList);
            }
        }

        List<String> columnList = new ArrayList<>();
        columnList.add("");
        columnList.add("");
        columnList.add("Total Amount");
        columnList.add(binding.totalAmount.getText().toString());
        reportList.add(columnList);

        reportExport = new ReportToSpreadsheet("Sale Invoices", directory_path);
        reportExport.exportReport(reportList, "/Sale/InvoiceSale.xls", buildExportSubtitle(),
                new ReportToSpreadsheet.ExportListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void onCompleted(String filePath) {
                Toast.makeText(activity, getString(R.string.toast_sales_report_successfully_exported), Toast.LENGTH_SHORT).show();
                openGeneratedPDF();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void openGeneratedReport(String FilePath) {
        File file = new File(FilePath);
        if (file.exists()) {
            Intent excelIntent = new Intent(Intent.ACTION_VIEW);
            Uri uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
            excelIntent.setDataAndType(uri, "application/vnd.ms-excel");
            excelIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try {
                startActivity(excelIntent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(activity, getString(R.string.toast_no_application_available_to_view_excel), Toast.LENGTH_LONG).show();
            }
        }
    }

    private String buildExportSubtitle() {
        if (invoiceDate != null && !invoiceDate.isEmpty()) {
            return "Period: " + invoiceDate;
        }
        return "All records";
    }

    public void openGeneratedPDF() {

        File file = new File(directory_path + "/Sale/InvoiceSale.xls");
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
        intentShareFile.setType("application/vnd.ms-excel");
        intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);
        List<ResolveInfo> resInfoList = activity.getPackageManager().queryIntentActivities(intentShareFile, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            activity.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        startActivity(Intent.createChooser(intentShareFile, "Share Invoice Sale"));

    }

    public void setPopUpWindow() {

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.sale_wise_dialog, null);
        PopupWindow mypopupWindow = PopupUi.create(activity, view);

        LinearLayout dayWiseLayout = view.findViewById(R.id.dayWiseLayout);
        LinearLayout monthWiseLayout = view.findViewById(R.id.monthWiseLayout);
        LinearLayout yearWiseLayout = view.findViewById(R.id.yearWiseLayout);

        //Get Current Date
        calender = Calendar.getInstance();
        mYear = calender.get(Calendar.YEAR);
        mMonth = calender.get(Calendar.MONTH);
        mDay = calender.get(Calendar.DAY_OF_MONTH);

        dayWiseLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();

                datePickerDialog = new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        if ((monthOfYear + 1) > 9) {
                            if ((dayOfMonth) > 9) {
                                invoiceDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                                getDateReportList(invoiceDate);
                            } else {
                                invoiceDate = year + "-" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getDateReportList(invoiceDate);
                            }
                        } else {
                            if ((dayOfMonth) > 9) {
                                invoiceDate = year + "-" + "0" + (monthOfYear + 1) + "-" + dayOfMonth;
                                getDateReportList(invoiceDate);
                            } else {
                                invoiceDate = year + "-" + "0" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getDateReportList(invoiceDate);
                            }
                        }
                    }
                }, mYear, mMonth, mDay);

                datePickerDialog.show();
            }
        });

        monthWiseLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();

                MonthYearPickerDialogFragment dialogFragment = MonthYearPickerDialogFragment.getInstance(mMonth, mYear, "Select Month");
                dialogFragment.show(getChildFragmentManager(), null);
                dialogFragment.setOnDateSetListener(new MonthYearPickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(int year, int monthOfYear) {
                        if ((monthOfYear + 1) > 9) {
                            invoiceDate = year + "-" + (monthOfYear + 1);
                            getDateReportList(invoiceDate);
                        } else {
                            invoiceDate = year + "-" + "0" + (monthOfYear + 1);
                            getDateReportList(invoiceDate);
                        }
                    }
                });

            }
        });


        yearWiseLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();
                MonthPickerDialog.Builder builder = new MonthPickerDialog.Builder(activity, new MonthPickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(int selectedMonth, int selectedYear) {
                        invoiceDate = String.valueOf(selectedYear);
                        mYear = selectedYear;
                        getDateReportList(invoiceDate);
                    }
                }, mYear, 0);

                builder.showYearOnly()
                        .setYearRange(1990, 2050)
                        .build()
                        .show();
            }
        });

        PopupUi.showAsToolbarMenu(mypopupWindow, binding.toolbar.menuIcon);

    }

    private void dismissLoader() {
        try {
            if (pDialog != null && pDialog.isShowing()) {
                pDialog.dismiss();
            }
        } catch (Exception ignored) {
        }
        pDialog = null;
    }

    @Override
    public void onDestroyView() {
        dismissLoader();
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getReportList();
    }

    public void getReportList() {
        if (isLoading) {
            return;
        }
        isLoading = true;
        isDateMonthWise = false;
        invoiceDate = "";
        pageNumber = 0;

        pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        new LoadInitialReport("").execute();
    }

    public void getDateReportList(String selectedDate) {
        if (isLoading) {
            return;
        }
        isLoading = true;
        isDateMonthWise = true;
        invoiceDate = selectedDate;
        pageNumber = 0;

        binding.nestedScrollView.setVisibility(View.GONE);
        binding.noDataFound.setVisibility(View.GONE);

        pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        new LoadInitialReport(selectedDate).execute();
    }

    private void removeLoadingFooter() {
        if (!invoiceResponseList.isEmpty()
                && invoiceResponseList.get(invoiceResponseList.size() - 1) == null) {
            int idx = invoiceResponseList.size() - 1;
            invoiceResponseList.remove(idx);
            if (adapter != null) {
                adapter.notifyItemRemoved(idx);
            }
        }
    }

    private void bindReportPage(List<InvoiceResponse> page, float totalAmount) {
        invoiceResponseList = new ArrayList<>();
        if (page != null && !page.isEmpty()) {
            invoiceResponseList.addAll(page);
            adapter = new ReportAdapter(activity, invoiceResponseList);
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
            binding.recyclerView.setAdapter(adapter);
            binding.totalAmount.setText(MainActivity.currencyName + " "
                    + String.format(Locale.US, "%.2f", totalAmount));
            String period = OperationalReportCharts.formatPeriodLabel(
                    isDateMonthWise ? invoiceDate : "");
            List<ReportRankItem> breakdown = OperationalReportCharts.groupedBreakdown(
                    posBillingWalaDatabase, "invoiceType",
                    isDateMonthWise ? invoiceDate : "");
            OperationalReportCharts.bindListSummary(binding, activity, totalPages, totalAmount,
                    breakdown, getString(R.string.ui_billing_wise_details),
                    getString(R.string.ui_amount_breakdown), period);
            binding.nestedScrollView.setVisibility(View.VISIBLE);
            binding.noDataFound.setVisibility(View.GONE);
            pageNumber = page.size();
        } else {
            binding.nestedScrollView.setVisibility(View.GONE);
            binding.noDataFound.setVisibility(View.VISIBLE);
            pageNumber = 0;
        }
    }

    private class LoadInitialReport extends AsyncTask<Void, Void, List<InvoiceResponse>> {
        private final String dateFilter;
        private int count;
        private float totalAmount;

        LoadInitialReport(String dateFilter) {
            this.dateFilter = dateFilter == null ? "" : dateFilter;
        }

        @Override
        protected List<InvoiceResponse> doInBackground(Void... voids) {
            count = posBillingWalaDatabase.getInvoiceCount(dateFilter);
            totalAmount = posBillingWalaDatabase.getInvoiceTotal(dateFilter);
            if (count <= 0) {
                return new ArrayList<>();
            }
            return posBillingWalaDatabase.getInvoiceList(dateFilter, 0);
        }

        @Override
        protected void onPostExecute(List<InvoiceResponse> page) {
            dismissLoader();
            if (!isAdded()) {
                isLoading = false;
                return;
            }
            totalPages = count;
            bindReportPage(page, totalAmount);
            isLoading = false;
        }
    }

    /** Loads exactly one more page on scroll — never chains all pages. */
    private class getDownloadBills extends AsyncTask<Void, Void, List<InvoiceResponse>> {
        @Override
        protected void onPreExecute() {
            if (isLoading || pageNumber >= totalPages || adapter == null) {
                cancel(true);
                return;
            }
            isLoading = true;
            invoiceResponseList.add(null);
            adapter.notifyItemInserted(invoiceResponseList.size() - 1);
        }

        @Override
        protected List<InvoiceResponse> doInBackground(Void... voids) {
            if (isCancelled()) {
                return null;
            }
            String dateFilter = isDateMonthWise ? invoiceDate : "";
            return posBillingWalaDatabase.getInvoiceList(dateFilter, pageNumber);
        }

        @Override
        protected void onPostExecute(List<InvoiceResponse> page) {
            if (!isAdded()) {
                isLoading = false;
                return;
            }
            removeLoadingFooter();
            if (page != null && !page.isEmpty()) {
                int start = invoiceResponseList.size();
                invoiceResponseList.addAll(page);
                adapter.notifyItemRangeInserted(start, page.size());
                pageNumber += page.size();
            }
            isLoading = false;
        }

        @Override
        protected void onCancelled() {
            isLoading = false;
        }
    }
}