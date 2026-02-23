package com.example.vipayee.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vipayee.AppConstants;
import com.example.vipayee.R;
import com.example.vipayee.api.ApiClient;
import com.example.vipayee.crypto.GCMUtil;
import com.example.vipayee.utils.BaseActivity;
import com.example.vipayee.utils.DeviceInfoUtil;
import com.example.vipayee.utils.HeaderUtil;
import com.example.vipayee.utils.SessionManager;

import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EnterAmountActivity extends BaseActivity {

    private static final String TAG = "STAN_DEBUG";
    private static final String HARD_CODED_ENCRYPTED_PAYLOAD = AppConstants.stan_payload;

    private EditText etAmount, etName;
    private StringBuilder amountInput = new StringBuilder();
    private TextToSpeech tts;
    private Vibrator vibrator;

    // Braille State
    private boolean d1, d2, d3, d4;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_amount);

        // 1. Initialize Views
        etAmount = findViewById(R.id.etAmount);
        etName = findViewById(R.id.etName);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Prevent system keyboard from appearing
        etAmount.setShowSoftInputOnFocus(false);

        // 2. Handle Intent Data
        String baseUpiUri = getIntent().getStringExtra("upi_uri");
        if (baseUpiUri != null && !baseUpiUri.isEmpty()) {
            String payeeName = extractParam(baseUpiUri, "pn");
            if (payeeName != null) {
                etName.setText(payeeName.replace("+", " "));
            }
        }

        // 3. Initialize Accessibility & Keys
        initTts();
        setupBrailleButtons();
    }

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("en", "IN"));
                speak("Enter amount. Use pattern 2 and 4 to proceed to payment.");
            }
        });
    }

    private void setupBrailleButtons() {
        findViewById(R.id.dot1).setOnClickListener(v -> onDotPressed(1));
        findViewById(R.id.dot2).setOnClickListener(v -> onDotPressed(2));
        findViewById(R.id.dot3).setOnClickListener(v -> onDotPressed(3));
        findViewById(R.id.dot4).setOnClickListener(v -> onDotPressed(4));
    }

    private void onDotPressed(int dot) {
        switch (dot) {
            case 1: d1 = !d1; break;
            case 2: d2 = !d2; break;
            case 3: d3 = !d3; break;
            case 4: d4 = !d4; break;
        }
        handler.removeCallbacks(processDigitRunnable);
        handler.postDelayed(processDigitRunnable, 500); // Wait for multi-dot input
    }

    private final Runnable processDigitRunnable = this::processDigit;

    private void processDigit() {
        String pattern = (d1 ? "1" : "0") + (d2 ? "1" : "0") + (d3 ? "1" : "0") + (d4 ? "1" : "0");
        resetDots();

        // CHECK ACTIONS FIRST
        if (pattern.equals("0101")) { // Special Pattern: Dots 2 and 4 to PAY
            handlePaymentAction();
            return;
        }

        if (pattern.equals("0000")) return;

        // GET DIGIT OR SYMBOL
        String digit = getBrailleDigit(pattern);
        if (!digit.isEmpty()) {
            amountInput.append(digit);
            etAmount.setText(amountInput.toString());
            vibrate();
            speak(digit.equals(".") ? "Decimal" : digit);
        } else {
            speak("Invalid input");
        }
    }

    private String getBrailleDigit(String pattern) {

        switch (pattern) {

            // Numbers 1–9 and 0 (Braille 4-dot mapping)

            case "1000": return "1";
            case "1100": return "2";
            case "1010": return "3";
            case "1011": return "4";
            case "1001": return "5";
            case "1110": return "6";
            case "1111": return "7";
            case "1101": return "8";
            case "0110": return "9";
            case "0111": return "0";

            // Decimal point (example: dots 3 + 4)
            case "0011": return ".";

            default: return "";
        }
    }


    private void handlePaymentAction() {
        String amount = amountInput.toString().trim();

        if (amount.isEmpty()) {
            speak("Please enter an amount");
            return;
        }

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Session expired. Login again.", Toast.LENGTH_LONG).show();
            return;
        }

        session.saveLastTxnAmount(amount);
        speak("Paying " + amount + " rupees. Enter M pin.");

        Intent intent = new Intent(this, MpinActivity.class);
        intent.putExtra(MpinActivity.EXTRA_MODE, MpinActivity.MODE_TRANSACTION);
        startActivity(intent);

        generateStanRrn();
    }

    private void generateStanRrn() {
        try {
            SessionManager session = new SessionManager(this);
            JSONObject wrapper = new JSONObject();
            wrapper.put("d", HARD_CODED_ENCRYPTED_PAYLOAD);

            RequestBody body = RequestBody.create(MediaType.parse("text/plain"), wrapper.toString());
            Map<String, String> headers = HeaderUtil.baseHeaders(DeviceInfoUtil.getDeviceInfo(this), AppConstants.getApiKey());

            HeaderUtil.addSecurityHeaders(headers);
            headers.put("auth_token", session.getAuthToken());
            headers.put("Content-Type", "text/plain");

            ApiClient.create().generateStanRrn(headers, body).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    handleStanResponse(response);
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(TAG, "STAN API FAILURE", t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "STAN EXCEPTION", e);
        }
    }

    private void handleStanResponse(Response<ResponseBody> response) {
        try {
            if (response.isSuccessful() && response.body() != null) {
                String raw = response.body().string();
                JSONObject wrapper = new JSONObject(raw);
                String decrypted = GCMUtil.decrypt(wrapper.optString("response"), AppConstants.getSecretKeyBytes());
                JSONObject data = new JSONObject(decrypted);

                if (data.optInt("response_code") == 1) {
                    JSONObject res = data.getJSONObject("response");
                    new SessionManager(this).saveStanAndChannelRef(res.optString("stan"), res.optString("channel_ref_no"));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "RESPONSE PARSE ERROR", e);
        }
    }

    private void resetDots() { d1 = d2 = d3 = d4 = false; }

    private void speak(String msg) {
        if (tts != null) {
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void vibrate() {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(100);
        }
    }

    private String extractParam(String uri, String key) {
        try {
            return Uri.parse(uri).getQueryParameter(key);
        } catch (Exception e) {
            return null;
        }
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