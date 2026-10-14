package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.mulungushi.campuscompanionapp.network.SessionManager;

import java.util.concurrent.Executor;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean dark = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE)
                .getBoolean(SettingsActivity.KEY_DARK, false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                dark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                     : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        // Edge-to-edge: photo runs behind status bar
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        Window w = getWindow();
        w.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        w.setNavigationBarColor(0xFF0F1F45);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            w.setDecorFitsSystemWindows(false);
        }

        // If already logged in, skip straight to dashboard
        if (SessionManager.getInstance(this).isLoggedIn()) {
            promptBiometricUnlock();
            return;
        }

        setContentView(R.layout.activity_splash);
        applyNavBarInset();

        findViewById(R.id.btnSignUp).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.btnSignIn).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(0, 0);
        });
    }

    private void applyNavBarInset() {
        View sheet = findViewById(R.id.bottomSheet);
        if (sheet != null) {
            sheet.setOnApplyWindowInsetsListener((v, insets) -> {
                int bottomInset = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bottomInset = insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom;
                } else {
                    bottomInset = insets.getSystemWindowInsetBottom();
                }
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                        v.getPaddingRight(),
                        (int) (48 * getResources().getDisplayMetrics().density) + bottomInset);
                return insets;
            });
            sheet.requestApplyInsets();
        }
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

        // Show the layout briefly while the biometric prompt is up
        setContentView(R.layout.activity_splash);
        applyNavBarInset();

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
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    private void goToDashboard() {
        String role = SessionManager.getInstance(this).getUserRole();
        Intent intent;
        if ("lecturer".equalsIgnoreCase(role)) {
            intent = new Intent(this, LecturerDashboardActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}
