package com.pos_billingwala.Retrofit;


import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiInterface {

    @FormUrlEncoded
    @POST("registerTrial.php")
    Call<AllApiResponse> registerTrial(@Field("name") String name,
                                       @Field("contact_number") String contactNumber,
                                       @Field("address") String address,
                                       @Field("shopName") String shopName);

    @FormUrlEncoded
    @POST("Login.php")
    Call<LoginResponse> loginCheck(@Field("app_licence_key") String appLicenceKey,
                                   @Field("android_device_id") String androidDeviceId);

    @FormUrlEncoded
    @POST("LoginMpin.php")
    Call<LoginResponse> loginMpin(@Field("mpin") String enteredMpin,
                                  @Field("app_licence_key") String appLicenceKey,
                                  @Field("androidId") String androidId,
                                  @Field("android_device_name") String androidDeviceName);

    @FormUrlEncoded
    @POST("updateMPin.php")
    Call<LoginResponse> updateMpin(@Field("mpin") String enteredMpin,
                                   @Field("app_licence_key") String app_licence_key,
                                   @Field("androidId") String androidId,
                                   @Field("android_device_name") String androidDeviceName);

    @FormUrlEncoded
    @POST("updateAndroidKey.php")
    Call<LoginResponse> updateLicenceKey(@Field("app_licence_key") String app_licence_key,
                                         @Field("androidId") String androidId,
                                         @Field("android_device_name") String androidDeviceName);

    @FormUrlEncoded
    @POST("check_licence_expire.php")
    Call<LoginResponse> checkLicenceExpire(@Field("userId") String userId,
                                           @Field("android_device_id") String androidId,
                                           @Field("android_device_name") String android_device_name);

    @GET("getFoodTypeList.php")
    Call<AllApiResponse> getFoodTypeList();

    @GET("getSubcategoryList.php")
    Call<AllApiResponse> getSubcategoryList(@Query("userId") String userId);

    @GET("getPortionList.php")
    Call<AllApiResponse> getPortionList(@Query("userId") String userId);

    @GET("getCategoryList.php")
    Call<AllApiResponse> getCategoryList(@Query("userId") String userId);

    @GET("getProductList.php")
    Call<AllApiResponse> getProductList(@Query("userId") String userId);

    @GET("getCompanyList.php")
    Call<AllApiResponse> getCompanyList(@Query("userId") String userId);

    @GET("getCompanyPrinterSetting.php")
    Call<AllApiResponse> getCompanyPrinterSetting(@Query("userId") String userId);

    @GET("getInvoiceList.php")
    Call<AllApiResponse> getInvoiceList(@Query("userId") String userId);

    @GET("getInvoiceList.php")
    Call<AllApiResponse> getInvoiceListByDate(@Query("userId") String userId,
                                              @Query("invoiceDate") String invoiceDate);

    @GET("getInvoiceProductList.php")
    Call<AllApiResponse> getInvoiceProductList(@Query("userId") String userId);

    @GET("getInvoiceProductList.php")
    Call<AllApiResponse> getInvoiceProductListByDate(@Query("userId") String userId,
                                                     @Query("invoiceDate") String invoiceDate);

    @GET("getInventoryList.php")
    Call<AllApiResponse> getInventoryList(@Query("userId") String userId);

    @GET("getExpensesList.php")
    Call<AllApiResponse> getExpensesList(@Query("userId") String userId);

    @FormUrlEncoded
    @POST("insertCategory.php")
    Call<AllApiResponse> saveCategory(@Field("userId") String userId,
                                      @Field("categoryName") String categoryName,
                                      @Field("categoryDeletedStatus") String categoryDeletedStatus,
                                      @Field("categoryNetworkStatus") String categoryNetworkStatus,
                                      @Field("foodTypeCode") String foodTypeCode);

    @FormUrlEncoded
    @POST("insertSubcategory.php")
    Call<AllApiResponse> saveSubcategory(@Field("userId") String userId,
                                         @Field("categoryId") String categoryId,
                                         @Field("categoryNetworkStatus") String categoryNetworkStatus,
                                         @Field("subcategoryName") String subcategoryName,
                                         @Field("subcategoryDeletedStatus") String subcategoryDeletedStatus,
                                         @Field("subcategoryNetworkStatus") String subcategoryNetworkStatus);

    @FormUrlEncoded
    @POST("insertProduct.php")
    Call<AllApiResponse> saveProduct(@Field("userId") String userId,
                                     @Field("categoryId") String categoryId,
                                     @Field("categoryName") String categoryName,
                                     @Field("productCode") String productCode,
                                     @Field("productName") String productName,
                                     @Field("productPrice") String productPrice,
                                     @Field("productUnit") String productUnit,
                                     @Field("productCGST") String productCGST,
                                     @Field("productSGST") String productSGST,
                                     @Field("productNetworkStatus") String productNetworkStatus,
                                     @Field("productDeletedStatus") String productDeletedStatus,
                                     @Field("subcategoryId") String subcategoryId);

    @FormUrlEncoded
    @POST("insertPortion.php")
    Call<AllApiResponse> savePortion(@Field("userId") String userId,
                                     @Field("productId") String productId,
                                     @Field("productNetworkStatus") String productNetworkStatus,
                                     @Field("portionName") String portionName,
                                     @Field("portionPrice") String portionPrice,
                                     @Field("portionSortOrder") String portionSortOrder,
                                     @Field("portionDeletedStatus") String portionDeletedStatus,
                                     @Field("portionNetworkStatus") String portionNetworkStatus);

    @FormUrlEncoded
    @POST("insertCompanyPrinterSetting.php")
    Call<AllApiResponse> savePrinterSetting(@Field("userId") String userId,
                                            @Field("printerName") String printerName,
                                            @Field("KOTPrinterName") String KOTPrinterName,
                                            @Field("invoicePrefix") String invoicePrefix,
                                            @Field("invoiceTitle") String invoiceTitle,
                                            @Field("invoiceTermsCondition") String invoiceTermsCondition,
                                            @Field("logoUse") String logoUse,
                                            @Field("paymentUse") String paymentUse,
                                            @Field("customerUse") String customerUse,
                                            @Field("productQuantityUpdate") String productQuantityUpdate,
                                            @Field("bluetoothAddress") String bluetoothAddress,
                                            @Field("bluetoothKOTAddress") String bluetoothKOTAddress,
                                            @Field("printerFeedLines") String printerFeedLines,
                                            @Field("KotPrinterFeedLines") String KotPrinterFeedLines);

    @FormUrlEncoded
    @POST("insertCompanyDetail.php")
    Call<AllApiResponse> saveCompanyDetails(@Field("userId") String userId,
                                            @Field("companyLogo") String companyLogo,
                                            @Field("companyName") String companyName,
                                            @Field("cashierName") String cashierName,
                                            @Field("companyMobile") String companyMobile,
                                            @Field("companyAddress") String companyAddress,
                                            @Field("currencyName") String currencyName,
                                            @Field("tableStatus") String tableStatus,
                                            @Field("noOfTable") String noOfTable,
                                            @Field("countryName") String countryName,
                                            @Field("stateName") String stateName,
                                            @Field("gstStatus") String gstStatus,
                                            @Field("gstNumber") String gstNumber,
                                            @Field("panNumber") String panNumber,
                                            @Field("paymentLogo") String paymentLogo,
                                            @Field("companyFssis") String companyFssis);

    @FormUrlEncoded
    @POST("insertInvoice.php")
    Call<AllApiResponse> saveInvoice(@Field("userId") String userId,
                                     @Field("noOfTable") String noOfTable,
                                     @Field("invoiceNumber") String invoiceNumber,
                                     @Field("customerName") String customerName,
                                     @Field("customerMobile") String customerMobile,
                                     @Field("customerEmail") String customerEmail,
                                     @Field("customerAddress") String customerAddress,
                                     @Field("subTotal") String subTotal,
                                     @Field("totalGSTAmount") String totalGSTAmount,
                                     @Field("discount") String discount,
                                     @Field("discountType") String discountType,
                                     @Field("totalAmount") String totalAmount,
                                     @Field("paymentMode") String paymentMode,
                                     @Field("invoiceDate") String invoiceDate,
                                     @Field("invoiceType") String invoiceType,
                                     @Field("invoiceOrderStatus") String invoiceOrderStatus,
                                     @Field("invoiceNetworkStatus") String invoiceNetworkStatus);

    @FormUrlEncoded
    @POST("insertInvoiceProduct.php")
    Call<AllApiResponse> saveInvoiceProduct(@Field("invoiceNumber") String invoiceNumber,
                                            @Field("productName") String productName,
                                            @Field("productPrice") String productPrice,
                                            @Field("productUnit") String productUnit,
                                            @Field("productCGST") String productCGST,
                                            @Field("productSGST") String productSGST,
                                            @Field("productQuantity") String productQuantity,
                                            @Field("productStatus") String productStatus,
                                            @Field("invoiceProductNetworkStatus") String invoiceProductNetworkStatus,
                                            @Field("portionId") String portionId,
                                            @Field("portionName") String portionName,
                                            @Field("snapshotProductName") String snapshotProductName,
                                            @Field("snapshotLinePrice") String snapshotLinePrice);

    @FormUrlEncoded
    @POST("insertInventory.php")
    Call<AllApiResponse> saveInventory(@Field("userId") String userId,
                                       @Field("productId") String productId,
                                       @Field("productInventoryQuantity") String productInventoryQuantity,
                                       @Field("afterSaleInventoryQuantity") String afterSaleInventoryQuantity,
                                       @Field("saleInventoryQuantity") String saleInventoryQuantity,
                                       @Field("inventoryDate") String inventoryDate,
                                       @Field("inventoryNetworkStatus") String inventoryNetworkStatus);

    @FormUrlEncoded
    @POST("insertExpenses.php")
    Call<AllApiResponse> saveExpenses(@Field("userId") String userId,
                                      @Field("expensesName") String expensesName,
                                      @Field("expensesAmount") String expensesAmount,
                                      @Field("expensesDate") String expensesDate,
                                      @Field("expensesNetworkStatus") String expensesNetworkStatus);

    @GET("LogOut.php")
    Call<AllApiResponse> serverLogout(@Query("licenceKey") String licenceKey);


    @GET("getMessMemberList.php")
    Call<AllApiResponse> getMessMemberList(@Query("userId") String userId);

    @GET("getMessMemberPaymentList.php")
    Call<AllApiResponse> getMessMemberPaymentList(@Query("userId") String userId);

    @GET("getMessInvoiceList.php")
    Call<AllApiResponse> getMessInvoiceList(@Query("userId") String userId);

    @FormUrlEncoded
    @POST("insertMessMember.php")
    Call<AllApiResponse> saveMessMember(@Field("userId") String userId,
                                        @Field("memberName") String memberName,
                                        @Field("memberMobileNumber") String memberMobileNumber,
                                        @Field("memberAltenetMobileNumber") String memberAlternetMobileNumber,
                                        @Field("memberAddress") String memberAddress,
                                        @Field("memberNetworkStatus") String memberNetworkStatus,
                                        @Field("memberStatus") String memberStatus);

    @FormUrlEncoded
    @POST("insertMessPayment.php")
    Call<AllApiResponse> saveMessMemberPayment(@Field("userId") String userId,
                                               @Field("memberId") String memberId,
                                               @Field("memberName") String memberName,
                                               @Field("paymentMessAmount") String paymentMessAmount,
                                               @Field("paymentPaidAmount") String paymentPaidAmount,
                                               @Field("messTotalDays") String messTotalDays,
                                               @Field("paymentDate") String paymentDate,
                                               @Field("paymentNetworkStatus") String paymentNetworkStatus,
                                               @Field("paymentStatus") String paymentStatus);

    @FormUrlEncoded
    @POST("insertMessInvoice.php")
    Call<AllApiResponse> saveMessInvoice(@Field("userId") String userId,
                                         @Field("memberName") String memberName,
                                         @Field("messType") String messType,
                                         @Field("messInvoiceDate") String messInvoiceDate,
                                         @Field("messInvoiceNetworkStatus") String messInvoiceNetworkStatus,
                                         @Field("messInvoiceStatus") String messInvoiceStatus);


}
