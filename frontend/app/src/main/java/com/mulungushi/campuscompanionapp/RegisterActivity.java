package com.yourpackage.campuscompanion; // TODO: replace with your actual package name

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.RegisterRequest;
import com.mulungushi.campuscompanionapp.network.RegisterResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etStudentName, etStudentNumber, etPhoneNumber, etEmail, etPassword, etConfirmPassword;
    private Spinner spProgramme;

    // Codes must match what authController.js's register() accepts exactly.
    private static final String[] PROGRAMME_CODES = {"CS", "IT", "DS"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        draftPrefs = getSharedPreferences(DRAFT_PREFS_NAME, MODE_PRIVATE);

        bindViews();
        setupDropdowns();
        setupValidationWatchers();
        restoreDraft();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGoToLogin).setOnClickListener(v ->
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
        findViewById(R.id.btnRegisterSubmit).setOnClickListener(v -> attemptRegister());
    }

    private void bindViews() {
        tilStudentName = findViewById(R.id.tilStudentName);
        tilStudentId = findViewById(R.id.tilStudentId);
        tilProgramme = findViewById(R.id.tilProgramme);
        tilLabGroup = findViewById(R.id.tilLabGroup);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etStudentName = findViewById(R.id.etStudentName);
        etStudentId = findViewById(R.id.etStudentId);
        etClaimCode = findViewById(R.id.etClaimCode);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spProgramme = findViewById(R.id.spProgramme);

        ArrayAdapter<String> programmeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, PROGRAMME_CODES);
        programmeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProgramme.setAdapter(programmeAdapter);

        actProgramme = findViewById(R.id.actProgramme);
        actLabGroup = findViewById(R.id.actLabGroup);

        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        tvPasswordMatch = findViewById(R.id.tvPasswordMatch);

        strengthDot1 = findViewById(R.id.strengthDot1);
        strengthDot2 = findViewById(R.id.strengthDot2);
        strengthDot3 = findViewById(R.id.strengthDot3);
    }

    private void setupDropdowns() {
        ArrayAdapter<CharSequence> programmeAdapter = ArrayAdapter.createFromResource(
                this, R.array.programme_options, android.R.layout.simple_list_item_1);
        actProgramme.setAdapter(programmeAdapter);
        actProgramme.setOnItemClickListener((parent, view, position, id) -> tilProgramme.setError(null));

        ArrayAdapter<CharSequence> labGroupAdapter = ArrayAdapter.createFromResource(
                this, R.array.lab_group_options, android.R.layout.simple_list_item_1);
        actLabGroup.setAdapter(labGroupAdapter);
        actLabGroup.setOnItemClickListener((parent, view, position, id) -> tilLabGroup.setError(null));
    }

    private void setupValidationWatchers() {
        etStudentName.addTextChangedListener(simpleWatcher(() -> {
            tilStudentName.setError(null);
            saveDraft();
        }));

        etStudentId.addTextChangedListener(simpleWatcher(() -> {
            String id = getText(etStudentId);
            if (id.length() == 9) {
                tilStudentId.setBoxStrokeColor(ContextCompat.getColor(this, R.color.color_valid_green));
                tilStudentId.setError(null);
            } else {
                tilStudentId.setBoxStrokeColor(ContextCompat.getColor(this, R.color.color_border_blue));
            }
            saveDraft();
        }));

        etClaimCode.addTextChangedListener(simpleWatcher(this::saveDraft));

        etPassword.addTextChangedListener(simpleWatcher(() -> {
            tilPassword.setError(null);
            updatePasswordStrength(getText(etPassword));
            updatePasswordMatch();
        }));

        etConfirmPassword.addTextChangedListener(simpleWatcher(() -> {
            tilConfirmPassword.setError(null);
            updatePasswordMatch();
        }));
    }

    /**
     * Scores password strength from 0-4 based on length + mixed case + digits + symbols,
     * then maps that to a Weak / Fair / Strong tier (1 / 2 / 3 dots filled).
     */
    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            tvPasswordStrength.setVisibility(android.view.View.INVISIBLE);
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

        tvPasswordStrength.setVisibility(android.view.View.VISIBLE);

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

    private void setDotFilled(android.view.View dot, boolean filled) {
        dot.setBackgroundResource(R.drawable.shape_loading_dot);
        dot.setAlpha(filled ? 1f : 0.3f);
    }

    private void updatePasswordMatch() {
        String password = getText(etPassword);
        String confirm = getText(etConfirmPassword);

        if (confirm.isEmpty()) {
            tvPasswordMatch.setVisibility(android.view.View.INVISIBLE);
            return;
        }

        tvPasswordMatch.setVisibility(android.view.View.VISIBLE);
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

    private void attemptRegister() {
        String name = etStudentName.getText().toString().trim();
        String studentNumber = etStudentNumber.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String programmeCode = (String) spProgramme.getSelectedItem();

        boolean isValid = true;

        if (name.length() < 2 || name.length() > 100) {
            tilStudentName.setError(getString(R.string.error_name_length));
            isValid = false;
        }

        if (studentId.length() != 9) {
            tilStudentId.setError(getString(R.string.error_student_id_length));
            isValid = false;
        }

        if (programme.isEmpty()) {
            tilProgramme.setError(getString(R.string.error_programme_required));
            isValid = false;
        }

        if (labGroup.isEmpty()) {
            tilLabGroup.setError(getString(R.string.error_lab_group_required));
            isValid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_password_required));
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError(getString(R.string.error_confirm_password_required));
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.password_not_matched));
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        if (studentNumber.length() != 9) {
            Toast.makeText(this, "Student number must be 9 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.matches("\\+?\\d{7,15}")) {
            Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegisterSetEnabled(false);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        RegisterRequest request = new RegisterRequest(name, studentNumber, programmeCode, email, phone, password);

        apiService.register(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                btnRegisterSetEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    String msg = response.body().getMessage();
                    Toast.makeText(RegisterActivity.this,
                            msg != null ? msg : "Registered successfully",
                            Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                    return;
                }

                String errorMsg = "Registration failed";
                if (response.errorBody() != null) {
                    try {
                        RegisterResponse errorResponse = new Gson().fromJson(
                                response.errorBody().charStream(), RegisterResponse.class);
                        if (errorResponse != null && errorResponse.getError() != null) {
                            errorMsg = errorResponse.getError();
                        }
                    } catch (Exception ignored) {
                        // fall back to default message
                    }
                }
                Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                btnRegisterSetEnabled(true);
                Toast.makeText(RegisterActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void btnRegisterSetEnabled(boolean enabled) {
        Button btnRegister = findViewById(R.id.btnRegister);
        if (btnRegister != null) {
            btnRegister.setEnabled(enabled);
        }
    }
    }

    // ---- Draft persistence (temporary SharedPreferences stand-in for Room) ----

    private void saveDraft() {
        draftPrefs.edit()
                .putString(KEY_DRAFT_NAME, getText(etStudentName))
                .putString(KEY_DRAFT_ID, getText(etStudentId))
                .putString(KEY_DRAFT_CLAIM_CODE, getText(etClaimCode))
                .putString(KEY_DRAFT_PROGRAMME, actProgramme.getText().toString())
                .putString(KEY_DRAFT_LAB_GROUP, actLabGroup.getText().toString())
                .apply();
    }

    private void restoreDraft() {
        etStudentName.setText(draftPrefs.getString(KEY_DRAFT_NAME, ""));
        etStudentId.setText(draftPrefs.getString(KEY_DRAFT_ID, ""));
        etClaimCode.setText(draftPrefs.getString(KEY_DRAFT_CLAIM_CODE, ""));
        actProgramme.setText(draftPrefs.getString(KEY_DRAFT_PROGRAMME, ""), false);
        actLabGroup.setText(draftPrefs.getString(KEY_DRAFT_LAB_GROUP, ""), false);
    }

    private void clearDraft() {
        draftPrefs.edit().clear().apply();
    }

    // ---- Helpers ----

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
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
}
