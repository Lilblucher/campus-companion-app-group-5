package com.mulungushi.campuscompanionapp;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.mulungushi.campuscompanionapp.network.SessionManager;

import java.util.concurrent.Executor;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1800;
    private static final String PREFS_NAME = "campus_companion_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ROLE = "user_role";

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        animateLoadingDots();

        View btnSignUp = findViewById(R.id.btnSignUp);
        if (btnSignUp != null) {
            btnSignUp.setOnClickListener(v -> {
                handler.removeCallbacksAndMessages(null);
                startActivity(new Intent(SplashActivity.this, RegisterActivity.class));
            });
        }

        View btnSignIn = findViewById(R.id.btnSignIn);
        if (btnSignIn != null) {
            btnSignIn.setOnClickListener(v -> {
                handler.removeCallbacksAndMessages(null);
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            });
        }

        handler.postDelayed(this::decideNextScreen, SPLASH_DELAY_MS);
    }

    private void animateLoadingDots() {
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        bounce(dot1, 0);
        bounce(dot2, 150);
        bounce(dot3, 300);
    }

    private void bounce(View dot, long startDelay) {
        if (dot == null) return;
        ObjectAnimator animator = ObjectAnimator.ofFloat(dot, "translationY", 0f, -16f);
        animator.setDuration(300);
        animator.setStartDelay(startDelay);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();
    }

    private void decideNextScreen() {
        boolean isLoggedIn = SessionManager.getInstance(this).isLoggedIn();

        if (!isLoggedIn) {
            // Stay on splash screen with interactive SIGN UP / SIGN IN buttons
            return;
        }

        promptBiometricUnlock();
    }

    private void promptBiometricUnlock() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            goToDashboard();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        goToDashboard();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(SplashActivity.this, errString, Toast.LENGTH_SHORT).show();
                        goToLogin();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Campus Companion")
                .setSubtitle("Confirm it's you to continue")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                                | BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void goToLogin() {
        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        finish();
    }

    private void goToDashboard() {
        String role = SessionManager.getInstance(this).getUserRole();

        Intent intent;
        if ("lecturer".equalsIgnoreCase(role)) {
            intent = new Intent(SplashActivity.this, LecturerDashboardActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
