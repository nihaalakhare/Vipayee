package com.example.vipayee.api;

import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // 🔐 LOGIN
    @POST("mbank.svc/api/authenticate")
    Call<ResponseBody> authenticate(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );

    // 💰 BALANCE ENQUIRY
    @POST("mbank.svc/api")
    Call<ResponseBody> balanceEnquiry(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );

    @POST("mbank.svc/api/generate_pin")
    Call<ResponseBody> generatePin(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );


    // 🧾 GENERATE STAN / RRN  ✅ (THIS WAS MISSING)
    @POST("mbank.svc/api/generate_stan_rrn")
    Call<ResponseBody> generateStanRrn(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );

    @GET("mbank.svc/api")
    Call<ResponseBody> getProfile(
            @Query("mobile_number") String mobile,
            @Query("custid") String custId,
            @HeaderMap Map<String, String> headers
    );


    // Registration flow Api
    @POST("mbank.svc/api")
    Call<ResponseBody> verifyMobileNumber(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );

    @POST("mbank.svc/api/generate_otp")
    Call<ResponseBody> generateOtp(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );

    @POST("mbank.svc/api")
    Call<ResponseBody> verifyOtpForReg(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );

    @POST("mbank.svc/api")
    Call<ResponseBody> createProfile(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );


}
