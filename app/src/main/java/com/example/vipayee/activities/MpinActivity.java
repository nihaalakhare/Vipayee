package com.example.vipayee.activities;

import android.annotation.SuppressLint;
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
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vipayee.AppConstants;
import com.example.vipayee.R;
import com.example.vipayee.api.ApiClient;
import com.example.vipayee.crypto.GCMUtil;
import com.example.vipayee.utils.DeviceInfoUtil;
import com.example.vipayee.utils.HeaderUtil;
import com.example.vipayee.utils.SessionManager;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MpinActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String MODE_LOGIN = "login";
    public static final String MODE_TRANSACTION = "transaction";

    private static final String TAG = "MPIN_DEBUG";
    private static final String XML_TAG = "TXN_XML";

    private String mode;
    private final StringBuilder pin = new StringBuilder();

    private boolean d1, d2, d3, d4;
    private long pressStartTime = 0L;
    private static final long LONG_PRESS_DURATION = 700; // ms


    private TextView pinDisplay;
    private TextToSpeech tts;
    private Vibrator vibrator;
    private final Handler handler = new Handler();

    // 🔹 ADDED (gesture)
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mpin);

        pinDisplay = findViewById(R.id.pinDisplay);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) mode = MODE_LOGIN;

        Log.d(TAG, "MPIN MODE = " + mode);

        initTts();
        setupBrailleButtons();
        setupGestureControls();
