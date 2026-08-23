package com.pos_billingwala.Fragment;

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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import com.pos_billingwala.Extra.SimpleDividerItemDecoration;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.Utils.ReportToExcel;
import com.pos_billingwala.databinding.FragmentInvoicePaymentModeWiseReportBinding;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;

@SuppressLint("NonConstantResourceId, StaticFieldLeak, SetTextI18n")
public class InvoicePaymentModeWiseReport extends Fragment implements View.OnClickListener {

    public static Activity activity;
    static int pageNumber = 0, totalPages, limit = 25;
    public int mYear, mMonth, mDay;
    public String paymentMode = "Cash";
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
    FragmentInvoicePaymentModeWiseReportBinding binding;


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
        binding = FragmentInvoicePaymentModeWiseReportBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);
        binding.heading.setText(paymentMode + " Invoice Reports");

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new ReportSetting(), true);
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
                if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight()) {
                    if (!isLoading && pageNumber < totalPages) {
                        new getDownloadBills().execute();
                    }
                }
            }
        });

        binding.backToSetting.setOnClickListener(this);
        binding.menuIcon.setOnClickListener(this);
        binding.shareInvoice.setOnClickListener(this);

        return view;

    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new ReportSetting(), true);
        } else if (id == R.id.menuIcon) {
            setPopUpWindow();
        } else if (id == R.id.shareInvoice) {
            if (!invoiceResponseList.isEmpty()) {
                exportSale();
            }
        }
    }

    public Bitmap convertLayout(NestedScrollView nestedScrollView) {

        nestedScrollView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        nestedScrollView.layout(0, 0, nestedScrollView.getMeasuredWidth(), nestedScrollView.getMeasuredHeight());

        nestedScrollView.setDrawingCacheEnabled(true);
        nestedScrollView.buildDrawingCache();

        Bitmap bitmap = Bitmap.createBitmap(nestedScrollView.getWidth(),
                nestedScrollView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable background = nestedScrollView.getBackground();
        if (background != null) {
            background.draw(canvas);
        } else {
            canvas.drawColor(Color.WHITE);
        }
        nestedScrollView.draw(canvas);
        nestedScrollView.buildDrawingCache();

        return bitmap;

    }

    public void exportSale() {

        ReportToExcel reportToExcel;
        List<List<String>> reportList = new ArrayList<>();

        List<String> col_name = new ArrayList<>();
        col_name.add("SR No");
        col_name.add("Invoice Date");
        col_name.add("Invoice Number");
        col_name.add("Payment Mode");
        col_name.add("Invoice Amount ( " + MainActivity.currencyName + " )");
        reportList.add(col_name);

        for (int i = 0; i < invoiceResponseList.size(); i++) {
            List<String> columnList = new ArrayList<>();
            InvoiceResponse invoiceResponse = invoiceResponseList.get(i);
            if (invoiceResponse != null) {
                columnList.add(String.valueOf(i + 1));
                columnList.add(invoiceResponse.getInvoiceDate());
                columnList.add(invoiceResponse.getInvoiceNumber());
                columnList.add(paymentMode);
                columnList.add(invoiceResponse.getTotalAmount());
                reportList.add(columnList);
            }
        }

        List<String> columnList = new ArrayList<>();
        columnList.add("");
        columnList.add("");
        columnList.add("");
        columnList.add("Total Amount");
        columnList.add(binding.totalAmount.getText().toString());
        reportList.add(columnList);

        reportToExcel = new ReportToExcel("Sale Invoices", directory_path);
        reportToExcel.exportReport(reportList, "/Sale/InvoicePaymentSale" + paymentMode + ".xls", new ReportToExcel.ExportListener() {
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
        File file = new File(directory_path + "/Sale/InvoicePaymentSale" + paymentMode + ".xls");
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

    public void openGeneratedPDF() {

        File file = new File(directory_path + "/Sale/InvoicePaymentSale" + paymentMode + ".xls");
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
        intentShareFile.setType(URLConnection.guessContentTypeFromName(file.getName()));
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
        view = inflater.inflate(R.layout.payment_mode_sale_wise_dialog, null);
        PopupWindow mypopupWindow = new PopupWindow(view, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true);

        LinearLayout cashModeWiseLayout = view.findViewById(R.id.cashModeWiseLayout);
        LinearLayout onlineModeWiseLayout = view.findViewById(R.id.onlineModeWiseLayout);
        LinearLayout bankModeWiseLayout = view.findViewById(R.id.bankModeWiseLayout);

        LinearLayout dayWiseLayout = view.findViewById(R.id.dayWiseLayout);
        LinearLayout monthWiseLayout = view.findViewById(R.id.monthWiseLayout);
        LinearLayout yearWiseLayout = view.findViewById(R.id.yearWiseLayout);

        //Get Current Date
        calender = Calendar.getInstance();
        mYear = calender.get(Calendar.YEAR);
        mMonth = calender.get(Calendar.MONTH);
        mDay = calender.get(Calendar.DAY_OF_MONTH);

        cashModeWiseLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();
                paymentMode = "Cash";
                binding.heading.setText(paymentMode + " Invoice Reports");
                getReportList(paymentMode);
            }
        });

        onlineModeWiseLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();
                paymentMode = "UPI";
                binding.heading.setText(paymentMode + " Invoice Reports");
                getReportList(paymentMode);
            }
        });

        bankModeWiseLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();
                paymentMode = "Bank";
                binding.heading.setText(paymentMode + " Invoice Reports");
                getReportList(paymentMode);
            }
        });
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

        mypopupWindow.showAsDropDown(binding.menuIcon, 0, -75);

    }


    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getReportList(paymentMode);
    }

    public void getReportList(String paymentMode) {
        if (isLoading) {
            return;
        }
        isLoading = true;
        isDateMonthWise = false;
        invoiceDate = "";
        pageNumber = 0;
        this.paymentMode = paymentMode;

        pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        new LoadInitialPaymentReport("").execute();
    }

    public void getDateReportList(String selectedDate) {
        if (isLoading) {
            return;
        }
        isLoading = true;
        isDateMonthWise = true;
        invoiceDate = selectedDate;
        pageNumber = 0;

        pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        new LoadInitialPaymentReport(selectedDate).execute();
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

    private void bindPaymentReportPage(List<InvoiceResponse> page, float totalAmount) {
        invoiceResponseList = new ArrayList<>();
        if (page != null && !page.isEmpty()) {
            invoiceResponseList.addAll(page);
            adapter = new ReportAdapter(activity, invoiceResponseList);
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
            binding.recyclerView.addItemDecoration(new SimpleDividerItemDecoration(activity));
            binding.recyclerView.setAdapter(adapter);
            binding.totalAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalAmount));
            binding.nestedScrollView.setVisibility(View.VISIBLE);
            binding.noDataFound.setVisibility(View.GONE);
            pageNumber = page.size();
        } else {
            binding.nestedScrollView.setVisibility(View.GONE);
            binding.noDataFound.setVisibility(View.VISIBLE);
            pageNumber = 0;
        }
    }

    private class LoadInitialPaymentReport extends AsyncTask<Void, Void, List<InvoiceResponse>> {
        private final String dateFilter;
        private int count;
        private float totalAmount;

        LoadInitialPaymentReport(String dateFilter) {
            this.dateFilter = dateFilter == null ? "" : dateFilter;
        }

        @Override
        protected List<InvoiceResponse> doInBackground(Void... voids) {
            count = posBillingWalaDatabase.getInvoicePaymentModeCount(dateFilter, paymentMode);
            totalAmount = posBillingWalaDatabase.getInvoicePaymentModeTotal(dateFilter, paymentMode);
            if (count <= 0) {
                return new ArrayList<>();
            }
            return posBillingWalaDatabase.getInvoicePaymentModeList(dateFilter, 0, paymentMode);
        }

        @Override
        protected void onPostExecute(List<InvoiceResponse> page) {
            if (!isAdded()) {
                isLoading = false;
                return;
            }
            if (pDialog != null && pDialog.isShowing()) {
                pDialog.dismiss();
            }
            totalPages = count;
            bindPaymentReportPage(page, totalAmount);
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
            return posBillingWalaDatabase.getInvoicePaymentModeList(dateFilter, pageNumber, paymentMode);
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