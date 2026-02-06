package com.example.vipayee.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vipayee.R;
import com.example.vipayee.utils.SessionManager;

import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private GestureDetector gestureDetector;
    private TextToSpeech tts;

    private Handler idleHandler = new Handler();
    private Runnable idleRunnable;

    private static final long IDLE_TIME = 10_000; // 10 seconds
    private static final int MAX_IDLE_SPEAK = 2;

    private int idleSpeakCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            redirectToMpin();
            return;
        }

        gestureDetector = new GestureDetector(this, new SwipeListener());
        initTts();
        setupIdleWatcher();
    }

    /* ===================== TTS ===================== */

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("en", "IN"));
                speakInstructions();
                resetIdleTimer();
            }
        });
    }

    private void speakInstructions() {
        speak(
                "Swipe up for transactions " +
                        "Swipe left for mini statement " +
                        "Swipe right to check balance"
        );
    }

    private void speak(String msg) {
        if (tts != null) {
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    /* ===================== IDLE HANDLING ===================== */

    private void setupIdleWatcher() {
        idleRunnable = () -> {
            idleSpeakCount++;

            if (idleSpeakCount <= MAX_IDLE_SPEAK) {
                speakInstructions();
                resetIdleTimer();
            } else {
                expireSession();
            }
        };
    }

    private void resetIdleTimer() {
        idleHandler.removeCallbacks(idleRunnable);
        idleHandler.postDelayed(idleRunnable, IDLE_TIME);
    }

    private void expireSession() {
        SessionManager session = new SessionManager(this);
        session.setLoggedIn(false);

        speak("Session expired. Please enter MPIN again.");

        idleHandler.removeCallbacks(idleRunnable);

        new Handler().postDelayed(this::redirectToMpin, 1500);
    }

    private void redirectToMpin() {
        startActivity(
                new Intent(this, MpinActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
        );
        finish();
    }

    /* ===================== GESTURES ===================== */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        idleSpeakCount = 0; // user is active again
        resetIdleTimer();
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private class SwipeListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY) {

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffX > 0) {
                        startActivity(new Intent(
                                HomeActivity.this,
                                BalanceMpinActivity.class
                        ));
                        return true;
                    } else {
                        startActivity(new Intent(
                                HomeActivity.this,
                                MiniStatementActivity.class
                        ));
                        return true;
                    }
                }
            } else {
                if (Math.abs(diffY) > SWIPE_THRESHOLD &&
                        Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffY < 0) {
                        startActivity(new Intent(
                                HomeActivity.this,
                                TransactionOptionActivity.class
                        ));
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /* ===================== LIFECYCLE ===================== */

    @Override
    protected void onResume() {
        super.onResume();
        idleSpeakCount = 0;
        speakInstructions();
        resetIdleTimer();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        idleHandler.removeCallbacks(idleRunnable);
        super.onDestroy();
    }
}
