package com.example.vipayee.utils;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vipayee.activities.MpinActivity;
import com.example.vipayee.utils.SessionManager;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    private static final long GLOBAL_IDLE_TIMEOUT = 30_000; // 30 seconds
    private final Handler idleHandler = new Handler(Looper.getMainLooper());

    private TextToSpeech tts;

    private final Runnable expireRunnable = this::expireSession;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        resetGlobalIdleTimer();
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetGlobalIdleTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        idleHandler.removeCallbacks(expireRunnable);
    }

    private void resetGlobalIdleTimer() {
        idleHandler.removeCallbacks(expireRunnable);
        idleHandler.postDelayed(expireRunnable, GLOBAL_IDLE_TIMEOUT);
    }

    private void expireSession() {

        SessionManager session = new SessionManager(this);
        session.setLoggedIn(false);

        // 🔊 INIT TTS ONLY FOR EXPIRY
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("en", "IN"));
                tts.speak(
                        "Session expired. Please enter M PIN again.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "SESSION_EXPIRED"
                );
            }
        });

        // ⏳ Delay so speech is heard
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }

            Intent i = new Intent(this, MpinActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();

        }, 2000); // 2 seconds
    }
}
