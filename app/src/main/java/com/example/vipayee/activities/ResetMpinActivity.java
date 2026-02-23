package com.example.vipayee.activities;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.EditText;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vipayee.AppConstants;
import com.example.vipayee.api.ApiClient;
import com.example.vipayee.crypto.GCMUtil;
import com.example.vipayee.utils.DeviceInfoUtil;
import com.example.vipayee.utils.HeaderUtil;

import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

import com.example.vipayee.R;
import com.example.vipayee.utils.BaseActivity;

public class ResetMpinActivity extends BaseActivity {

    private static final String TAG = "RESET_MPIN";
    private TextToSpeech tts;

    private EditText etMobile, etCustId, etSecurityCode, etOtp, etNewPin, etConfirmPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_mpin);

        etMobile = findViewById(R.id.etMobile);
        etCustId = findViewById(R.id.etCustId);
        etSecurityCode = findViewById(R.id.etSecurityCode);
        etOtp = findViewById(R.id.etOtp);
        etNewPin = findViewById(R.id.etNewPin);
        etConfirmPin = findViewById(R.id.etConfirmPin);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("en", "IN"));
                speak("Reset M pin screen");
            }
        });

        findViewById(R.id.btnResetMpin).setOnClickListener(v -> submit());
    }


    private void speak(String msg) {
        if (tts != null) {
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void submit() {
        String mobile = etMobile.getText().toString().trim();
        String custId = etCustId.getText().toString().trim();
        String secCode = etSecurityCode.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String pin = etNewPin.getText().toString().trim();
        String confirm = etConfirmPin.getText().toString().trim();

        if (mobile.isEmpty() || custId.isEmpty() || secCode.isEmpty()
                || otp.isEmpty() || pin.isEmpty() || confirm.isEmpty()) {
            toast("All fields are required");
            return;
        }

        if (!pin.equals(confirm)) {
            toast("MPIN does not match");
            return;
        }

        if (pin.length() != 4) {
            toast("MPIN must be 4 digits");
            return;
        }

        triggerGenerateMpinApi(mobile, custId, secCode, otp, pin);
    }

    private void triggerGenerateMpinApi(
            String mobile, String custId, String securityCode,
            String otp, String newPin
    ) {

        try {
            JSONObject payload = new JSONObject();
            payload.put("pin_type", "mpin");
            payload.put("pin", newPin);
            payload.put("pin_confirmation", newPin);
            payload.put("mobile_number", mobile);
            payload.put("security_code", securityCode);
            payload.put("otp", otp);
            payload.put("custid", custId);
            payload.put("otp_purpose_code", "FRGT_MPIN");

            Log.d(TAG, "📦 RAW PAYLOAD: " + payload);

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

            Map<String, String> headers = HeaderUtil.baseHeaders(
                    DeviceInfoUtil.getDeviceInfo(this),
                    AppConstants.getApiKey()
            );
            HeaderUtil.addSecurityHeaders(headers);

            ApiClient.create().generatePin(headers, body)
                    .enqueue(new Callback<ResponseBody>() {

                        @Override
                        public void onResponse(Call<ResponseBody> call,
                                               Response<ResponseBody> response) {
                            handleResponse(response);
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            toast("Network error");
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "RESET MPIN ERROR", e);
            toast("Something went wrong");
        }
    }

    private void handleResponse(Response<ResponseBody> response) {
        try {
            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "❌ generate_pin unsuccessful");
                speak("Failed to reset MPIN");
                return;
            }

            String raw = response.body().string();
            Log.d(TAG, "📨 generate_pin RAW RESPONSE: " + raw);

            JSONObject wrapper = new JSONObject(raw);

            if (wrapper.optInt("response_code") != 1) {
                Log.e(TAG, "❌ generate_pin ERROR: " + wrapper);
                speak(wrapper.optString("error_message", "MPIN reset failed"));
                return;
            }

            String decrypted = GCMUtil.decrypt(
                    wrapper.getString("response"),
                    AppConstants.getSecretKeyBytes()
            );

            Log.d(TAG, "🔓 generate_pin DECRYPTED RESPONSE: " + decrypted);

            speak("M pin reset successful");
            startActivity(new Intent(this, MpinActivity.class));

        } catch (Exception e) {
            Log.e(TAG, "💥 generate_pin RESPONSE ERROR", e);
            speak("Something went wrong");
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}

