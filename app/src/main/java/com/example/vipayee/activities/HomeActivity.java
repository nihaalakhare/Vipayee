package com.example.vipayee.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vipayee.R;
import com.example.vipayee.utils.SessionManager;

public class HomeActivity extends AppCompatActivity {

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, MpinActivity.class));
            finish();
            return;
        }

        gestureDetector = new GestureDetector(this, new SwipeListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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

            // 🔹 Horizontal swipe
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffX > 0) {
                        // 👉 RIGHT → Balance
                        startActivity(new Intent(HomeActivity.this,
                                BalanceMpinActivity.class));
                        return true;
                    } else {
                        // 👈 LEFT → Mini Statement
                        startActivity(new Intent(HomeActivity.this,
                                MiniStatementActivity.class));
                        return true;
                    }
                }
            }
            // 🔹 Vertical swipe
            else {
                if (Math.abs(diffY) > SWIPE_THRESHOLD &&
                        Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffY < 0) {
                        // 👆 UP → Transaction Options
                        startActivity(new Intent(HomeActivity.this,
                                TransactionOptionActivity.class));
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