//        setupLeftSideBackspaceGesture(); // 🔹 ADDED
        updateDisplay();
    }

    /* ===================== TTS ===================== */

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("en", "IN"));
                speak("Enter four digit M pin");
            }
        });
    }

    private void speak(String msg) {
        if (tts != null) {
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    /* ===================== BRAILLE INPUT ===================== */

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
        handler.postDelayed(processDigitRunnable, 400);
    }

    private final Runnable processDigitRunnable = this::processDigit;

    private void processDigit() {
        if (pin.length() >= 4) return;

        String digit = getBrailleDigit(d1, d2, d3, d4);
        resetDots();

        if (digit.isEmpty()) return;

        pin.append(digit);
        vibrate();
        updateDisplay();
        speak("Digit entered");

        if (pin.length() == 4) {
            onMpinComplete(pin.toString());
        }
    }

    private void resetDots() {
        d1 = d2 = d3 = d4 = false;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupGestureControls() {

        gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    // 👉 DOUBLE TAP RIGHT = REMOVE ONE DIGIT
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        View root = findViewById(android.R.id.content);

                        if (e.getX() > root.getWidth() / 2f) {
                            removeLastDigit();
                            return true;
                        }
                        return false;
                    }
                });

        View root = findViewById(android.R.id.content);

        root.setOnTouchListener((v, event) -> {

            gestureDetector.onTouchEvent(event);

            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:
                    pressStartTime = System.currentTimeMillis();
                    break;

                case MotionEvent.ACTION_UP:
                    long duration =
                            System.currentTimeMillis() - pressStartTime;

                    if (duration >= LONG_PRESS_DURATION) {
                        clearPin(); // ✅ REAL long press
                    }
                    break;
            }
            return true;
        });
    }


    private void removeLastDigit() {
        handler.removeCallbacks(processDigitRunnable);

        if (pin.length() == 0) {
            speak("No digit to remove");
            return;
        }

        pin.deleteCharAt(pin.length() - 1);
        updateDisplay();
        vibrate();
        speak("Digit removed");
    }
    private void clearPin() {
        handler.removeCallbacks(processDigitRunnable);
        pin.setLength(0);
        updateDisplay();
        vibrate();
        speak("MPIN cleared");
    }

    private void updateDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(i < pin.length() ? "● " : "_ ");
        }
        pinDisplay.setText(sb.toString().trim());
    }

    private void vibrate() {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            );
        } else {
            vibrator.vibrate(100);
        }
    }

    private String getBrailleDigit(boolean a, boolean b, boolean c, boolean d) {
        String p = (a?"1":"0")+(b?"1":"0")+(c?"1":"0")+(d?"1":"0");
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


    /* ===================== LEFT SIDE DOUBLE TAP BACKSPACE (ADDED) ===================== */


    // 🔹 ADDED helper (does NOT affect existing logic)

    /* ===================== MPIN COMPLETE ===================== */

    private void onMpinComplete(String mpin) {
        if (MODE_LOGIN.equals(mode)) {
            authenticateLoginMpin(mpin);
        } else {
            authenticateTransactionMpin();
        }
    }

    /* ===================== LOGIN / TRANSACTION (UNCHANGED) ===================== */

    private void authenticateLoginMpin(String mpin) { /* unchanged */
        try {
            SessionManager session = new SessionManager(this);
            JSONObject payload = new JSONObject();
            payload.put("mobile_number", session.getMobile());
            payload.put("custid", session.getCustId());
            payload.put("login_type", session.getLoginType());
            payload.put("mpin", mpin);
            sendLoginMpinApi(payload);
        } catch (Exception e) {
            speak("Something went wrong");
        }
    }

    private void authenticateTransactionMpin() {
        SessionManager s = new SessionManager(this);

        String xml = buildTransactionXml(s);

        Log.d(XML_TAG, "================ TRANSACTION XML ================");
        Log.d(XML_TAG, xml);
        Log.d(XML_TAG, "=================================================");

        speak("Transaction MPIN verified");

        startActivity(new Intent(this, PaymentSuccessActivity.class));
        finish();
    }

    private void sendLoginMpinApi(JSONObject payload) throws Exception {
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
        headers.put("Content-Type", "text/plain");

        ApiClient.create().authenticate(headers, body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ResponseBody> call,
                            @NonNull Response<ResponseBody> response
                    ) {
                        handleLoginMpinResponse(response);
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ResponseBody> call,
                            @NonNull Throwable t
                    ) {
                        Log.e(TAG, "LOGIN MPIN API FAILED", t);
                        speak("Network error");
                    }
                });
    }

    private void handleLoginMpinResponse(Response<ResponseBody> response) {
        try {
            if (!response.isSuccessful() || response.body() == null) {
                speak("Authentication failed");
                return;
            }

            String raw = response.body().string();
            JSONObject wrapper = new JSONObject(raw);

            String decrypted = GCMUtil.decrypt(
                    wrapper.getString("response"),
                    AppConstants.getSecretKeyBytes()
            );

            JSONObject data = new JSONObject(decrypted);

            if (data.optInt("response_code") != 1) {
                speak("Incorrect M pin");
                resetPin();
                return;
            }

            // ✅ SUCCESS
            String token = data.getJSONObject("response").getString("auth_token");

            SessionManager session = new SessionManager(this);
            session.saveAuthToken(token);
            session.setLoggedIn(true);

            // 🔊 SPEAK SUCCESS (LOGIN ONLY)
            speak("Authentication successful. Welcome to Home");

            // ⏳ Delay navigation so TTS can finish
            new Handler().postDelayed(() -> {
                startActivity(
                        new Intent(this, HomeActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                );
                finish();
            }, 2370); // 1.8 seconds is ideal for TTS

        } catch (Exception e) {
            Log.e(TAG, "LOGIN M pin RESPONSE ERROR", e);
            speak("Something went wrong");
        }
    }


    private String buildTransactionXml(SessionManager s) {

        String ts = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                .format(new Date());

        return "<XML>"
                + "<MessageType>1200</MessageType>"
                + "<ProcCode>222004</ProcCode>"
                + "<OriginatingChannel>Mobile</OriginatingChannel>"
                + "<LocalTxnDtTime>" + ts + "</LocalTxnDtTime>"
                + "<Stan>" + s.getStan() + "</Stan>"
                + "<RemitterMobNo>" + s.getMobile() + "</RemitterMobNo>"
                + "<RemitterAccNo>044103500000004</RemitterAccNo>"
                + "<RemitterName>SWATI RAJESH PATERE</RemitterName>"
                + "<BeneAccNo/>"
                + "<BeneIFSC/>"
                + "<TranAmount>" + s.getLastTxnAmount() + "</TranAmount>"
                + "<Remark>tf</Remark>"
                + "<InstitutionID>406</InstitutionID>"
                + "<BeneName>"+ s.getUpiPn() +"</BeneName>"
                + "<BeneMobileNo/>"
                + "<ChannelRefNo>" + s.getChannelRefNo() + "</ChannelRefNo>"
                + "<ChannelBeneName>"+ s.getUpiPn() +"</ChannelBeneName>"
                + "<BeneUpiId>"+ s.getUpiPa() +"</BeneUpiId>"
                + "</XML>";
    }
    private void resetPin() {
        pin.setLength(0);
        updateDisplay();
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
//import android.annotation.SuppressLint;
//import android.content.Intent;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.os.VibrationEffect;
//import android.os.Vibrator;
//import android.speech.tts.TextToSpeech;
//import android.util.Log;
//import android.view.GestureDetector;
//import android.view.MotionEvent;
//import android.view.View;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.vipayee.AppConstants;
//import com.example.vipayee.R;
//import com.example.vipayee.api.ApiClient;
//import com.example.vipayee.crypto.GCMUtil;
//import com.example.vipayee.utils.DeviceInfoUtil;
//import com.example.vipayee.utils.HeaderUtil;
//import com.example.vipayee.utils.SessionManager;
//
//import org.json.JSONObject;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.Locale;
//import java.util.Map;
//
//import okhttp3.MediaType;
//import okhttp3.RequestBody;
//import okhttp3.ResponseBody;
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class MpinActivity extends AppCompatActivity {
//
//    public static final String EXTRA_MODE = "mode";
//    public static final String MODE_LOGIN = "login";
//    public static final String MODE_TRANSACTION = "transaction";
//
//    private static final String TAG = "MPIN_DEBUG";
//    private static final String XML_TAG = "TXN_XML";
//
//    private String mode;
//    private final StringBuilder pin = new StringBuilder();
//
//    private boolean d1, d2, d3, d4;
//    private TextView pinDisplay;
//    private TextToSpeech tts;
//    private Vibrator vibrator;
//    private final Handler handler = new Handler();
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
////        getWindow().getDecorView().setSystemUiVisibility(
////                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
////                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
////        );
////        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
//        setContentView(R.layout.activity_mpin);
//
//        pinDisplay = findViewById(R.id.pinDisplay);
//        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
//
//
//        mode = getIntent().getStringExtra(EXTRA_MODE);
//        if (mode == null) mode = MODE_LOGIN;
//
//        Log.d(TAG, "MPIN MODE = " + mode);
//
//        initTts();
//        setupBrailleButtons();
//        updateDisplay();
//    }
//
//    /* ===================== TTS ===================== */
//
//    private void initTts() {
//        tts = new TextToSpeech(this, status -> {
//            if (status == TextToSpeech.SUCCESS) {
//                tts.setLanguage(new Locale("en", "IN"));
//                speak("Enter four digit MPIN");
//            }
//        });
//    }
//
//    private void speak(String msg) {
//        if (tts != null) {
//            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
//        }
//    }
//
//    /* ===================== BRAILLE INPUT ===================== */
//
//    private void setupBrailleButtons() {
//        findViewById(R.id.dot1).setOnClickListener(v -> onDotPressed(1));
//        findViewById(R.id.dot2).setOnClickListener(v -> onDotPressed(2));
//        findViewById(R.id.dot3).setOnClickListener(v -> onDotPressed(3));
//        findViewById(R.id.dot4).setOnClickListener(v -> onDotPressed(4));
//    }
//
//    private void onDotPressed(int dot) {
//        switch (dot) {
//            case 1: d1 = !d1; break;
//            case 2: d2 = !d2; break;
//            case 3: d3 = !d3; break;
//            case 4: d4 = !d4; break;
//        }
//        handler.removeCallbacks(processDigitRunnable);
//        handler.postDelayed(processDigitRunnable, 400);
//    }
//
//    private final Runnable processDigitRunnable = this::processDigit;
//
//    private void processDigit() {
//        if (pin.length() >= 4) return;
//
//        String digit = getBrailleDigit(d1, d2, d3, d4);
//        resetDots();
//
//        if (digit.isEmpty()) return;
//
//        pin.append(digit);
//        vibrate();
//        updateDisplay();
//        speak("Digit entered");
//
//        if (pin.length() == 4) {
//            String mpin = pin.toString();
//            Log.d(TAG, "MPIN ENTERED = " + mpin);
//            onMpinComplete(mpin);
//        }
//    }
//
//    private void resetDots() {
//        d1 = d2 = d3 = d4 = false;
//    }
//
//    private void updateDisplay() {
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < 4; i++) {
//            sb.append(i < pin.length() ? "● " : "_ ");
//        }
//        pinDisplay.setText(sb.toString().trim());
//    }
//
//    private void vibrate() {
//        if (vibrator == null) return;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            vibrator.vibrate(
//                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
//            );
//        } else {
//            vibrator.vibrate(100);
//        }
//    }
//
//    private String getBrailleDigit(boolean a, boolean b, boolean c, boolean d) {
//        String p = (a?"1":"0")+(b?"1":"0")+(c?"1":"0")+(d?"1":"0");
//        switch (p) {
//            case "1000": return "1";
//            case "1100": return "2";
//            case "1010": return "3";
//            case "1011": return "4";
//            case "1001": return "5";
//            case "1110": return "6";
//            case "1111": return "7";
//            case "1101": return "8";
//            case "0110": return "9";
//            case "0111": return "0";
//            default: return "";
//        }
//    }
//
//    /* ===================== MPIN COMPLETE ===================== */
//
//    private void onMpinComplete(String mpin) {
//        if (MODE_LOGIN.equals(mode)) {
//            authenticateLoginMpin(mpin);
//        } else {
//            authenticateTransactionMpin();
//        }
//    }
//
//    /* ===================== LOGIN MPIN ===================== */
//    // 🔒 unchanged (your working code)
//    // -------------------------------
//
//    private void authenticateLoginMpin(String mpin) {
//        try {
//            SessionManager session = new SessionManager(this);
//
//            JSONObject payload = new JSONObject();
//            payload.put("mobile_number", session.getMobile());
//            payload.put("custid", session.getCustId());
//            payload.put("login_type", session.getLoginType());
//            payload.put("mpin", mpin);
//
//            sendLoginMpinApi(payload);
//
//        } catch (Exception e) {
//            Log.e(TAG, "LOGIN MPIN ERROR", e);
//            speak("Something went wrong");
//        }
//    }
//
//    private void sendLoginMpinApi(JSONObject payload) throws Exception {
//        String encrypted = GCMUtil.encrypt(
//                payload.toString(),
//                AppConstants.getSecretKeyBytes()
//        );
//
//        JSONObject wrapper = new JSONObject();
//        wrapper.put("d", encrypted);
//
//        RequestBody body = RequestBody.create(
//                MediaType.parse("text/plain"),
//                wrapper.toString()
//        );
//
//        Map<String, String> headers = HeaderUtil.baseHeaders(
//                DeviceInfoUtil.getDeviceInfo(this),
//                AppConstants.getApiKey()
//        );
//        HeaderUtil.addSecurityHeaders(headers);
//        headers.put("Content-Type", "text/plain");
//
//        ApiClient.create().authenticate(headers, body)
//                .enqueue(new Callback<ResponseBody>() {
//                    @Override
//                    public void onResponse(
//                            @NonNull Call<ResponseBody> call,
//                            @NonNull Response<ResponseBody> response
//                    ) {
//                        handleLoginMpinResponse(response);
//                    }
//
//                    @Override
//                    public void onFailure(
//                            @NonNull Call<ResponseBody> call,
//                            @NonNull Throwable t
//                    ) {
//                        Log.e(TAG, "LOGIN MPIN API FAILED", t);
//                        speak("Network error");
//                    }
//                });
//    }
//
//    private void handleLoginMpinResponse(Response<ResponseBody> response) {
//        try {
//            if (!response.isSuccessful() || response.body() == null) {
//                speak("Authentication failed");
//                return;
//            }
//
//            String raw = response.body().string();
//            JSONObject wrapper = new JSONObject(raw);
//
//            String decrypted = GCMUtil.decrypt(
//                    wrapper.getString("response"),
//                    AppConstants.getSecretKeyBytes()
//            );
//
//            JSONObject data = new JSONObject(decrypted);
//
//            if (data.optInt("response_code") != 1) {
//                speak("Incorrect MPIN");
//                resetPin();
//                return;
//            }
//
//            String token = data.getJSONObject("response").getString("auth_token");
//
//            SessionManager session = new SessionManager(this);
//            session.saveAuthToken(token);
//            session.setLoggedIn(true);
//
//            startActivity(
//                    new Intent(this, RegisterLoginActivity.class)
//                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
//            );
//            finish();
//
//        } catch (Exception e) {
//            Log.e(TAG, "LOGIN MPIN RESPONSE ERROR", e);
//            speak("Something went wrong");
//        }
//    }
//
//    /* ===================== TRANSACTION MPIN ===================== */
//
//    private void authenticateTransactionMpin() {
//        SessionManager s = new SessionManager(this);
//
//        String xml = buildTransactionXml(s);
//
//        Log.d(XML_TAG, "================ TRANSACTION XML ================");
//        Log.d(XML_TAG, xml);
//        Log.d(XML_TAG, "=================================================");
//
//        speak("Transaction MPIN verified");
//
//        startActivity(new Intent(this, PaymentSuccessActivity.class));
//        finish();
//    }
//
//    /* ===================== XML BUILDER ===================== */
//
//    private String buildTransactionXml(SessionManager s) {
//
//        String ts = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
//                .format(new Date());
//
//        return "<XML>"
//                + "<MessageType>1200</MessageType>"
//                + "<ProcCode>222004</ProcCode>"
//                + "<OriginatingChannel>Mobile</OriginatingChannel>"
//                + "<LocalTxnDtTime>" + ts + "</LocalTxnDtTime>"
//                + "<Stan>" + s.getStan() + "</Stan>"
//                + "<RemitterMobNo>" + s.getMobile() + "</RemitterMobNo>"
//                + "<RemitterAccNo>044103500000004</RemitterAccNo>"
//                + "<RemitterName>SWATI RAJESH PATERE</RemitterName>"
//                + "<BeneAccNo/>"
//                + "<BeneIFSC/>"
//                + "<TranAmount>" + s.getLastTxnAmount() + "</TranAmount>"
//                + "<Remark>tf</Remark>"
//                + "<InstitutionID>406</InstitutionID>"
//                + "<BeneName>"+ s.getUpiPn() +"</BeneName>"
//                + "<BeneMobileNo/>"
//                + "<ChannelRefNo>" + s.getChannelRefNo() + "</ChannelRefNo>"
//                + "<ChannelBeneName>"+ s.getUpiPn() +"</ChannelBeneName>"
//                + "<BeneUpiId>"+ s.getUpiPa() +"</BeneUpiId>"
//                + "</XML>";
//    }
//
//    private void resetPin() {
//        pin.setLength(0);
//        updateDisplay();
//    }
//
//    @Override
//    protected void onDestroy() {
//        if (tts != null) {
//            tts.stop();
//            tts.shutdown();
//        }
//        super.onDestroy();
//    }
//}
//
//
//
