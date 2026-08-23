package com.posbillingwala.owner.Retrofit;


import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiInterface {

    @FormUrlEncoded
    @POST("Login.php")
    Call<LoginResponse> loginCheck(@Field("contactNumber") String contactNumber);

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

    @FormUrlEncoded
    @POST("insertCustomerCategory.php")
    Call<AllApiResponse> saveCategory(@Field("userId") String userId,
                                      @Field("categoryName") String categoryName,
                                      @Field("categoryNetworkStatus") String categoryNetworkStatus);

    @FormUrlEncoded
    @POST("deleteCategory.php")
    Call<AllApiResponse> deleteCategory(@Field("categoryId") String categoryId);


    @FormUrlEncoded
    @POST("updateCategory.php")
    Call<AllApiResponse> updateCategory(@Field("categoryId") String categoryId,
                                        @Field("categoryName") String categoryName);

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
                                       @Field("productCode") String productCode);

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
                                     @Field("productNetworkStatus") String productNetworkStatus);

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


}
