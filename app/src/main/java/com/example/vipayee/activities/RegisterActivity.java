package com.example.vipayee.activities;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.speech.tts.TextToSpeech;
import androidx.annotation.NonNull;

import com.example.vipayee.R;
import com.example.vipayee.manager.RegistrationManager;
import com.example.vipayee.utils.BaseActivity;
import com.example.vipayee.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends BaseActivity {

    private static final String TAG = "REGISTER_ACTIVITY";

    private TextInputEditText etMobile, etCustId;
    private TextInputLayout tilMobile, tilCustId;
    private Button btnContinue;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_login);

        initViews();
        setupListeners();
    }
    private void speak(String msg) {
        if (tts != null) {
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID");
        }
    }
    private void initViews() {
        tilMobile = findViewById(R.id.tilMobile);
        tilCustId = findViewById(R.id.tilCustId);

        etMobile = findViewById(R.id.etMobile);
        etCustId = findViewById(R.id.etCustId);

        btnContinue = findViewById(R.id.btnContinue);
    }

    private void setupListeners() {
        btnContinue.setOnClickListener(v -> {
            if (validateInputs()) {
                verifyMobileWithServer();
            }
        });
    }

    private boolean validateInputs() {

        String mobile = etMobile.getText() != null
                ? etMobile.getText().toString().trim() : "";

        String custId = etCustId.getText() != null
                ? etCustId.getText().toString().trim() : "";

        tilMobile.setError(null);
        tilCustId.setError(null);

        if (TextUtils.isEmpty(mobile)) {
            tilMobile.setError("Mobile number is required");
            return false;
        }

        if (mobile.length() != 10) {
            tilMobile.setError("Enter valid 10-digit mobile number");
            return false;
        }

        if (TextUtils.isEmpty(custId)) {
            tilCustId.setError("Customer ID is required");
            return false;
        }

        return true;
    }

    private void verifyMobileWithServer() {

        String mobile = etMobile.getText().toString().trim();
        String custId = etCustId.getText().toString().trim();

        Log.d(TAG, "Verifying mobile: " + mobile);

        RegistrationManager manager =
                new RegistrationManager(this);

        manager.verifyMobile(mobile, custId,
                new RegistrationManager.RegistrationCallback() {

                    @Override
                    public void onSuccess() {

                        Log.d(TAG, "Mobile verification success");

                        // Save data for next steps
                        new SessionManager(RegisterActivity.this)
                                .saveLoginData(mobile, custId, "0");

                        // Move to next step (for now MPIN)
                        startActivity(
                                new Intent(RegisterActivity.this,
                                        RegistrationOtpActivity.class)
                        );

                        finish();
                    }

                    @Override
                    public void onError(@NonNull String message) {

                        Log.e(TAG, "Verification failed: " + message);

                        // Show error in UI
                        tilMobile.setError(message);

                        speak("Verification failed");
                    }
                });
    }
}
