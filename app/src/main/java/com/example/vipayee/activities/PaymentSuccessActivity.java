package com.example.vipayee.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vipayee.R;
import com.example.vipayee.utils.BaseActivity;
import com.example.vipayee.utils.SessionManager;

public class PaymentSuccessActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_success);

        TextView tvSuccess = findViewById(R.id.tvSuccess);

        SessionManager session = new SessionManager(this);

        // 🔹 Prefer session-stored amount
        String amount = session.getLastTxnAmount();

        if (amount == null || amount.isEmpty()) {
            amount = "--";
        }

        tvSuccess.setText("Payment Successful\n₹" + amount);

        // ✅ Clear transaction data after success
        session.clearUpiDetails();
        session.clearTransactionData();
    }
}
