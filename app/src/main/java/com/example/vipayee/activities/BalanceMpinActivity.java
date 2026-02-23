package com.example.vipayee.activities;



import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;
import com.example.vipayee.AppConstants;
import com.example.vipayee.R;
import com.example.vipayee.api.ApiClient;
import com.example.vipayee.crypto.GCMUtil;
import com.example.vipayee.utils.BaseActivity;
import com.example.vipayee.utils.HeaderUtil;
import com.example.vipayee.utils.SessionManager;

import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BalanceMpinActivity extends BaseActivity {

    private static final String TAG = "BALANCE_BRAILLE";

    private TextView pinDisplay;
    private final StringBuilder pin = new StringBuilder();
    private int digitCount = 0;

    private boolean d1, d2, d3, d4;

    private Vibrator vibrator;
    private TextToSpeech tts;
    private final Handler handler = new Handler();
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance_mpin);

        pinDisplay = findViewById(R.id.pinDisplay);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        initTTS();
        setupGestures();

        findViewById(R.id.dot1).setOnClickListener(v -> onDotPressed(1));
        findViewById(R.id.dot2).setOnClickListener(v -> onDotPressed(2));
        findViewById(R.id.dot3).setOnClickListener(v -> onDotPressed(3));
        findViewById(R.id.dot4).setOnClickListener(v -> onDotPressed(4));
    }

    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("en", "IN"));
                speak("Enter your MPIN.");
            }
        });
    }

    private void speak(String msg) {
        if (tts != null) {
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID");
        }
    }

    private void onDotPressed(int dot) {
        vibrate();
        switch (dot) {
            case 1: d1 = true; break;
            case 2: d2 = true; break;
            case 3: d3 = true; break;
            case 4: d4 = true; break;
        }
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::processDigit, 600);
    }

    private void processDigit() {
        if (pin.length() >= 4) return;

        String digit = getBrailleDigit(d1, d2, d3, d4);
        resetDots();

        if (digit.isEmpty()) {
            speak("Invalid pattern");
            return;
        }

        pin.append(digit);
        digitCount++;
        updateDisplay();
        speak("Digit " + digitCount);

        if (digitCount == 4) {
            speak("Verifying");
            checkBalance();
        }
    }

    private String getBrailleDigit(boolean a, boolean b, boolean c, boolean d) {
        String p = (a ? "1" : "0") + (b ? "1" : "0") + (c ? "1" : "0") + (d ? "1" : "0");
        switch (p) {
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
            default: return "";
        }
    }

    private void checkBalance() {
        try {
            SessionManager session = new SessionManager(this);
            String authToken = session.getAuthToken();

            JSONObject payload = new JSONObject();
            payload.put("acc_no", session.getPrimaryAccNo());

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
                    HeaderUtil.baseHeaders(session.getDeviceInfo(), AppConstants.getApiKey());
            headers.put("auth_token", authToken);
            headers.put("action", "BALANCE_ENQUIRY");
            HeaderUtil.addSecurityHeaders(headers);

            ApiClient.create().balanceEnquiry(headers, body)
                    .enqueue(new Callback<ResponseBody>() {

                        @Override
                        public void onResponse(Call<ResponseBody> call,
                                               Response<ResponseBody> response) {
                            handleResponse(response);
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            speak("Network error");
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Balance request failed", e);
        }
    }

    private void handleResponse(Response<ResponseBody> response) {
        try {
            if (response.body() == null) return;

            String raw = response.body().string();
            JSONObject wrapper = new JSONObject(raw);

            if (wrapper.optInt("response_code") != 1) {
                speak(wrapper.optString("error_message", "Error"));
                resetPin();
                return;
            }

            String decrypted = GCMUtil.decrypt(
                    wrapper.getString("response"),
                    AppConstants.getSecretKeyBytes()
            );

            JSONObject data = new JSONObject(decrypted);
            JSONObject table =
                    data.getJSONObject("response")
                            .getJSONArray("Table")
                            .getJSONObject(0);

            String balanceText = table.optString("Balance", "");
            String withdrawableBalance = extractWithdrawableBalance(balanceText);

            Intent intent = new Intent(this, CheckBalanceActivity.class);
            intent.putExtra("balance_text", withdrawableBalance);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            speak("Balance not available");
            resetPin();
        }
    }

    private String extractWithdrawableBalance(String text) {
        Pattern pattern =
                Pattern.compile("Withdrawable Bal:\\s*INR\\s*([0-9,.]+)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return "₹" + matcher.group(1);
        }
        return "₹0.00";
    }

    private void resetDots() {
        d1 = d2 = d3 = d4 = false;
    }

    private void resetPin() {
        pin.setLength(0);
        digitCount = 0;
        updateDisplay();
    }

    private void updateDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(i < pin.length() ? "● " : "_ ");
        }
        pinDisplay.setText(sb.toString().trim());
    }

    private void vibrate() {
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, 100));
            }
        }
    }

    private void setupGestures() {
        gestureDetector =
                new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (pin.length() > 0) {
                            pin.deleteCharAt(pin.length() - 1);
                            digitCount--;
                            updateDisplay();
                            speak("Deleted");
                        }
                        return true;
                    }
                });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
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










