package com.example.vipayee.activities;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vipayee.R;
import com.example.vipayee.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    View boxShape;
    View ovalShape;
    TextView logoText;
    RelativeLayout splashRoot;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
//  |
//  View.SYSTEM_UI_FLAG_FULLSCREEN, | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//        // Transparent bars (just in case)
//        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        // Status bar color (can be changed during animation)
//        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
//        getWindow().setStatusBarColor(Color.WHITE);


        setContentView(R.layout.activity_splash);



        boxShape = findViewById(R.id.boxshape);
        ovalShape = findViewById(R.id.ovalShape);
        logoText = findViewById(R.id.logotext);
        splashRoot = findViewById(R.id.splashroot);

        startAnimationSequence();
    }

    private void startAnimationSequence() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            boxShape.setVisibility(View.VISIBLE);
            boxShape.setScaleX(1f);
            boxShape.setScaleY(1f);
            boxShape.setRotation(0f);
            boxShape.setAlpha(1f);

            // Step 1: Rotate
            boxShape.animate().rotation(225f).setDuration(500).withEndAction(() -> {

                // Step 2: Grow
                boxShape.animate().scaleX(2.0f).scaleY(2.0f).setDuration(500).withEndAction(() -> {

                    // Step 3: Morph into circle
                    GradientDrawable drawable = (GradientDrawable) boxShape.getBackground();
                    ObjectAnimator cornerAnim =
                            ObjectAnimator.ofFloat(drawable, "cornerRadius", 30f, 500f);
                    cornerAnim.setDuration(400);
                    cornerAnim.start();

                    // Step 4: Shrink & fade
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        boxShape.animate()
                                .scaleX(-15f)
                                .scaleY(-15f)
                                .alpha(0f)
                                .setDuration(500)
                                .withEndAction(() -> {
                                    boxShape.setVisibility(View.GONE);
                                    animateGradientBackground();
                                    logoText.setVisibility(View.VISIBLE);
                                    animateTypingText("Vipayee");
                                });
                    }, 400);
                });
            });

        }, 300);
    }

    private void animateTypingText(String fullText) {
        final int[] index = {0};
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                logoText.setText(fullText.substring(0, index[0] + 1));
                index[0]++;
                if (index[0] < fullText.length()) {
                    handler.postDelayed(this, 150);
                }
            }
        };
        handler.post(runnable);
    }

    private void animateGradientBackground() {

        int[] colors = {0xFFFFA500, 0xFFFF4500}; // Orange → Dark Orange
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, colors);
        splashRoot.setBackground(gradient);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            getWindow().setStatusBarColor(Color.parseColor("#FFA500"));

            new Handler(Looper.getMainLooper()).postDelayed(() -> {

                SessionManager session = new SessionManager(SplashActivity.this);
                Intent intent;

                if (!session.isRegistered()) {
                    intent = new Intent(SplashActivity.this, RegisterActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, MpinActivity.class);
                }

                startActivity(intent);
                finish();

            }, 1000);

        }, 400);
    }
}
