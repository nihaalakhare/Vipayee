package com.example.vipayee.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vipayee.AppConstants;
import com.example.vipayee.R;
import com.example.vipayee.api.ApiClient;
import com.example.vipayee.crypto.GCMUtil;
import com.example.vipayee.utils.BaseActivity;
import com.example.vipayee.utils.DeviceInfoUtil;
import com.example.vipayee.utils.HeaderUtil;
import com.example.vipayee.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistrationOtpActivity extends BaseActivity {

    private static final String TAG = "REG_OTP";

    private TextInputEditText etOtp;
    private Button btnVerify;

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_otp);

        session = new SessionManager(this);

        etOtp = findViewById(R.id.etOtp);
        btnVerify = findViewById(R.id.btnVerify);

        generateOtp();

        btnVerify.setOnClickListener(v -> {
            String otp = etOtp.getText() != null
                    ? etOtp.getText().toString().trim()
                    : "";

            if (otp.length() < 4) {
                etOtp.setError("Enter valid OTP");
                return;
            }

            verifyOtp(otp);
        });
    }

    /* ===================== GENERATE OTP ===================== */
    private void showMessage(String message) {
        Log.d(TAG, "📢 " + message);
    }

    private void generateOtp() {

        try {

            String mobile = session.getMobile();
            String custId = session.getCustId();

            JSONObject payload = new JSONObject();
            payload.put("for", mobile);
            payload.put("custid", custId);
            payload.put("purpose_code", "GENERATE_OTP_FOR_MBANK_REG");

            Log.d(TAG, " RAW PAYLOAD: " + payload);

            String encrypted = GCMUtil.encrypt(
                    payload.toString(),
                    AppConstants.getSecretKeyBytes()
            );

            JSONObject wrapper = new JSONObject();
            wrapper.put("d", encrypted);

            RequestBody body = RequestBody.create(
                    MediaType.parse("text/plain"),
                    wrapper.toString()
            );

            Map<String, String> headers =
                    HeaderUtil.baseHeaders(
                            DeviceInfoUtil.getDeviceInfo(this),
                            AppConstants.getApiKey()
                    );

            headers.put("action", "GENERATE_OTP_FOR_MBANK_REG");
            HeaderUtil.addSecurityHeaders(headers);

            ApiClient.create().generateOtp(headers, body)
                    .enqueue(new Callback<ResponseBody>() {

                        @Override
                        public void onResponse(
                                @NonNull Call<ResponseBody> call,
                                @NonNull Response<ResponseBody> response) {
                            handleGenerateOtpResponse(response);
                        }

                        @Override
                        public void onFailure(
                                @NonNull Call<ResponseBody> call,
                                @NonNull Throwable t) {
                            Log.e(TAG, "Network error", t);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "generateOtp exception", e);
        }
    }

    private void handleGenerateOtpResponse(Response<ResponseBody> response) {

        try {

            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "OTP generation failed");
                return;
            }

            String raw = response.body().string();
            Log.d(TAG, "RAW RESPONSE: " + raw);

            JSONObject wrapper = new JSONObject(raw);

            if (wrapper.optInt("response_code") != 1) {
                Log.e(TAG, "OTP not generated");
                return;
            }

            String decrypted = GCMUtil.decrypt(
                    wrapper.getString("response"),
                    AppConstants.getSecretKeyBytes()
            );

            Log.d(TAG, "DECRYPTED RESPONSE: " + decrypted);

        } catch (Exception e) {
            Log.e(TAG, "OTP parse error", e);
        }
    }

    /* ===================== VERIFY OTP (NEXT STEP) ===================== */

