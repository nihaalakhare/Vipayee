package com.example.vipayee.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vipayee.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager session = new SessionManager(this);

        if (!session.isRegistered()) {
            // First-time user
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            // Registered user → always ask MPIN
            startActivity(new Intent(this, MpinActivity.class));
        }

        finish();
    }

}
