package com.posbillingwala.owner.Retrofit;


import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.CatalogImportHistoryResponse;
import com.posbillingwala.owner.Model.CatalogImportPreviewResponse;
import com.posbillingwala.owner.Model.LoginResponse;

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
    Call<LoginResponse> loginCheck(@Field("contactNumber") String contactNumber,
                                   @Field("password") String password);

    @GET("getTotalCount.php")
    Call<AllApiResponse> getTotalCount(@Query("userId") String userId);

    @GET("getProfile.php")
    Call<AllApiResponse> getProfile(@Query("userId") String userId);

    @GET("getInvoiceList.php")
    Call<AllApiResponse> getInvoiceList(@Query("userId") String userId);

    @GET("getInvoiceProductList.php")
    Call<AllApiResponse> getInvoiceProductList(@Query("invoiceId") String invoiceId);

    @GET("getCustomerCategoryList.php")
    Call<AllApiResponse> getCategoryList(@Query("userId") String userId);

    @GET("getSubcategoryList.php")
    Call<AllApiResponse> getSubcategoryList(@Query("userId") String userId,
                                            @Query("categoryId") String categoryId);

    @FormUrlEncoded
    @POST("insertCustomerCategory.php")
    Call<AllApiResponse> saveCategory(@Field("userId") String userId,
                                      @Field("categoryName") String categoryName,
                                      @Field("categoryNetworkStatus") String categoryNetworkStatus);

    @FormUrlEncoded
    @POST("insertCustomerSubcategory.php")
    Call<AllApiResponse> saveSubcategory(@Field("userId") String userId,
                                         @Field("categoryId") String categoryId,
                                         @Field("subcategoryName") String subcategoryName,
                                         @Field("subcategoryNetworkStatus") String subcategoryNetworkStatus);

    @FormUrlEncoded
    @POST("deleteCategory.php")
    Call<AllApiResponse> deleteCategory(@Field("categoryId") String categoryId);


    @FormUrlEncoded
    @POST("updateCategory.php")
    Call<AllApiResponse> updateCategory(@Field("categoryId") String categoryId,
                                        @Field("categoryName") String categoryName);

    @FormUrlEncoded
    @POST("updateSubcategory.php")
    Call<AllApiResponse> updateSubcategory(@Field("userId") String userId,
                                           @Field("subcategoryId") String subcategoryId,
                                           @Field("subcategoryName") String subcategoryName);

    @FormUrlEncoded
    @POST("deleteSubcategory.php")
    Call<AllApiResponse> deleteSubcategory(@Field("userId") String userId,
                                           @Field("subcategoryId") String subcategoryId);

    @FormUrlEncoded
    @POST("deleteProductPortion.php")
    Call<AllApiResponse> deleteProductPortion(@Field("userId") String userId,
                                              @Field("portionId") String portionId);

    @GET("getCustomerProductList.php")
    Call<AllApiResponse> getProductList(@Query("userId") String userId);

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
                                       @Field("productCode") String productCode,
                                       @Field("subcategoryId") String subcategoryId);

    @GET("getProductDetails.php")
    Call<AllApiResponse> getProductDetails(@Query("productId") String productId);

    @FormUrlEncoded
    @POST("insertCustomerProduct.php")
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
                                     @Field("subcategoryId") String subcategoryId);

    @GET("getPortionMasterList.php")
    Call<AllApiResponse> getPortionMasterList(@Query("userId") String userId);

    @FormUrlEncoded
    @POST("insertCustomerPortionMaster.php")
    Call<AllApiResponse> savePortionMaster(@Field("userId") String userId,
                                           @Field("portionName") String portionName,
                                           @Field("portionMasterDeletedStatus") String portionMasterDeletedStatus,
                                           @Field("portionMasterNetworkStatus") String portionMasterNetworkStatus);

    @GET("getPortionList.php")
    Call<AllApiResponse> getPortionList(@Query("userId") String userId,
                                        @Query("productId") String productId);

    @FormUrlEncoded
    @POST("insertCustomerPortion.php")
    Call<AllApiResponse> savePortion(@Field("userId") String userId,
                                     @Field("productId") String productId,
                                     @Field("portionMasterId") String portionMasterId,
                                     @Field("portionName") String portionName,
                                     @Field("portionPrice") String portionPrice,
                                     @Field("portionSortOrder") String portionSortOrder,
                                     @Field("portionNetworkStatus") String portionNetworkStatus);

    @FormUrlEncoded
    @POST("insertExportAllProduct.php")
    Call<AllApiResponse> insertExportProduct(@Field("customerId") String customerId,
                                             @Field("categoryName") String categoryName,
                                             @Field("productCode") String productCode,
                                             @Field("productName") String productName,
                                             @Field("productUnit") String productUnit,
                                             @Field("productPrice") String productPrice,
                                             @Field("productCGST") String productCGST,
                                             @Field("productSGST") String productSGST);

    @FormUrlEncoded
    @POST("updateReportPin.php")
    Call<AllApiResponse> updateReportPin(@Field("userId") String userId,
                                         @Field("reportPin") String reportPin);

    @FormUrlEncoded
    @POST("updateProfile.php")
    Call<AllApiResponse> updateCustomerDetails(@Field("userId") String customerId,
                                               @Field("customerName") String customerName,
                                               @Field("customerMobileNumber") String customerMobileNumber,
                                               @Field("customerAddress") String customerAddress,
                                               @Field("customerShopName") String customerShopName);

    @FormUrlEncoded
    @POST("updateSaleData.php")
    Call<AllApiResponse> updateSaleData(@Field("licenseId") String licenseId,
                                        @Field("totalSaleData") String totalSaleData,
                                        @Field("todaySaleData") String todaySaleData);

    @GET("getStoreWise.php")
    Call<AllApiResponse> getStoreWise(@Query("userId") String userId);

    @GET("getInvoiceLicenceIdWise.php")
    Call<AllApiResponse> getInvoiceLicenceIdWiseList(@Query("userId") String userId,
                                                     @Query("saleDate") String saleDate);

    @GET("getBranchComparison.php")
    Call<AllApiResponse> getBranchComparison(@Query("userId") String userId);

    @GET("getSalesDashboard.php")
    Call<AllApiResponse> getSalesDashboard(@Query("userId") String userId,
                                           @Query("branchId") String branchId);

    @GET("getSalesOverviewReport.php")
    Call<AllApiResponse> getSalesOverviewReport(@Query("userId") String userId,
                                                @Query("branchId") String branchId);

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
