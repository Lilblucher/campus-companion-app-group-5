package com.yourpackage.campuscompanion; // TODO: replace with your actual package name

import android.content.Intent;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

public class ForgotPasswordActivity extends AppCompatActivity {

    // TODO: remove once the real /forgot-password and /verify-reset-code endpoints exist.
    // For now, any well-formed email "succeeds" except this one, which demonstrates the
    // account-not-found error path; the only code that verifies successfully is 123456.
    private static final String STUB_EMAIL_NOT_FOUND = "notfound@test.com";
    private static final String STUB_VALID_CODE = "123456";
    private static final int RESEND_COOLDOWN_SECONDS = 30;

    private static final Pattern UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*[0-9].*");
    private static final Pattern SYMBOL = Pattern.compile(".*[^A-Za-z0-9].*");

    private TextInputLayout tilEmail, tilResetCode, tilNewPassword, tilConfirmNewPassword;
    private TextInputEditText etEmail, etResetCode, etNewPassword, etConfirmNewPassword;
    private TextView tvHeading, tvCodeSentMessage, tvPasswordStrength, tvPasswordMatch;
    private Button btnGetCode, btnResetPassword;
    private TextView btnResendCode;
    private View strengthRow, strengthDot1, strengthDot2, strengthDot3;

    private String verifiedEmail;
    private CountDownTimer resendCountdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        bindViews();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnGetCode.setOnClickListener(v -> attemptSendCode());
        btnResendCode.setOnClickListener(v -> attemptSendCode());
        btnResetPassword.setOnClickListener(v -> attemptResetPassword());

        etEmail.addTextChangedListener(clearErrorWatcher(tilEmail));
        etResetCode.addTextChangedListener(clearErrorWatcher(tilResetCode));

        etNewPassword.addTextChangedListener(simpleWatcher(() -> {
            tilNewPassword.setError(null);
            updatePasswordStrength(getText(etNewPassword));
            updatePasswordMatch();
        }));

