package com.example.vipayee.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;

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

public class RegistrationGenerateTpinActivity extends BaseActivity {

    private static final String TAG = "REG_GEN_TPIN";

    private TextInputEditText etPin, etConfirmPin;
    private Button btnGenerate;

    private SessionManager session;
    private String otp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_generate_tpin);

        session = new SessionManager(this);
        otp = getIntent().getStringExtra("otp");

        etPin = findViewById(R.id.etPin);
        etConfirmPin = findViewById(R.id.etConfirmPin);
        btnGenerate = findViewById(R.id.btnGenerateTpin);

        btnGenerate.setOnClickListener(v -> validateAndGenerate());
    }

    private void validateAndGenerate() {

        String pin = etPin.getText() != null
                ? etPin.getText().toString().trim()
                : "";

        String confirmPin = etConfirmPin.getText() != null
                ? etConfirmPin.getText().toString().trim()
                : "";

        if (pin.length() != 4) {
            etPin.setError("Enter 4 digit TPIN");
            return;
        }

        if (!pin.equals(confirmPin)) {
            etConfirmPin.setError("TPIN does not match");
            return;
        }

        generateTpinApi(pin, confirmPin);
    }

    private void generateTpinApi(String pin, String confirmPin) {

        try {

            JSONObject payload = new JSONObject();
            payload.put("pin_type", "tpin");
            payload.put("pin", pin);
            payload.put("pin_confirmation", confirmPin);
            payload.put("mobile_number", session.getMobile());
            payload.put("security_code", session.getPan());
            payload.put("otp", otp);
            payload.put("custid", session.getCustId());
            payload.put("otp_purpose_code", "MBANK_REG");

            // 🔥 Device info inside body (as per your decrypted sample)
            JSONObject deviceInfo = new JSONObject();
            deviceInfo.put("imei", "");
            deviceInfo.put("icc_no", "");
            deviceInfo.put("android_id", session.getDeviceInfo());
            deviceInfo.put("application_version", "1.0.6");
            deviceInfo.put("android_version", "11");

            payload.put("device_info", deviceInfo);

            Log.d(TAG, "RAW TPIN PAYLOAD: " + payload);

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

            headers.put("auth_token", "");
            HeaderUtil.addSecurityHeaders(headers);

            ApiClient.create()
                    .generatePin(headers, body)
                    .enqueue(new Callback<ResponseBody>() {

                        @Override
                        public void onResponse(
                                @NonNull Call<ResponseBody> call,
                                @NonNull Response<ResponseBody> response) {

                            handleGenerateTpinResponse(response);
                        }

                        @Override
                        public void onFailure(
                                @NonNull Call<ResponseBody> call,
                                @NonNull Throwable t) {

                            Log.e(TAG, "Generate TPIN failed", t);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Generate TPIN error", e);
        }
    }

    private void handleGenerateTpinResponse(Response<ResponseBody> response) {

        try {

            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "TPIN API failed");
                return;
            }

            String raw = response.body().string();
            Log.d(TAG, "TPIN RAW RESPONSE: " + raw);

            JSONObject wrapper = new JSONObject(raw);

            if (!wrapper.optString("response_code").equals("1")) {
                Log.e(TAG, "TPIN error: "
                        + wrapper.optString("error_message"));
                return;
            }

            String decrypted = GCMUtil.decrypt(
                    wrapper.getString("response"),
                    AppConstants.getSecretKeyBytes()
            );

            Log.d(TAG, "TPIN DECRYPTED: " + decrypted);

            // 🔥 Registration Complete
            session.setRegistered(true);

            // Move to login screen
            startActivity(new Intent(
                    this,
                    MpinActivity.class
            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK));

        } catch (Exception e) {
            Log.e(TAG, "TPIN response parse error", e);
        }
    }
}