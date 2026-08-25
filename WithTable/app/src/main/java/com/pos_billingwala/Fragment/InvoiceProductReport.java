package com.pos_billingwala.Fragment;

import static com.pos_billingwala.Utils.RequestCodes.directory_path;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.github.dewinjm.monthyearpicker.MonthYearPickerDialog;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.ReportProductAdapter;
import com.pos_billingwala.BuildConfig;
import com.pos_billingwala.CalenderView.MonthPickerDialog;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.CartItemType;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Extra.SimpleDividerItemDecoration;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentInvoiceProductReportBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;

@SuppressLint("SetTextI18n")
public class InvoiceProductReport extends Fragment implements View.OnClickListener {

    public static final String ARG_INVOICE_ITEM_TYPE = "invoiceItemType";
    public static Activity activity;
    public static NestedScrollView nestedScrollView;
    public static FragmentInvoiceProductReportBinding binding;
    public int mYear, mMonth, mDay;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    List<InvoiceProductResponse> invoiceProductResponseList = new ArrayList<>();
    ReportProductAdapter adapter;
    Calendar calender;
    DatePickerDialog datePickerDialog;
    String invoiceDate = "";
    String invoiceItemTypeFilter = null;
    int PERMISSION_ALL = 1;

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
        binding = FragmentInvoiceProductReportBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        if (getArguments() != null) {
            invoiceItemTypeFilter = getArguments().getString(ARG_INVOICE_ITEM_TYPE);
        }

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

        initViews();

