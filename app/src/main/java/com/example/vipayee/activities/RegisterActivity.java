package com.example.vipayee.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vipayee.R;
import com.example.vipayee.utils.BaseActivity;
import com.example.vipayee.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends BaseActivity {

    private TextInputEditText etMobile, etCustId;
    private TextInputLayout tilMobile, tilCustId;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_login);

        tilMobile = findViewById(R.id.tilMobile);
        tilCustId = findViewById(R.id.tilCustId);

        etMobile = findViewById(R.id.etMobile);
        etCustId = findViewById(R.id.etCustId);

        btnContinue = findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(v -> {
            if (validateInputs()) {
                proceedNext();
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

    private void proceedNext() {

        String mobile = etMobile.getText().toString().trim();
        String custId = etCustId.getText().toString().trim();

        new SessionManager(this)
                .saveLoginData(mobile, custId, "0");

        startActivity(new Intent(this, MpinActivity.class));
        finish();
    }
}
