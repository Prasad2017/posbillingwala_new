package com.posbillingwala.admin.Retrofit;


import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.CatalogImportHistoryResponse;
import com.posbillingwala.admin.Model.CatalogImportPreviewResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiInterface {

    @FormUrlEncoded
    @POST("Login.php")
    Call<AllApiResponse> loginAdmin(@Field("userEmail") String userEmail,
                                    @Field("password") String password);


    @GET("getCustomerList.php")
    Call<AllApiResponse> getCustomerList();

    @GET("getCustomerList.php")
    Call<AllApiResponse> getCustomerList(@Query("limit") int limit);

    @GET("getCustomerDetails.php")
    Call<AllApiResponse> getCustomerDetails(@Query("customerId") String customerId);


    @GET("getCustomerCount.php")
    Call<AllApiResponse> getCustomerCount();

    @FormUrlEncoded
    @POST("insertCustomer.php")
    Call<AllApiResponse> customerRegistration(@Field("name") String name,
                                              @Field("contact_number") String contact_number,
                                              @Field("address") String address,
                                              @Field("shopName") String shopName,
                                              @Field("licenseValidity") String licenseValidity,
                                              @Field("licenseType") String licenseType,
                                              @Field("amount") String amount,
                                              @Field("fastBilling") String fastBilling,
                                              @Field("takeAway") String takeAway,
                                              @Field("dineIn") String dineIn,
                                              @Field("mess") String mess);


    @FormUrlEncoded
    @POST("insertExportAllProduct.php")
    Call<AllApiResponse> insertExportProduct(@Field("customerId") String customerId,
                                             @Field("categoryName") String categoryName,
                                             @Field("productName") String productName,
                                             @Field("productUnit") String productUnit,
                                             @Field("productPrice") String productPrice,
                                             @Field("productCGST") String productCGST,
                                             @Field("productSGST") String productSGST);

    @FormUrlEncoded
    @POST("updateCustomerLicenceDetails.php")
    Call<AllApiResponse> updateCustomerLicenceDetails(@Field("licensesId") String licensesId,
                                                      @Field("licenseValidity") String licenseValidity,
                                                      @Field("licenseType") String licenseType,
                                                      @Field("amount") String amount,
                                                      @Field("registrationDate") String registrationDate);

    @FormUrlEncoded
    @POST("updateLicenseStatus.php")
    Call<AllApiResponse> updateLicenseStatus(@Field("licensesId") String licensesId,
                                             @Field("action") String action);


    @FormUrlEncoded
    @POST("updateCustomerDetails.php")
    Call<AllApiResponse> updateCustomerDetails(@Field("customerId") String customerId,
                                               @Field("customerName") String customerName,
                                               @Field("customerMobileNumber") String customerMobileNumber,
                                               @Field("customerAddress") String customerAddress,
                                               @Field("customerShopName") String customerShopName);


    @FormUrlEncoded
    @POST("insertCustomerCategory.php")
    Call<AllApiResponse> saveCategory(@Field("userId") String userId,
                                      @Field("categoryName") String categoryName,
                                      @Field("categoryNetworkStatus") String categoryNetworkStatus,
                                      @Field("foodTypeId") String foodTypeId);

    @FormUrlEncoded
    @POST("insertCustomerProduct.php")
    Call<AllApiResponse> saveProduct(@Field("userId") String userId,
                                     @Field("categoryId") String categoryId,
                                     @Field("categoryName") String categoryName,
                                     @Field("productName") String productName,
                                     @Field("productPrice") String productPrice,
                                     @Field("productUnit") String productUnit,
                                     @Field("productCGST") String productCGST,
                                     @Field("productSGST") String productSGST,
                                     @Field("productNetworkStatus") String productNetworkStatus,
                                     @Field("subcategoryId") String subcategoryId);

    @GET("getCustomerCategoryList.php")
    Call<AllApiResponse> getCategoryList(@Query("userId") String userId);

    @GET("getCustomerProductList.php")
    Call<AllApiResponse> getProductList(@Query("userId") String userId);

    @GET("getFoodTypeList.php")
    Call<AllApiResponse> getFoodTypeList();

    @GET("getSubcategoryList.php")
    Call<AllApiResponse> getSubcategoryList(@Query("userId") String userId,
                                            @Query("categoryId") String categoryId);

    @GET("getPortionList.php")
    Call<AllApiResponse> getPortionList(@Query("userId") String userId,
                                        @Query("productId") String productId);

    @GET("getPortionMasterList.php")
    Call<AllApiResponse> getPortionMasterList(@Query("userId") String userId);

    @FormUrlEncoded
    @POST("insertCustomerSubcategory.php")
    Call<AllApiResponse> saveSubcategory(@Field("userId") String userId,
                                         @Field("categoryId") String categoryId,
                                         @Field("subcategoryName") String subcategoryName,
                                         @Field("subcategoryNetworkStatus") String subcategoryNetworkStatus);

    @FormUrlEncoded
    @POST("insertCustomerPortionMaster.php")
    Call<AllApiResponse> savePortionMaster(@Field("userId") String userId,
                                           @Field("portionName") String portionName,
                                           @Field("portionMasterNetworkStatus") String portionMasterNetworkStatus,
                                           @Field("portionMasterDeletedStatus") String portionMasterDeletedStatus);

    @FormUrlEncoded
    @POST("insertCustomerPortion.php")
    Call<AllApiResponse> savePortion(@Field("userId") String userId,
                                     @Field("productId") String productId,
                                     @Field("portionName") String portionName,
                                     @Field("portionPrice") String portionPrice,
                                     @Field("portionSortOrder") String portionSortOrder,
                                     @Field("portionNetworkStatus") String portionNetworkStatus,
                                     @Field("portionMasterId") String portionMasterId);

    @FormUrlEncoded
    @POST("deleteProduct.php")
    Call<AllApiResponse> deleteProduct(@Field("productId") String productId);

    @FormUrlEncoded
    @POST("updateProduct.php")
    Call<AllApiResponse> updateProduct(@Field("productId") String productId,
                                       @Field("categoryId") String categoryId,
                                       @Field("categoryName") String categoryName,
                                       @Field("productName") String productName,
                                       @Field("productPrice") String productPrice,
                                       @Field("productUnit") String productUnit,
                                       @Field("productCGST") String productCGST,
                                       @Field("productSGST") String productSGST,
                                       @Field("subcategoryId") String subcategoryId);


    @FormUrlEncoded
    @POST("deleteCategory.php")
    Call<AllApiResponse> deleteCategory(@Field("categoryId") String categoryId);


    @FormUrlEncoded
    @POST("updateCategory.php")
    Call<AllApiResponse> updateCategory(@Field("categoryId") String categoryId,
                                        @Field("categoryName") String categoryName);


    @GET("getProductDetails.php")
    Call<AllApiResponse> getProductDetails(@Query("productId") String productId);

    @GET("getDealerList.php")
    Call<AllApiResponse> getDealerList();

    @FormUrlEncoded
    @POST("updateDealerProfile.php")
    Call<AllApiResponse> updateDealerProfile(@Field("userId") String userId,
                                             @Field("dealerName") String dealerName,
                                             @Field("dealerMobileNumber") String dealerMobileNumber,
                                             @Field("dealerEmail") String dealerEmail,
                                             @Field("dealerAddress") String dealerAddress,
                                             @Field("dealerAadhaarNumber") String dealerAadhaarNumber);

    @FormUrlEncoded
    @POST("insertNewLicence.php")
    Call<AllApiResponse> customerNewLicenceRegistration(@Field("customerId") String customerId,
                                                        @Field("name") String name,
                                                        @Field("contact_number") String contact_number,
                                                        @Field("address") String address,
                                                        @Field("shopName") String shopName,
                                                        @Field("branchName") String branchName,
                                                        @Field("licenseValidity") String licenseValidity,
                                                        @Field("licenseType") String licenseType,
                                                        @Field("amount") String amount,
                                                        @Field("fastBilling") String fastBilling,
                                                        @Field("takeAway") String takeAway,
                                                        @Field("dineIn") String dineIn,
                                                        @Field("mess") String mess);

    @GET("getProfile.php")
    Call<AllApiResponse> getProfile(@Query("userId") String dealerId);

    @FormUrlEncoded
    @POST("insertDealer.php")
    Call<AllApiResponse> insertDealer(@Field("name") String name,
                                      @Field("contact_number") String contact_number,
                                      @Field("address") String address,
                                      @Field("email") String email,
                                      @Field("aadhar_number") String aadhar_number,
                                      @Field("password") String password);

    @FormUrlEncoded
    @POST("updateDealerStatus.php")
    Call<AllApiResponse> updateDealerStatus(@Field("userId") String userId,
                                            @Field("action") String action);

    @GET("getDealerReport.php")
    Call<AllApiResponse> getDealerReport(@Query("dealerId") String dealerId);

    @GET("getDealerSalesOverview.php")
    Call<AllApiResponse> getDealerSalesOverview();

    @GET("getDealerSalesOverview.php")
    Call<AllApiResponse> getDealerSalesOverview(@Query("limit") int limit);

    @GET("getCustomerComboList.php")
    Call<AllApiResponse> getCustomerComboList(@Query("userId") String userId);

    @FormUrlEncoded
    @POST("insertCustomerCombo.php")
    Call<AllApiResponse> insertCustomerCombo(@Field("userId") String userId,
                                             @Field("comboName") String comboName,
                                             @Field("comboPrice") String comboPrice,
                                             @Field("comboNetworkStatus") String comboNetworkStatus,
                                             @Field("comboCode") String comboCode,
                                             @Field("comboCGST") String comboCGST,
                                             @Field("comboSGST") String comboSGST,
                                             @Field("comboActiveStatus") String comboActiveStatus,
                                             @Field("comboDeletedStatus") String comboDeletedStatus);

    @GET("getDeviceList.php")
    Call<AllApiResponse> getDeviceList(@Query("customerId") String customerId);

    @GET("getErrorLogList.php")
    Call<AllApiResponse> getErrorLogList();

    @GET("getErrorLogList.php")
    Call<AllApiResponse> getErrorLogList(@Query("limit") int limit);

    @GET("getErrorLogDetails.php")
    Call<AllApiResponse> getErrorLogDetails(@Query("id") String id);

    @FormUrlEncoded
    @POST("updateErrorLogResolution.php")
    Call<AllApiResponse> updateErrorLogResolution(@Field("id") String id,
                                                   @Field("resolutionNotes") String resolutionNotes,
                                                   @Field("resolvedBy") String resolvedBy);

    @GET("getCustomerSales.php")
    Call<AllApiResponse> getCustomerSales(@Query("customerId") String customerId,
                                          @Query("invoiceDate") String invoiceDate);

    @GET("getCustomerCatalogSummary.php")
    Call<AllApiResponse> getCustomerCatalogSummary(@Query("customerId") String customerId);

    @GET("getDealerCustomerList.php")
    Call<AllApiResponse> getDealerCustomerList(@Query("dealerId") String dealerId);

    @GET("getDealerLicenseList.php")
    Call<AllApiResponse> getDealerLicenseList(@Query("dealerId") String dealerId);

    @GET("getSalesOverviewReport.php")
    Call<AllApiResponse> getSalesOverviewReport();

    @GET("getCustomerReport.php")
    Call<AllApiResponse> getCustomerReport();

    @GET("getLicenseReport.php")
    Call<AllApiResponse> getLicenseReport();

    @GET("getBranchReport.php")
    Call<AllApiResponse> getBranchReport();

    @GET("getDeviceReport.php")
    Call<AllApiResponse> getDeviceReport();

    @GET("getSalesDashboard.php")
    Call<AllApiResponse> getSalesDashboard();

    @GET("getRecentInvoices.php")
    Call<AllApiResponse> getRecentInvoices(@Query("limit") int limit, @Query("q") String q);

    @GET("getInvoiceDetails.php")
    Call<AllApiResponse> getInvoiceDetails(@Query("invoiceId") String invoiceId);

    @GET("getCrashList.php")
    Call<AllApiResponse> getCrashList(@Query("q") String q, @Query("app") String app, @Query("status") String status);

    @GET("getCrashDetails.php")
    Call<AllApiResponse> getCrashDetails(@Query("crashId") String crashId);

    @GET("getCrashAnalytics.php")
    Call<AllApiResponse> getCrashAnalytics();

    @FormUrlEncoded
    @POST("updateCrashStatus.php")
    Call<AllApiResponse> updateCrashStatus(@Field("crashId") String crashId, @Field("status") String status);

    @GET("getSupportTickets.php")
    Call<AllApiResponse> getSupportTickets(@Query("status") String status);

    @GET("getSupportTicketDetails.php")
    Call<AllApiResponse> getSupportTicketDetails(@Query("ticketId") String ticketId);

    @FormUrlEncoded
    @POST("createSupportTicket.php")
    Call<AllApiResponse> createSupportTicket(@Field("appName") String appName,
                                             @Field("category") String category,
                                             @Field("subject") String subject,
                                             @Field("description") String description);

    @FormUrlEncoded
    @POST("replySupportTicket.php")
    Call<AllApiResponse> replySupportTicket(@Field("ticketId") String ticketId,
                                            @Field("message") String message,
                                            @Field("sender") String sender);

    @FormUrlEncoded
    @POST("updateSupportTicketStatus.php")
    Call<AllApiResponse> updateSupportTicketStatus(@Field("ticketId") String ticketId,
                                                   @Field("status") String status);

    @GET("getWebsiteContacts.php")
    Call<AllApiResponse> getWebsiteContacts(@Query("status") String status);

    @GET("getWebsiteContactDetails.php")
    Call<AllApiResponse> getWebsiteContactDetails(@Query("contactId") String contactId);

    @FormUrlEncoded
    @POST("updateWebsiteContactStatus.php")
    Call<AllApiResponse> updateWebsiteContactStatus(@Field("contactId") String contactId,
                                                    @Field("status") String status);

    @Multipart
    @POST("catalogImportValidate.php")
    Call<CatalogImportPreviewResponse> catalogImportValidate(@Part("customerId") RequestBody customerId,
                                                             @Part("importType") RequestBody importType,
                                                             @Part MultipartBody.Part importFile);

    @FormUrlEncoded
    @POST("catalogImportConfirm.php")
    Call<CatalogImportPreviewResponse> catalogImportConfirm(@Field("customerId") String customerId,
                                                            @Field("importSessionId") String importSessionId);

    @GET("catalogImportHistory.php")
    Call<CatalogImportHistoryResponse> catalogImportHistory(@Query("customerId") String customerId,
                                                            @Query("importType") String importType);

}
