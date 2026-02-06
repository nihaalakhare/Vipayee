package com.example.vipayee.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vipayee.R;
import com.example.vipayee.utils.BaseActivity;

public class TransactionOptionActivity extends BaseActivity {

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_option);

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
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            // Horizontal Swipes
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // 👉 SWIPE RIGHT: Per XML, this is "Scan QR"
                        startActivity(new Intent(TransactionOptionActivity.this, ScanQrActivity.class));
                    } else {
                        // 👈 SWIPE LEFT: Per XML, this is "Phone Number"
                        // startActivity(new Intent(TransactionOptionActivity.this, PhonePayActivity.class));
                    }
                    return true;
                }
            }
            // Vertical Swipes
            else {
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY < 0) {
                        // 👆 SWIPE UP: Per XML, this is "Generate QR"
                        startActivity(new Intent(TransactionOptionActivity.this, GenerateQrActivity.class));
                    } else {
                        // 👇 SWIPE DOWN: Back/Home
                        finish();
                    }
                    return true;
                }
            }
            return false;
        }
    }
}