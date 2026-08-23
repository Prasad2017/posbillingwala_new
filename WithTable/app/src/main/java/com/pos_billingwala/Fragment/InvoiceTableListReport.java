package com.pos_billingwala.Fragment;

import static com.pos_billingwala.Utils.RequestCodes.directory_path;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.dewinjm.monthyearpicker.MonthYearPickerDialog;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;
import com.jaredrummler.materialspinner.MaterialSpinner;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.ReportAdapter;
import com.pos_billingwala.BuildConfig;
import com.pos_billingwala.CalenderView.MonthPickerDialog;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.SimpleDividerItemDecoration;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.Utils.ReportToExcel;
import com.pos_billingwala.databinding.FragmentInvoiceTableListReportBinding;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

@SuppressLint("SetTextI18n")
public class InvoiceTableListReport extends Fragment implements View.OnClickListener {

    public static Activity activity;
    static int pageNumber = 0, totalPages, limit = 25;
    public int mYear, mMonth, mDay;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    ReportAdapter adapter;
    List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
    List<CompanyResponse> companyResponseList = new ArrayList<>();
    String noOfTable, invoiceType;
    Calendar calender;
    DatePickerDialog datePickerDialog;
    String invoiceDate = "";
    String[] tableList;
    boolean isLoading = false, isDateMonthWise = false;
    FragmentInvoiceTableListReportBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInvoiceTableListReportBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();


        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        Bundle bundle = getArguments();
        if (bundle != null) {
            noOfTable = bundle.getString("noOfTable");
            invoiceType = bundle.getString("invoiceType");
        }

        binding.heading.setText("Report of " + noOfTable);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    if (invoiceType.equalsIgnoreCase("table_wise")) {
                        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                        ((MainActivity) activity).loadFragment(new InvoiceTableReport(), true);
                    } else {
                        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                        ((MainActivity) activity).loadFragment(new InvoiceTakeAwayReport(), true);
                    }
                    return true;
                }
                return false;
            }
        });

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
            if (invoiceType.equalsIgnoreCase("table_wise")) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new InvoiceTableReport(), true);
            } else {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new InvoiceTakeAwayReport(), true);
            }
        } else if (id == R.id.menuIcon) {
            setPopUpWindow();
        } else if (id == R.id.shareInvoice) {
            if (!invoiceResponseList.isEmpty()) {
                exportSale();
            }
        }
    }

    public void exportSale() {

        ReportToExcel reportToExcel;
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


        reportToExcel = new ReportToExcel("Table Wise Sale Invoices", directory_path);
        reportToExcel.exportReport(reportList, "/Sale/InvoiceSale" + invoiceType + ".xls", new ReportToExcel.ExportListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void onCompleted(String filePath) {
                Toast.makeText(activity, "Sales Report Successfully Exported", Toast.LENGTH_SHORT).show();
                // openGeneratedReport(directory_path + "/Sale/InvoiceSale.xls");
                openGeneratedPDF();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void openGeneratedPDF() {

        File file = new File(directory_path + "/Sale/InvoiceSale" + invoiceType + ".xls");
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
        view = inflater.inflate(R.layout.table_sale_wise_dialog, null);
        PopupWindow mypopupWindow = new PopupWindow(view, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true);

        LinearLayout dayWiseLayout = view.findViewById(R.id.dayWiseLayout);
        LinearLayout monthWiseLayout = view.findViewById(R.id.monthWiseLayout);
        LinearLayout yearWiseLayout = view.findViewById(R.id.yearWiseLayout);
        LinearLayout tableWiseLayout = view.findViewById(R.id.tableWiseLayout);
        if (invoiceType.equalsIgnoreCase("table_wise")) {
            tableWiseLayout.setVisibility(View.VISIBLE);
        } else {
            tableWiseLayout.setVisibility(View.GONE);
        }

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
                                getDateTableReportList(invoiceDate);
                            } else {
                                invoiceDate = year + "-" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getDateTableReportList(invoiceDate);
                            }
                        } else {
                            if ((dayOfMonth) > 9) {
                                invoiceDate = year + "-" + "0" + (monthOfYear + 1) + "-" + dayOfMonth;
                                getDateTableReportList(invoiceDate);
                            } else {
                                invoiceDate = year + "-" + "0" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getDateTableReportList(invoiceDate);
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
                            getDateTableReportList(invoiceDate);
                        } else {
                            invoiceDate = year + "-" + "0" + (monthOfYear + 1);
                            getDateTableReportList(invoiceDate);
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
                        getDateTableReportList(invoiceDate);
                    }
                }, mYear, 0);

                builder.showYearOnly()
                        .setYearRange(1990, 2050)
                        .build()
                        .show();
            }
        });

        tableWiseLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();
                selectTableList();
            }
        });

        mypopupWindow.showAsDropDown(binding.menuIcon, 0, -75);

    }

    public void selectTableList() {

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.table_list_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        TextView dismissTable = dialog.findViewById(R.id.dismissTable);
        TextView selectTable = dialog.findViewById(R.id.selectTable);
        MaterialSpinner tableSpinner = dialog.findViewById(R.id.tableSpinner);

        dismissTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        if (!companyResponseList.isEmpty()) {

            int noOfTable = Integer.parseInt(companyResponseList.get(0).getNoOfTable());
            if (noOfTable > 0) {
                tableList = new String[noOfTable];
                for (int i = 0; i < noOfTable; i++) {
                    tableList[i] = String.valueOf(i + 1);
                }

                try {
                    final ArrayAdapter adapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_item, tableList);
                    adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                    tableSpinner.setAdapter(adapter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        tableSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {
                noOfTable = tableList[position];
                binding.heading.setText("Report of " + noOfTable);
            }
        });

        selectTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                getTableReportList();
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);

    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getCompanyDetails();
        getTableReportList();
    }

    public void getCompanyDetails() {
        companyResponseList = posBillingWalaDatabase.getCompanyDetails();
    }

    public void getTableReportList() {
        if (isLoading) {
            return;
        }
        isLoading = true;
        isDateMonthWise = false;
        pageNumber = 0;
        new LoadInitialTableList("").execute();
    }

    public void getDateTableReportList(String selectedDate) {
        if (isLoading) {
            return;
        }
        isLoading = true;
        isDateMonthWise = true;
        invoiceDate = selectedDate;
        pageNumber = 0;
        new LoadInitialTableList(selectedDate).execute();
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

    private void bindTableListPage(List<InvoiceResponse> page, float totalAmount) {
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

    private class LoadInitialTableList extends AsyncTask<Void, Void, List<InvoiceResponse>> {
        private final String dateFilter;
        private int count;
        private float totalAmount;

        LoadInitialTableList(String dateFilter) {
            this.dateFilter = dateFilter == null ? "" : dateFilter;
        }

        @Override
        protected List<InvoiceResponse> doInBackground(Void... voids) {
            count = posBillingWalaDatabase.getTableReportInvoiceCount(dateFilter, noOfTable, invoiceType);
            totalAmount = posBillingWalaDatabase.getTableReportInvoiceTotal(dateFilter, noOfTable, invoiceType);
            if (count <= 0) {
                return new ArrayList<>();
            }
            return posBillingWalaDatabase.getTableReportList(dateFilter, noOfTable, invoiceType, 0);
        }

        @Override
        protected void onPostExecute(List<InvoiceResponse> page) {
            if (!isAdded()) {
                isLoading = false;
                return;
            }
            totalPages = count;
            bindTableListPage(page, totalAmount);
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
            return posBillingWalaDatabase.getTableReportList(dateFilter, noOfTable, invoiceType, pageNumber);
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