//    private void verifyOtp(String otp) {
//
//        try {
//
//            String mobile = session.getMobile();
//
//            JSONObject payload = new JSONObject();
//            payload.put("mobile_number", mobile);
//            payload.put("otp", otp);
//            payload.put("purpose_code", "MBANK_REG");
//
//            Log.d(TAG, " VERIFY OTP RAW PAYLOAD: " + payload);
//
//            String encrypted = GCMUtil.encrypt(
//                    payload.toString(),
//                    AppConstants.getSecretKeyBytes()
//            );
//
//            JSONObject wrapper = new JSONObject();
//            wrapper.put("d", encrypted);
//
//            RequestBody body = RequestBody.create(
//                    MediaType.parse("text/plain"),
//                    wrapper.toString()
//            );
//
//            Map<String, String> headers =
//                    HeaderUtil.baseHeaders(
//                            DeviceInfoUtil.getDeviceInfo(this),
//                            AppConstants.getApiKey()
//                    );
//
//            headers.put("action", "VERIFY_OTP_FOR_REG");
//            headers.put("auth_token", "");
//            HeaderUtil.addSecurityHeaders(headers);
//
//            ApiClient.create().verifyOtpForReg(headers, body)
//                    .enqueue(new Callback<ResponseBody>() {
//
//                        @Override
//                        public void onResponse(
//                                @NonNull Call<ResponseBody> call,
//                                @NonNull Response<ResponseBody> response) {
//
//                            handleVerifyOtpResponse(response);
//                        }
//
//                        @Override
//                        public void onFailure(
//                                @NonNull Call<ResponseBody> call,
//                                @NonNull Throwable t) {
//
//                            Log.e(TAG, " VERIFY OTP FAILED", t);
//                        }
//                    });
//
//        } catch (Exception e) {
//            Log.e(TAG, "VERIFY OTP EXCEPTION", e);
//        }
//    }
//
//    private void handleVerifyOtpResponse(Response<ResponseBody> response) {
//
//        try {
//
//            if (!response.isSuccessful() || response.body() == null) {
//                Log.e(TAG, " VERIFY OTP unsuccessful response");
//                return;
//            }
//
//            String raw = response.body().string();
//            Log.d(TAG, " VERIFY OTP RAW RESPONSE: " + raw);
//
//            JSONObject wrapper = new JSONObject(raw);
//
//            String responseCode = wrapper.optString("response_code", "0");
//
//            if (!responseCode.equals("1")) {
//                Log.e(TAG, "OTP verification failed: " + wrapper);
//                return;
//            }
//
//            String decrypted = GCMUtil.decrypt(
//                    wrapper.getString("response"),
//                    AppConstants.getSecretKeyBytes()
//            );
//
//            Log.d(TAG, " VERIFY OTP DECRYPTED RESPONSE: " + decrypted);
//            Log.d(TAG, " OTP VERIFIED SUCCESSFULLY");
//
//        } catch (Exception e) {
//            Log.e(TAG, " VERIFY OTP RESPONSE ERROR", e);
//        }
//    }
//
private void verifyOtp(String otp) {

    try {

        String mobile = session.getMobile();

        JSONObject payload = new JSONObject();
        payload.put("mobile_number", mobile);
        payload.put("otp", otp);
        payload.put("purpose_code", "MBANK_REG");

        Log.d(TAG, "📦 VERIFY OTP RAW PAYLOAD: " + payload);

        String encrypted = GCMUtil.encrypt(
                payload.toString(),
                AppConstants.getSecretKeyBytes()
        );

        JSONObject wrapper = new JSONObject();
        wrapper.put("d", encrypted);

        RequestBody body = RequestBody.create(
                MediaType.parse("text/plain"),
                wrapper.toString()
        );

        Map<String, String> headers =
                HeaderUtil.baseHeaders(
                        DeviceInfoUtil.getDeviceInfo(this),
                        AppConstants.getApiKey()
                );

        headers.put("action", "VERIFY_OTP_FOR_REG");
        headers.put("auth_token", "");   // registration flow → empty
        HeaderUtil.addSecurityHeaders(headers);

        Log.d(TAG, "📤 VERIFY OTP HEADERS: " + headers);

        ApiClient.create().verifyOtpForReg(headers, body)
                .enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<ResponseBody> call,
                            @NonNull Response<ResponseBody> response) {

                        handleVerifyOtpResponse(response);
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ResponseBody> call,
                            @NonNull Throwable t) {

                        Log.e(TAG, "🚨 VERIFY OTP NETWORK FAILED", t);
                        showMessage("Network error");
                    }
                });

    } catch (Exception e) {
        Log.e(TAG, "💥 VERIFY OTP EXCEPTION", e);
        showMessage("Unexpected error occurred");
    }
}

    private void handleVerifyOtpResponse(Response<ResponseBody> response) {

        try {

            if (!response.isSuccessful() || response.body() == null) {

                Log.e(TAG, "❌ VERIFY OTP unsuccessful response");
                showMessage("Server error: " + response.code());
                return;
            }

            String raw = response.body().string();
            Log.d(TAG, "📨 VERIFY OTP RAW RESPONSE: " + raw);

            if (raw == null || raw.trim().isEmpty()) {

                showMessage("Server Not Responding");
                return;
            }

            JSONObject wrapper = new JSONObject(raw);

            String responseCode =
                    wrapper.optString("response_code", "0");

            if (!responseCode.equals("1")) {

                String errorMessage =
                        wrapper.optString("error_message",
                                "OTP verification failed");

                Log.e(TAG, "❌ OTP verification failed: " + errorMessage);
                showMessage(errorMessage);
                return;
            }

            // Decrypt only if response exists
            if (wrapper.has("response") &&
                    !wrapper.isNull("response")) {

                String decrypted = GCMUtil.decrypt(
                        wrapper.getString("response"),
                        AppConstants.getSecretKeyBytes()
                );

                Log.d(TAG, "🔓 VERIFY OTP DECRYPTED RESPONSE: " + decrypted);
            }

            Log.d(TAG, "✅ OTP VERIFIED SUCCESSFULLY");

            // 🔥 Next Step (for now just log)
            showMessage("OTP verified successfully");


// Move to CreateProfile screen
            Intent intent = new Intent(
                    RegistrationOtpActivity.this,
                    CreateProfileActivity.class
            );

            intent.putExtra("otp",
                    etOtp.getText().toString().trim());

            startActivity(intent);
            finish();
            // TODO: Move to MPIN setup screen

        } catch (Exception e) {

            Log.e(TAG, "💥 VERIFY OTP RESPONSE ERROR", e);
            showMessage("Unexpected error occurred");
        }
    }

}
