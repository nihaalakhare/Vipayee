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

public class CreateProfileActivity extends BaseActivity {

    private static final String TAG = "CREATE_PROFILE";

    private TextInputEditText etName, etEmail;
    private Button btnCreate;

    private String otp;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);

        session = new SessionManager(this);

        otp = getIntent().getStringExtra("otp");

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        btnCreate = findViewById(R.id.btnCreateProfile);

        btnCreate.setOnClickListener(v -> validateAndCreate());
    }

    private void validateAndCreate() {

        String name = etName.getText() != null
                ? etName.getText().toString().trim()
                : "";

        String email = etEmail.getText() != null
                ? etEmail.getText().toString().trim()
                : "";

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return;
        }

        createProfileApi(name, email);
    }

    private void createProfileApi(String name, String email) {

        try {

            JSONObject payload = new JSONObject();
            payload.put("name", name);
            payload.put("email", email); // optional
            payload.put("mobile_number", session.getMobile());
            payload.put("otp", otp);
            payload.put("custid", session.getCustId());

            Log.d(TAG, "RAW PAYLOAD: " + payload);

            // 🔐 Encrypt
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

            headers.put("action", "CREATE_PROFILE");
            headers.put("auth_token", "");
            HeaderUtil.addSecurityHeaders(headers);

            ApiClient.create()
                    .createProfile(headers, body)
                    .enqueue(new Callback<ResponseBody>() {

                        @Override
                        public void onResponse(
                                @NonNull Call<ResponseBody> call,
                                @NonNull Response<ResponseBody> response) {

                            handleCreateProfileResponse(response);
                        }

                        @Override
                        public void onFailure(
                                @NonNull Call<ResponseBody> call,
                                @NonNull Throwable t) {

                            Log.e(TAG, "API FAILED", t);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "CREATE PROFILE ERROR", e);
        }
    }

    private void handleCreateProfileResponse(
            Response<ResponseBody> response) {

        try {

            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "Create profile failed");
                return;
            }

            String raw = response.body().string();
            Log.d(TAG, "RAW RESPONSE: " + raw);

            JSONObject wrapper = new JSONObject(raw);

            if (!wrapper.optString("response_code").equals("1")) {
                Log.e(TAG, "Profile creation error: "
                        + wrapper.optString("error_message"));
                return;
            }

            String decrypted = GCMUtil.decrypt(
                    wrapper.getString("response"),
                    AppConstants.getSecretKeyBytes()
            );

            Log.d(TAG, "DECRYPTED RESPONSE: " + decrypted);

            Intent intent = new Intent(
                    this,
                    RegistrationGenerateMpinActivity.class
            );

            intent.putExtra("otp", otp);
            startActivity(intent);
            finish();
            // 🔥 Move to MPIN setup
//            Intent intent = new Intent(
//                    this,
//                    MpinSetupActivity.class
//            );
////            startActivity(intent);
//            finish();

        } catch (Exception e) {
            Log.e(TAG, "Response parse error", e);
        }
    }
}