//package com.example.vipayee.activities;
//
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.TextView;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.vipayee.AppConstants;
//import com.example.vipayee.R;
//import com.example.vipayee.api.ApiClient;
//import com.example.vipayee.crypto.AESGCMUtil;
//import com.example.vipayee.utils.HeaderUtil;
//import com.example.vipayee.utils.SessionManager;
//
//import org.json.JSONObject;
//
//import java.util.Map;
//
//import okhttp3.MediaType;
//import okhttp3.RequestBody;
//import okhttp3.ResponseBody;
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class BalanceMpinActivity extends AppCompatActivity {
//
//    private static final String TAG = "BALANCE_FLOW";
//
//    private EditText etMpin; // kept only for UX consistency
//    private Button btnCheck;
//    private TextView tvBalance;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_balance_mpin);
//
//        etMpin = findViewById(R.id.etMpin);
//        btnCheck = findViewById(R.id.btnCheckBalance);
//        tvBalance = findViewById(R.id.tvBalance);
//
//        btnCheck.setOnClickListener(v -> checkBalance());
//    }
//
//    private void checkBalance() {
//        try {
//            SessionManager session = new SessionManager(this);
//
//            //  0. AUTH TOKEN CHECK
//            String authToken = session.getAuthToken();
//            Log.d("AUTH_DEBUG", "Using auth_token = " + authToken);
//
//            if (authToken == null || authToken.isEmpty()) {
//                tvBalance.setText("Session expired. Please login again.");
//                return;
//            }
//
//            // 🔹 1. PLAIN PAYLOAD (MATCH POSTMAN EXACTLY)
//            JSONObject payload = new JSONObject();
//            payload.put("acc_no", "016100100006637"); // ONLY THIS
//
//            Log.d(TAG, "PLAIN PAYLOAD: " + payload);
//
//            //  2. ENCRYPT PAYLOAD
//            String encrypted = AESGCMUtil.encrypt(
//                    payload.toString(),
//                    AppConstants.getSecretKeyBytes()
//            );
//
//            //  3. WRAP AS { "d": "" }
//            JSONObject wrapper = new JSONObject();
//            wrapper.put("d", encrypted);
//
//            RequestBody body = RequestBody.create(
//                    MediaType.parse("application/json; charset=utf-8"),
//                    wrapper.toString()
//            );
//
//            Log.d(TAG, "REQUEST BODY: " + wrapper);
//
//            // 🧾 4. HEADERS (ALREADY CORRECT)
//            Map<String, String> headers = HeaderUtil.baseHeaders(
//                    session.getDeviceInfo(),
//                    AppConstants.getApiKey()
//            );
//
//            HeaderUtil.addSecurityHeaders(headers);
//            headers.put("auth_token", authToken);
//            headers.put("action", "BALANCE_ENQUIRY");
//
//            Log.d("HEADERS_DEBUG", headers.toString());
//
//            // 🔁 5. API CALL
//            ApiClient.create()
//                    .balanceEnquiry(headers, body)
//                    .enqueue(new Callback<ResponseBody>() {
//                        @Override
//                        public void onResponse(
//                                Call<ResponseBody> call,
//                                Response<ResponseBody> response
//                        ) {
//                            handleResponse(response);
//                        }
//
//                        @Override
//                        public void onFailure(Call<ResponseBody> call, Throwable t) {
//                            Log.e(TAG, "API FAILED", t);
//                        }
//                    });
//
//        } catch (Exception e) {
//            Log.e(TAG, "BALANCE ERROR", e);
//        }
//    }
//
//    private void handleResponse(Response<ResponseBody> response) {
//        try {
//            if (!response.isSuccessful() || response.body() == null) {
//                Log.e(TAG, "HTTP ERROR " + response.code());
//                return;
//            }
//
//            String raw = response.body().string();
//            Log.d(TAG, "RAW RESPONSE:\n" + raw);
//
//            JSONObject wrapper = new JSONObject(raw);
//
//            if (wrapper.optInt("response_code") != 1) {
//                String errorMsg = wrapper.optString(
//                        "error_message",
//                        "Balance enquiry failed"
//                );
//                tvBalance.setText(errorMsg);
//                return;
//            }
//
//            String encryptedResp = wrapper.optString("response");
//            if (encryptedResp == null || encryptedResp.isEmpty()) {
//                Log.e(TAG, "Encrypted response missing");
//                return;
//            }
//
//            // 🔓 Decrypt
//            String decrypted = AESGCMUtil.decrypt(
//                    encryptedResp,
//                    AppConstants.getSecretKeyBytes()
//            );
//
//            Log.d(TAG, "DECRYPTED:\n" + decrypted);
//
//            JSONObject data = new JSONObject(decrypted);
//
//            // 🔹 Navigate JSON correctly
//            JSONObject responseObj = data.optJSONObject("response");
//            if (responseObj == null) {
//                tvBalance.setText("Invalid balance response");
//                return;
//            }
//
//            // 🔹 Get Table array
//            if (!responseObj.has("Table")) {
//                tvBalance.setText("Balance not available");
//                return;
//            }
//
//            JSONObject tableObj = responseObj
//                    .getJSONArray("Table")
//                    .getJSONObject(0);
//
//            String balanceText = tableObj.optString("Balance", "");
//
//            Log.d(TAG, "BALANCE TEXT = " + balanceText);
//
//            // 🔹 Extract Withdrawable Balance from string
//            String withdrawable = extractWithdrawableBalance(balanceText);
//
//            if (withdrawable == null) {
//                tvBalance.setText(balanceText); // fallback full text
//            } else {
//                tvBalance.setText("Withdrawable Balance: ₹ " + withdrawable);
//            }
//
//        } catch (Exception e) {
//            Log.e(TAG, "RESPONSE ERROR", e);
//            tvBalance.setText("Failed to read balance");
//        }
//    }
//
//    private String extractWithdrawableBalance(String text) {
//        try {
//            String key = "Withdrawable Bal:";
//            int index = text.indexOf(key);
//            if (index == -1) return null;
//
//            String sub = text.substring(index + key.length()).trim();
//            sub = sub.replace("INR", "").trim();
//
//            return sub;
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//}
//
//











//package com.example.vipayee.activities;
//
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.TextView;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.vipayee.AppConstants;
//import com.example.vipayee.R;
//import com.example.vipayee.api.ApiClient;
//import com.example.vipayee.crypto.AESGCMUtil;
//import com.example.vipayee.utils.HeaderUtil;
//import com.example.vipayee.utils.SessionManager;
//
//import org.json.JSONObject;
//
//import java.util.Map;
//
//import okhttp3.MediaType;
//import okhttp3.RequestBody;
//import okhttp3.ResponseBody;
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class BalanceMpinActivity extends AppCompatActivity {
//
//    private static final String TAG = "BALANCE_FLOW";
//
//    private EditText etMpin; // kept only for UX consistency
//    private Button btnCheck;
//    private TextView tvBalance;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_balance_mpin);
//
//        etMpin = findViewById(R.id.etMpin);
//        btnCheck = findViewById(R.id.btnCheckBalance);
//        tvBalance = findViewById(R.id.tvBalance);
//
//        btnCheck.setOnClickListener(v -> checkBalance());
//    }
//
//    private void checkBalance() {
//        try {
//            SessionManager session = new SessionManager(this);
//
//            //  0. AUTH TOKEN CHECK
//            String authToken = session.getAuthToken();
//            Log.d("AUTH_DEBUG", "Using auth_token = " + authToken);
//
//            if (authToken == null || authToken.isEmpty()) {
//                tvBalance.setText("Session expired. Please login again.");
//                return;
//            }
//
//            // 🔹 1. PLAIN PAYLOAD (MATCH POSTMAN EXACTLY)
//            JSONObject payload = new JSONObject();
//            payload.put("acc_no", "016100100006637"); // ONLY THIS
//
//            Log.d(TAG, "PLAIN PAYLOAD: " + payload);
//
//            //  2. ENCRYPT PAYLOAD
//            String encrypted = AESGCMUtil.encrypt(
//                    payload.toString(),
//                    AppConstants.getSecretKeyBytes()
//            );
//
//            //  3. WRAP AS { "d": "" }
//            JSONObject wrapper = new JSONObject();
//            wrapper.put("d", encrypted);
//
//            RequestBody body = RequestBody.create(
//                    MediaType.parse("application/json; charset=utf-8"),
//                    wrapper.toString()
//            );
//
//            Log.d(TAG, "REQUEST BODY: " + wrapper);
//
//            // 🧾 4. HEADERS (ALREADY CORRECT)
//            Map<String, String> headers = HeaderUtil.baseHeaders(
//                    session.getDeviceInfo(),
//                    AppConstants.getApiKey()
//            );
//
//            HeaderUtil.addSecurityHeaders(headers);
//            headers.put("auth_token", authToken);
//            headers.put("action", "BALANCE_ENQUIRY");
//
//            Log.d("HEADERS_DEBUG", headers.toString());
//
//            // 🔁 5. API CALL
//            ApiClient.create()
//                    .balanceEnquiry(headers, body)
//                    .enqueue(new Callback<ResponseBody>() {
//                        @Override
//                        public void onResponse(
//                                Call<ResponseBody> call,
//                                Response<ResponseBody> response
//                        ) {
//                            handleResponse(response);
//                        }
//
//                        @Override
//                        public void onFailure(Call<ResponseBody> call, Throwable t) {
//                            Log.e(TAG, "API FAILED", t);
//                        }
//                    });
//
//        } catch (Exception e) {
//            Log.e(TAG, "BALANCE ERROR", e);
//        }
//    }
//
//    private void handleResponse(Response<ResponseBody> response) {
//        try {
//            if (!response.isSuccessful() || response.body() == null) {
//                Log.e(TAG, "HTTP ERROR " + response.code());
//                return;
//            }
//
//            String raw = response.body().string();
//            Log.d(TAG, "RAW RESPONSE:\n" + raw);
//
//            JSONObject wrapper = new JSONObject(raw);
//
//            if (wrapper.optInt("response_code") != 1) {
//                String errorMsg = wrapper.optString(
//                        "error_message",
//                        "Balance enquiry failed"
//                );
//                tvBalance.setText(errorMsg);
//                return;
//            }
//
//            String encryptedResp = wrapper.optString("response");
//            if (encryptedResp == null || encryptedResp.isEmpty()) {
//                Log.e(TAG, "Encrypted response missing");
//                return;
//            }
//
//            // 🔓 Decrypt
//            String decrypted = AESGCMUtil.decrypt(
//                    encryptedResp,
//                    AppConstants.getSecretKeyBytes()
//            );
//
//            Log.d(TAG, "DECRYPTED:\n" + decrypted);
//
//            JSONObject data = new JSONObject(decrypted);
//
//            // 🔹 Navigate JSON correctly
//            JSONObject responseObj = data.optJSONObject("response");
//            if (responseObj == null) {
//                tvBalance.setText("Invalid balance response");
//                return;
//            }
//
//            // 🔹 Get Table array
//            if (!responseObj.has("Table")) {
//                tvBalance.setText("Balance not available");
//                return;
//            }
//
//            JSONObject tableObj = responseObj
//                    .getJSONArray("Table")
//                    .getJSONObject(0);
//
//            String balanceText = tableObj.optString("Balance", "");
//
//            Log.d(TAG, "BALANCE TEXT = " + balanceText);
//
//            // 🔹 Extract Withdrawable Balance from string
//            String withdrawable = extractWithdrawableBalance(balanceText);
//
//            if (withdrawable == null) {
//                tvBalance.setText(balanceText); // fallback full text
//            } else {
//                tvBalance.setText("Withdrawable Balance: ₹ " + withdrawable);
//            }
//
//        } catch (Exception e) {
//            Log.e(TAG, "RESPONSE ERROR", e);
//            tvBalance.setText("Failed to read balance");
//        }
//    }
//
//    private String extractWithdrawableBalance(String text) {
//        try {
//            String key = "Withdrawable Bal:";
//            int index = text.indexOf(key);
//            if (index == -1) return null;
//
//            String sub = text.substring(index + key.length()).trim();
//            sub = sub.replace("INR", "").trim();
//
//            return sub;
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//}
//
//
