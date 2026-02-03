package com.example.vipayee.api;

import com.example.vipayee.AppConstants;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
public class ApiClient {

    public static ApiService create() {
        return new Retrofit.Builder()
                .baseUrl(AppConstants.BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(ApiService.class);
    }
}
