package com.example.vipayee.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vipayee.R;
import com.example.vipayee.utils.BaseActivity;

public class EnterTpinActivity extends BaseActivity {

    private static final String HARDCODED_TPIN = "1234";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_tpin);

        EditText etTpin = findViewById(R.id.etTpin);
        Button btnConfirm = findViewById(R.id.btnConfirm);

        String upiUri = getIntent().getStringExtra("upi_uri");
        String amount = getIntent().getStringExtra("amount");

        btnConfirm.setOnClickListener(v -> {
            String enteredTpin = etTpin.getText().toString().trim();

            if (enteredTpin.isEmpty()) {
                etTpin.setError("Enter TPIN");
                return;
            }

            if (!enteredTpin.equals(HARDCODED_TPIN)) {
                Toast.makeText(this, "Incorrect TPIN", Toast.LENGTH_SHORT).show();
                return;
            }

            // TPIN verified → success
            Intent intent = new Intent(this, PaymentSuccessActivity.class);
            intent.putExtra("amount", amount);
            startActivity(intent);
            finish();
        });
    }
}
