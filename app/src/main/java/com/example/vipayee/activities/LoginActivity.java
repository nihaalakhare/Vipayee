package com.example.vipayee.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vipayee.R;
import com.example.vipayee.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    EditText etMobile, etCustId;
    Button btnContinue;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_login);

        etMobile = findViewById(R.id.etMobile);
        etCustId = findViewById(R.id.etCustId);
        btnContinue = findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(v -> {

            String mobile = etMobile.getText().toString();
            String custId = etCustId.getText().toString();

            // In real app → this comes from LOGIN API response
            new SessionManager(this)
                    .saveLoginData(mobile, custId, "0");

            startActivity(new Intent(this, MpinActivity.class));
            finish();
        });
    }
}

