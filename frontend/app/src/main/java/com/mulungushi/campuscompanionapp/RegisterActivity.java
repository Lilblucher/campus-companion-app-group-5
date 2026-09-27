package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.RegisterRequest;
import com.mulungushi.campuscompanionapp.network.RegisterResponse;

import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private SharedPreferences draftPrefs;
    private TextInputLayout tilStudentName, tilStudentId, tilProgramme, tilLabGroup, tilPassword, tilConfirmPassword;
    private TextInputEditText etStudentName, etStudentId, etClaimCode, etPassword, etConfirmPassword;
    private AutoCompleteTextView actProgramme, actLabGroup;
    private TextView tvPasswordStrength, tvPasswordMatch;
    private View strengthDot1, strengthDot2, strengthDot3;

    private static final String DRAFT_PREFS_NAME = "register_draft";
    private static final String KEY_DRAFT_NAME = "draft_name";
    private static final String KEY_DRAFT_ID = "draft_id";
    private static final String KEY_DRAFT_CLAIM_CODE = "draft_claim_code";
    private static final String KEY_DRAFT_PROGRAMME = "draft_programme";
    private static final String KEY_DRAFT_LAB_GROUP = "draft_lab_group";

    private static final Pattern UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern SYMBOL = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

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
        if (etStudentName != null) {
            etStudentName.addTextChangedListener(simpleWatcher(() -> {
                tilStudentName.setError(null);
                saveDraft();
            }));
        }

        if (etStudentId != null) {
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
        }

        if (etClaimCode != null) {
            etClaimCode.addTextChangedListener(simpleWatcher(this::saveDraft));
        }

        if (etPassword != null) {
            etPassword.addTextChangedListener(simpleWatcher(() -> {
                tilPassword.setError(null);
                updatePasswordStrength(getText(etPassword));
                updatePasswordMatch();
            }));
        }

        if (etConfirmPassword != null) {
            etConfirmPassword.addTextChangedListener(simpleWatcher(() -> {
                tilConfirmPassword.setError(null);
                updatePasswordMatch();
            }));
        }
    }

    /**
     * Scores password strength from 0-4 based on length + mixed case + digits + symbols,
     * then maps that to a Weak / Fair / Strong tier (1 / 2 / 3 dots filled).
     */
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
        if (dot != null) {
            dot.setBackgroundResource(R.drawable.shape_loading_dot);
            dot.setAlpha(filled ? 1f : 0.3f);
        }
    }

    private void updatePasswordMatch() {
        String password = getText(etPassword);
        String confirm = getText(etConfirmPassword);

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

    private void attemptRegister() {
        String name = getText(etStudentName);
        String studentId = getText(etStudentId);
        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);
        String programme = actProgramme.getText().toString().trim();
        String labGroup = actLabGroup.getText().toString().trim();

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
        } else if (password.length() < 8) {
            tilPassword.setError("Password must be at least 8 characters");
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

        btnRegisterSetEnabled(false);

        String programmeCode = "CS";
        if (programme.equalsIgnoreCase("Information Technology") || programme.equalsIgnoreCase("IT")) {
            programmeCode = "IT";
        } else if (programme.equalsIgnoreCase("Data Science") || programme.equalsIgnoreCase("DS")) {
            programmeCode = "DS";
        } else if (programme.equalsIgnoreCase("Computer Science") || programme.equalsIgnoreCase("CS")) {
            programmeCode = "CS";
        }

        String email = studentId + "@student.mulungushi.ac.zm";
        String phone = "0000000000";

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        RegisterRequest request = new RegisterRequest(name, studentId, programmeCode, email, phone, password);

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
        Button btnRegister = findViewById(R.id.btnRegisterSubmit);
        if (btnRegister != null) {
            btnRegister.setEnabled(enabled);
        }
    }

    // ---- Draft persistence ----

    private void saveDraft() {
        if (draftPrefs == null) return;
        draftPrefs.edit()
                .putString(KEY_DRAFT_NAME, getText(etStudentName))
                .putString(KEY_DRAFT_ID, getText(etStudentId))
                .putString(KEY_DRAFT_CLAIM_CODE, getText(etClaimCode))
                .putString(KEY_DRAFT_PROGRAMME, actProgramme != null ? actProgramme.getText().toString() : "")
                .putString(KEY_DRAFT_LAB_GROUP, actLabGroup != null ? actLabGroup.getText().toString() : "")
                .apply();
    }

    private void restoreDraft() {
        if (draftPrefs == null) return;
        if (etStudentName != null) etStudentName.setText(draftPrefs.getString(KEY_DRAFT_NAME, ""));
        if (etStudentId != null) etStudentId.setText(draftPrefs.getString(KEY_DRAFT_ID, ""));
        if (etClaimCode != null) etClaimCode.setText(draftPrefs.getString(KEY_DRAFT_CLAIM_CODE, ""));
        if (actProgramme != null) actProgramme.setText(draftPrefs.getString(KEY_DRAFT_PROGRAMME, ""), false);
        if (actLabGroup != null) actLabGroup.setText(draftPrefs.getString(KEY_DRAFT_LAB_GROUP, ""), false);
    }

    // ---- Helpers ----

    private String getText(EditText editText) {
        return (editText == null || editText.getText() == null) ? "" : editText.getText().toString().trim();
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