        //Add runtime permissions
        String[] PERMISSIONS = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        if (!hasPermissions(activity, PERMISSIONS)) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSION_ALL);
        }

        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        invoiceDate = df.format(c);

        return view;
    }

    public void initViews() {
        binding.menuIcon.setOnClickListener(this);
        binding.backToSetting.setOnClickListener(this);
        binding.shareInvoice.setOnClickListener(this);
        if (CartItemType.isCombo(invoiceItemTypeFilter) && binding.reportTitle != null) {
            binding.reportTitle.setText(R.string.ui_combo_wise_report);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.menuIcon) {
            setPopUpWindow();
        } else if (id == R.id.backToSetting) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.shareInvoice) {
            if (!invoiceProductResponseList.isEmpty()) {
                createPdf();
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

    public void createPdf() {

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Bitmap bitmap = convertLayout(nestedScrollView);

        if (bitmap != null) {

            try {
                File file = new File(directory_path + "/Sale/ProductSale[" + invoiceDate + "].png");
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            openGeneratedPDF();

        }

    }

    public void openGeneratedPDF() {

        File file = new File(directory_path + "/Sale/ProductSale[" + invoiceDate + "].png");
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
        intentShareFile.setType(URLConnection.guessContentTypeFromName(file.getName()));
        intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);
        List<ResolveInfo> resInfoList = activity.getPackageManager().queryIntentActivities(intentShareFile, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            activity.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        startActivity(Intent.createChooser(intentShareFile, "Share Product Sale"));

    }

    public void setPopUpWindow() {

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.product_wise_dialog, null);
        PopupWindow mypopupWindow = new PopupWindow(view, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true);

        LinearLayout topSaleProductLayout = view.findViewById(R.id.topSaleProductLayout);
        LinearLayout lessSaleProductLayout = view.findViewById(R.id.lessSaleProductLayout);
        LinearLayout dayWiseTopSaleProductLayout = view.findViewById(R.id.dayWiseTopSaleProductLayout);
        LinearLayout monthWiseTopSaleProductLayout = view.findViewById(R.id.monthWiseTopSaleProductLayout);
        LinearLayout yearWiseTopSaleProductLayout = view.findViewById(R.id.yearWiseTopSaleProductLayout);

        LinearLayout dayWiseLessSaleProductLayout = view.findViewById(R.id.dayWiseLessSaleProductLayout);
        LinearLayout monthWiseLessSaleProductLayout = view.findViewById(R.id.monthWiseLessSaleProductLayout);
        LinearLayout yearWiseLessSaleProductLayout = view.findViewById(R.id.yearWiseLessSaleProductLayout);

        //Get Current Date
        calender = Calendar.getInstance();
        mYear = calender.get(Calendar.YEAR);
        mMonth = calender.get(Calendar.MONTH);
        mDay = calender.get(Calendar.DAY_OF_MONTH);

        dayWiseTopSaleProductLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();

                datePickerDialog = new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        if ((monthOfYear) > 9) {
                            if ((dayOfMonth) > 9) {
                                invoiceDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                                getReportDateWiseProductList(invoiceDate, "DESC");
                            } else {
                                invoiceDate = year + "-" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getReportDateWiseProductList(invoiceDate, "DESC");
                            }
                        } else {
                            if ((dayOfMonth) > 9) {
                                invoiceDate = year + "-" + "0" + (monthOfYear + 1) + "-" + dayOfMonth;
                                getReportDateWiseProductList(invoiceDate, "DESC");
                            } else {
                                invoiceDate = year + "-" + "0" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getReportDateWiseProductList(invoiceDate, "DESC");
                            }
                        }
                    }
                }, mYear, mMonth, mDay);

                datePickerDialog.show();
            }
        });

        monthWiseTopSaleProductLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();

                MonthYearPickerDialogFragment dialogFragment = MonthYearPickerDialogFragment.getInstance(mMonth, mYear, "Select Month");
                dialogFragment.show(getChildFragmentManager(), null);
                dialogFragment.setOnDateSetListener(new MonthYearPickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(int year, int monthOfYear) {
                        if ((monthOfYear) > 9) {
                            invoiceDate = year + "-" + (monthOfYear + 1);
                            getReportDateWiseProductList(invoiceDate, "DESC");
                        } else {
                            invoiceDate = year + "-" + "0" + (monthOfYear + 1);
                            getReportDateWiseProductList(invoiceDate, "DESC");
                        }
                    }
                });

            }
        });

        yearWiseTopSaleProductLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();
                MonthPickerDialog.Builder builder = new MonthPickerDialog.Builder(activity, new MonthPickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(int selectedMonth, int selectedYear) {
                        invoiceDate = "" + selectedYear;
                        mYear = selectedYear;
                        getReportDateWiseProductList(invoiceDate, "DESC");
                    }
                }, mYear, 0);

                builder.showYearOnly()
                        .setYearRange(1990, 2050)
                        .build()
                        .show();
            }
        });

        dayWiseLessSaleProductLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();

                datePickerDialog = new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        if ((monthOfYear) > 9) {
                            if ((dayOfMonth) > 9) {
                                invoiceDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                                getReportDateWiseProductList(invoiceDate, "ASC");
                            } else {
                                invoiceDate = year + "-" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getReportDateWiseProductList(invoiceDate, "ASC");
                            }
                        } else {
                            if ((dayOfMonth) > 9) {
                                invoiceDate = year + "-" + "0" + (monthOfYear + 1) + "-" + dayOfMonth;
                                getReportDateWiseProductList(invoiceDate, "ASC");
                            } else {
                                invoiceDate = year + "-" + "0" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getReportDateWiseProductList(invoiceDate, "ASC");
                            }
                        }
                    }
                }, mYear, mMonth, mDay);

                datePickerDialog.show();
            }
        });

        monthWiseLessSaleProductLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();

                MonthYearPickerDialogFragment dialogFragment = MonthYearPickerDialogFragment.getInstance(mMonth, mYear, "Select Month");
                dialogFragment.show(getChildFragmentManager(), null);
                dialogFragment.setOnDateSetListener(new MonthYearPickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(int year, int monthOfYear) {
                        if ((monthOfYear) > 9) {
                            invoiceDate = year + "-" + (monthOfYear + 1);
                            getReportDateWiseProductList(invoiceDate, "ASC");
                        } else {
                            invoiceDate = year + "-" + "0" + (monthOfYear + 1);
                            getReportDateWiseProductList(invoiceDate, "ASC");
                        }
                    }
                });

            }
        });

        yearWiseLessSaleProductLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();
                MonthPickerDialog.Builder builder = new MonthPickerDialog.Builder(activity, new MonthPickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(int selectedMonth, int selectedYear) {
                        invoiceDate = "" + selectedYear;
                        mYear = selectedYear;
                        getReportDateWiseProductList(invoiceDate, "ASC");
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
        getReportDateWiseProductList(invoiceDate, "DESC");
    }


    public void getReportDateWiseProductList(String invoiceDate, String orderBy) {
        SweetAlertDialog loader = ListLoader.show(activity);
        try {
            if (CartItemType.isCombo(invoiceItemTypeFilter)) {
                binding.invoiceDate.setText(getString(R.string.ui_combo_wise_report) + " [ " + invoiceDate + "]");
            } else {
                binding.invoiceDate.setText("Product Sale [ " + invoiceDate + "]");
            }

            invoiceProductResponseList.clear();
            invoiceProductResponseList = posBillingWalaDatabase.getReportDateWiseProductList(
                    invoiceDate, orderBy, invoiceItemTypeFilter);
            if (!invoiceProductResponseList.isEmpty()) {
                adapter = new ReportProductAdapter(activity, invoiceProductResponseList);
                binding.recyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
                binding.recyclerView.addItemDecoration(new SimpleDividerItemDecoration(activity));
                binding.recyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                //   adapter.notifyItemInserted(invoiceProductResponseList.size() - 1);

                binding.noDataFound.setVisibility(View.GONE);
                binding.nestedScrollView.setVisibility(View.VISIBLE);

            } else {
                binding.noDataFound.setVisibility(View.VISIBLE);
                binding.nestedScrollView.setVisibility(View.GONE);
            }
        } finally {
            ListLoader.dismiss(loader);
        }
    }

}