        etConfirmNewPassword.addTextChangedListener(simpleWatcher(() -> {
            tilConfirmNewPassword.setError(null);
            updatePasswordMatch();
        }));
    }

    private void bindViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilResetCode = findViewById(R.id.tilResetCode);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmNewPassword = findViewById(R.id.tilConfirmNewPassword);

        etEmail = findViewById(R.id.etEmail);
        etResetCode = findViewById(R.id.etResetCode);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);

        tvHeading = findViewById(R.id.tvHeading);
        tvCodeSentMessage = findViewById(R.id.tvCodeSentMessage);
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        tvPasswordMatch = findViewById(R.id.tvPasswordMatch);

        btnGetCode = findViewById(R.id.btnGetCode);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnResendCode = findViewById(R.id.btnResendCode);

        strengthRow = findViewById(R.id.strengthRow);
        strengthDot1 = findViewById(R.id.strengthDot1);
        strengthDot2 = findViewById(R.id.strengthDot2);
        strengthDot3 = findViewById(R.id.strengthDot3);
    }

    // ---- Step 1: request code ----

    private void attemptSendCode() {
        String email = getText(etEmail);

        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_email_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            return;
        }

        // TODO: replace with a real Retrofit call to POST /forgot-password { email }.
        // Regardless of whether the email exists, many real systems return a generic
        // success response to avoid leaking which emails are registered — decide with
        // your backend team whether to follow that pattern or show a direct error as below.
        if (email.equalsIgnoreCase(STUB_EMAIL_NOT_FOUND)) {
            tilEmail.setError(getString(R.string.error_email_not_found));
            return;
        }

        verifiedEmail = email;
        showStep2(email);
        startResendCooldown();
    }

    private void showStep2(String email) {
        tvHeading.setText(R.string.enter_reset_code_heading);
        tilEmail.setEnabled(false);
        btnGetCode.setVisibility(View.GONE);

        tvCodeSentMessage.setText(getString(R.string.code_sent_message, email));
        tvCodeSentMessage.setVisibility(View.VISIBLE);
        tilResetCode.setVisibility(View.VISIBLE);
        btnResendCode.setVisibility(View.VISIBLE);
        tilNewPassword.setVisibility(View.VISIBLE);
        strengthRow.setVisibility(View.VISIBLE);
        tilConfirmNewPassword.setVisibility(View.VISIBLE);
        btnResetPassword.setVisibility(View.VISIBLE);
    }

    private void startResendCooldown() {
        btnResendCode.setEnabled(false);
        if (resendCountdown != null) {
            resendCountdown.cancel();
        }
        resendCountdown = new CountDownTimer(RESEND_COOLDOWN_SECONDS * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000L) + 1;
                btnResendCode.setText(getString(R.string.resend_code_in, secondsLeft));
            }

            @Override
            public void onFinish() {
                btnResendCode.setText(R.string.resend_code);
                btnResendCode.setEnabled(true);
            }
        };
        resendCountdown.start();
    }

    // ---- Step 2: verify code + set new password ----

    private void attemptResetPassword() {
        String code = getText(etResetCode);
        String newPassword = getText(etNewPassword);
        String confirmPassword = getText(etConfirmNewPassword);

        boolean isValid = true;

        if (code.isEmpty()) {
            tilResetCode.setError(getString(R.string.error_code_required));
            isValid = false;
        }

        if (newPassword.isEmpty()) {
            tilNewPassword.setError(getString(R.string.error_new_password_required));
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmNewPassword.setError(getString(R.string.error_confirm_password_required));
            isValid = false;
        } else if (!newPassword.equals(confirmPassword)) {
            tilConfirmNewPassword.setError(getString(R.string.password_not_matched));
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // TODO: replace with a real Retrofit call to POST /reset-password
        // { email: verifiedEmail, code, newPassword }.
        if (!code.equals(STUB_VALID_CODE)) {
            tilResetCode.setError(getString(R.string.error_code_invalid));
            return;
        }

        Toast.makeText(this, "Password reset (stub — no backend yet)", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
        finish();
    }

    // ---- Password strength / match (mirrors RegisterActivity's logic) ----

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            tvPasswordStrength.setVisibility(View.INVISIBLE);
            setDotFilled(strengthDot1, false);
            setDotFilled(strengthDot2, false);
            setDotFilled(strengthDot3, false);
            return;
        }

        int score = 0;
        if (password.length() >= 8) score++;
        if (UPPERCASE.matcher(password).matches() && LOWERCASE.matcher(password).matches()) score++;
        if (DIGIT.matcher(password).matches()) score++;
        if (SYMBOL.matcher(password).matches()) score++;

        tvPasswordStrength.setVisibility(View.VISIBLE);

        if (score <= 1) {
            tvPasswordStrength.setText(R.string.password_strength_weak);
            setDotFilled(strengthDot1, true);
            setDotFilled(strengthDot2, false);
            setDotFilled(strengthDot3, false);
        } else if (score <= 3) {
            tvPasswordStrength.setText(R.string.password_strength_fair);
            setDotFilled(strengthDot1, true);
            setDotFilled(strengthDot2, true);
            setDotFilled(strengthDot3, false);
        } else {
            tvPasswordStrength.setText(R.string.password_strength_strong);
            setDotFilled(strengthDot1, true);
            setDotFilled(strengthDot2, true);
            setDotFilled(strengthDot3, true);
        }
    }

    private void setDotFilled(View dot, boolean filled) {
        dot.setAlpha(filled ? 1f : 0.3f);
    }

    private void updatePasswordMatch() {
        String password = getText(etNewPassword);
        String confirm = getText(etConfirmNewPassword);

        if (confirm.isEmpty()) {
            tvPasswordMatch.setVisibility(View.INVISIBLE);
            return;
        }

        tvPasswordMatch.setVisibility(View.VISIBLE);
        if (password.equals(confirm)) {
            tvPasswordMatch.setText(R.string.password_matched);
            tvPasswordMatch.setTextColor(ContextCompat.getColor(this, R.color.color_success_text));
            tvPasswordMatch.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0);
        } else {
            tvPasswordMatch.setText(R.string.password_not_matched);
            tvPasswordMatch.setTextColor(ContextCompat.getColor(this, R.color.color_error_red));
            tvPasswordMatch.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    // ---- Helpers ----

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private TextWatcher clearErrorWatcher(TextInputLayout layout) {
        return simpleWatcher(() -> layout.setError(null));
    }

    private TextWatcher simpleWatcher(Runnable onChanged) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onChanged.run();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendCountdown != null) {
            resendCountdown.cancel();
        }
    }
}
