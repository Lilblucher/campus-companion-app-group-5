package com.mulungushi.campuscompanionapp;

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
import com.mulungushi.campuscompanionapp.network.ApiBodies;
import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.MessageResponse;

import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

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

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
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

        btnGetCode.setEnabled(false);
        btnResendCode.setEnabled(false);
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.forgotPassword(new ApiBodies.ForgotPassword(email)).enqueue(new Callback<MessageResponse>() {
            @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                btnGetCode.setEnabled(true);
                // The server always returns a generic success (it never reveals whether
                // the email is registered), so advance to step 2 on any 2xx.
                if (r.isSuccessful()) {
                    verifiedEmail = email;
                    if (findViewById(R.id.tvCodeSentMessage).getVisibility() != android.view.View.VISIBLE) {
                        showStep2(email);
                    }
                    startResendCooldown();
                } else {
                    btnResendCode.setEnabled(true);
                    tilEmail.setError("Couldn't send a code right now. Try again.");
                }
            }
            @Override public void onFailure(Call<MessageResponse> c, Throwable t) {
                btnGetCode.setEnabled(true);
                btnResendCode.setEnabled(true);
                tilEmail.setError("Network error. Check your connection.");
            }
        });
    }

    private void showStep2(String email) {
        tvHeading.setText(R.string.enter_reset_code_heading);
        tilEmail.setEnabled(false);
        btnGetCode.setVisibility(View.GONE);

        // Advance the 2-step indicator
        TextView stepEmail = findViewById(R.id.stepEmail);
        TextView stepCode = findViewById(R.id.stepCode);
        if (stepEmail != null) {
            stepEmail.setBackgroundResource(R.drawable.cc_dot_navy);
        }
        if (stepCode != null) {
            stepCode.setBackgroundResource(R.drawable.cc_dot_navy);
            stepCode.setTextColor(ContextCompat.getColor(this, R.color.white));
        }

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

        final String email = verifiedEmail != null ? verifiedEmail : getText(etEmail);
        btnResetPassword.setEnabled(false);
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.resetPassword(new ApiBodies.ResetPassword(email, code, newPassword)).enqueue(new Callback<MessageResponse>() {
            @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                btnResetPassword.setEnabled(true);
                if (r.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Password updated. Please sign in.", Toast.LENGTH_LONG).show();
                    Intent i = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    finish();
                } else {
                    String msg = parseError(r);
                    if (msg == null) msg = getString(R.string.error_code_invalid);
                    // The code is the most likely thing that's wrong/expired.
                    tilResetCode.setError(msg);
                }
            }
            @Override public void onFailure(Call<MessageResponse> c, Throwable t) {
                btnResetPassword.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Network error. Check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Pull the {"error": "..."} message out of a non-2xx response body. */
    private String parseError(Response<MessageResponse> r) {
        try {
            if (r.errorBody() == null) return null;
            MessageResponse m = new com.google.gson.Gson()
                    .fromJson(r.errorBody().charStream(), MessageResponse.class);
            return m != null ? m.error : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ---- Password strength / match (mirrors RegisterActivity's logic) ----

    private void updatePasswordStrength(String password) {
        int empty = ContextCompat.getColor(this, R.color.cc_skeleton_block);
        if (password.isEmpty()) {
            tvPasswordStrength.setVisibility(View.INVISIBLE);
            fillSegment(strengthDot1, empty);
            fillSegment(strengthDot2, empty);
            fillSegment(strengthDot3, empty);
            return;
        }

        int score = 0;
        if (password.length() >= 8) score++;
        if (UPPERCASE.matcher(password).matches() && LOWERCASE.matcher(password).matches()) score++;
        if (DIGIT.matcher(password).matches()) score++;
        if (SYMBOL.matcher(password).matches()) score++;

        tvPasswordStrength.setVisibility(View.VISIBLE);

        int red = ContextCompat.getColor(this, R.color.cc_crimson);
        int amber = ContextCompat.getColor(this, R.color.cc_warning_strong);
        int green = ContextCompat.getColor(this, R.color.cc_success);

        if (score <= 1) {
            tvPasswordStrength.setText("Weak");
            tvPasswordStrength.setTextColor(red);
            fillSegment(strengthDot1, red);
            fillSegment(strengthDot2, empty);
            fillSegment(strengthDot3, empty);
        } else if (score <= 3) {
            tvPasswordStrength.setText("Fair");
            tvPasswordStrength.setTextColor(amber);
            fillSegment(strengthDot1, amber);
            fillSegment(strengthDot2, amber);
            fillSegment(strengthDot3, empty);
        } else {
            tvPasswordStrength.setText("Strong");
            tvPasswordStrength.setTextColor(green);
            fillSegment(strengthDot1, green);
            fillSegment(strengthDot2, green);
            fillSegment(strengthDot3, green);
        }
    }

    private void fillSegment(View segment, int color) {
        if (segment == null) return;
        segment.setBackgroundResource(R.drawable.cc_skel_line);
        segment.getBackground().mutate().setTint(color);
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
            tvPasswordMatch.setTextColor(ContextCompat.getColor(this, R.color.cc_success));
            tvPasswordMatch.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ms_check_circle, 0, 0, 0);
            if (tvPasswordMatch.getCompoundDrawables()[0] != null)
                tvPasswordMatch.getCompoundDrawables()[0].mutate().setTint(ContextCompat.getColor(this, R.color.cc_success));
        } else {
            tvPasswordMatch.setText(R.string.password_not_matched);
            tvPasswordMatch.setTextColor(ContextCompat.getColor(this, R.color.cc_crimson_dark));
            tvPasswordMatch.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ms_cancel, 0, 0, 0);
            if (tvPasswordMatch.getCompoundDrawables()[0] != null)
                tvPasswordMatch.getCompoundDrawables()[0].mutate().setTint(ContextCompat.getColor(this, R.color.cc_crimson_dark));
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
