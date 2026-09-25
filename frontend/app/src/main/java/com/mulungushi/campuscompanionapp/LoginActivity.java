package com.yourpackage.campuscompanion; // TODO: replace with your actual package name

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    // TODO: keep these in sync with SplashActivity's PREFS_NAME / KEY_IS_LOGGED_IN
    private static final String PREFS_NAME = "campus_companion_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    // TODO: remove once the real /login API call is wired up — for now this is the only
    // combination that succeeds, so the rest of the team can test navigation and error states.
    private static final String STUB_VALID_STUDENT_ID = "123456789";
    private static final String STUB_VALID_PASSWORD = "password123";

    private TextInputLayout tilStudentId;
    private TextInputLayout tilPassword;
    private TextInputEditText etStudentId;
    private TextInputEditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tilStudentId = findViewById(R.id.tilStudentId);
        tilPassword = findViewById(R.id.tilPassword);
        etStudentId = findViewById(R.id.etStudentId);
        etPassword = findViewById(R.id.etPassword);

        clearErrorsOnEdit(etStudentId, tilStudentId);
        clearErrorsOnEdit(etPassword, tilPassword);

        findViewById(R.id.btnLogin).setOnClickListener(v -> attemptLogin());

        findViewById(R.id.btnRegister).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        findViewById(R.id.tvForgotPassword).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        findViewById(R.id.btnFacebookLogin).setOnClickListener(v -> showSocialLoginPlaceholder());
        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> showSocialLoginPlaceholder());
    }

    private void showSocialLoginPlaceholder() {
        // TODO: wire up real Facebook/Google SDK sign-in once console setup is done.
        Toast.makeText(this, R.string.social_login_coming_soon, Toast.LENGTH_SHORT).show();
    }

    private void attemptLogin() {
        String studentId = getText(etStudentId);
        String password = getText(etPassword);

        boolean isValid = true;

        if (studentId.length() != 9) {
            tilStudentId.setError(getString(R.string.student_number));
            isValid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.password));
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // TODO: replace this stubbed check with a real Retrofit call to POST /login,
        // e.g. authApi.login(studentId, password).enqueue(...)
        if (studentId.equals(STUB_VALID_STUDENT_ID) && password.equals(STUB_VALID_PASSWORD)) {
            setLoggedIn();
            goToMain();
        } else {
            showWrongCredentials();
        }
    }

    private void showWrongCredentials() {
        // Space as the Student ID field's error just tints its border red without
        // duplicating the message shown under the Password field.
        tilStudentId.setError(" ");
        tilPassword.setError(getString(R.string.error_wrong_credentials));
    }

    private void clearErrorsOnEdit(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void setLoggedIn() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply();
    }

    private void goToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
