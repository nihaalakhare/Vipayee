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

public class RegistrationGenerateMpinActivity extends BaseActivity {

    private static final String TAG = "REG_GEN_MPIN";

    private TextInputEditText etPin, etConfirmPin;
    private Button btnGenerate;
    private TextInputEditText etSecurityCode;
    private SessionManager session;
    private String otp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_generate_mpin);

        session = new SessionManager(this);
        otp = getIntent().getStringExtra("otp");
        etSecurityCode = findViewById(R.id.etSecurityCode);
        etPin = findViewById(R.id.etPin);
        etConfirmPin = findViewById(R.id.etConfirmPin);
        btnGenerate = findViewById(R.id.btnGenerateMpin);

        btnGenerate.setOnClickListener(v -> validateAndGenerate());
    }

    private void validateAndGenerate() {

        String securityCode = etSecurityCode.getText() != null
                ? etSecurityCode.getText().toString().trim().toUpperCase()
                : "";

        String pin = etPin.getText() != null
                ? etPin.getText().toString().trim()
                : "";

        String confirmPin = etConfirmPin.getText() != null
                ? etConfirmPin.getText().toString().trim()
                : "";

        if (securityCode.isEmpty()) {
            etSecurityCode.setError("Enter Security Code");
            return;
        }
        if (pin.length() != 4) {
            etPin.setError("Enter 4 digit MPIN");
            return;
        }

        if (!pin.equals(confirmPin)) {
            etConfirmPin.setError("MPIN does not match");
            return;
        }

        generateMpinApi(pin, confirmPin, securityCode);
    }

    private void generateMpinApi(String pin, String confirmPin , String securityCode) {

        try {

            JSONObject payload = new JSONObject();
            payload.put("pin_type", "mpin");
            payload.put("pin", pin);
            payload.put("pin_confirmation", confirmPin);
            payload.put("mobile_number", session.getMobile());
            payload.put("security_code", securityCode); // PAN from profile
            payload.put("otp", otp);
            payload.put("custid", session.getCustId());
            payload.put("otp_purpose_code", "MBANK_REG");

            Log.d(TAG, "RAW MPIN PAYLOAD: " + payload);

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

                            handleGenerateMpinResponse(response);
                        }

                        @Override
                        public void onFailure(
                                @NonNull Call<ResponseBody> call,
                                @NonNull Throwable t) {

                            Log.e(TAG, "Generate MPIN failed", t);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Generate MPIN exception", e);
        }
    }

    private void handleGenerateMpinResponse(Response<ResponseBody> response) {

        try {

            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "MPIN API failed");
                return;
            }

            String raw = response.body().string();
            Log.d(TAG, "MPIN RAW RESPONSE: " + raw);

            JSONObject wrapper = new JSONObject(raw);

            if (!wrapper.optString("response_code").equals("1")) {
                Log.e(TAG, "MPIN error: "
                        + wrapper.optString("error_message"));
                return;
            }

            String decrypted = GCMUtil.decrypt(
                    wrapper.getString("response"),
                    AppConstants.getSecretKeyBytes()
            );

            Log.d(TAG, "MPIN DECRYPTED: " + decrypted);

            // 🔥 Move to TPIN setup next
             startActivity(new Intent(this, RegistrationGenerateTpinActivity.class));

        } catch (Exception e) {
            Log.e(TAG, "MPIN response parse error", e);
        }
    }
}