package com.example.vipayee.activities;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vipayee.R;

import java.util.Locale;

public class BalanceResultActivity extends AppCompatActivity {

    private TextToSpeech tts;
    private boolean isTtsReady = false;
    private String speakText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance_result);

        TextView tvBalanceResult = findViewById(R.id.tvBalanceResult);

        String balance = getIntent().getStringExtra("balance_text");
        if (balance == null || balance.isEmpty()) {
            balance = "₹0.00";
        }

        tvBalanceResult.setText(balance);
        speakText = formatForSpeech(balance);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("en", "IN"));
                isTtsReady = true;
                speak();
            }
        });
    }

    private String formatForSpeech(String balance) {
        return "Your withdrawable balance is " +
                balance.replace("₹", "")
                        .replace(",", "")
                        .trim() +
                " rupees";
    }

    private void speak() {
        if (isTtsReady && tts != null) {
            tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "BALANCE_TTS");
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
