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
import com.mulungushi.campuscompanionapp.network.AuthResponse;
import com.mulungushi.campuscompanionapp.network.LoginRequest;
import com.mulungushi.campuscompanionapp.network.RegisterRequest;
import com.mulungushi.campuscompanionapp.network.RegisterResponse;
import com.mulungushi.campuscompanionapp.network.SessionManager;

import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private SharedPreferences draftPrefs;
    private TextInputLayout tilStudentName, tilStudentId, tilProgramme, tilYearOfStudy, tilLabGroup,
            tilPassword, tilConfirmPassword, tilPhone, tilEmail;
    private TextInputEditText etStudentName, etStudentId, etClaimCode, etPassword, etConfirmPassword,
            etPhone, etEmail;
    private AutoCompleteTextView actProgramme, actYearOfStudy, actLabGroup;
    private TextView tvPasswordStrength, tvPasswordMatch;
    private View strengthDot1, strengthDot2, strengthDot3;
    private View sameNameBanner;

    private static final String DRAFT_PREFS_NAME = "register_draft";
    private static final String KEY_DRAFT_NAME = "draft_name";
    private static final String KEY_DRAFT_ID = "draft_id";
    private static final String KEY_DRAFT_CLAIM_CODE = "draft_claim_code";
    private static final String KEY_DRAFT_PROGRAMME = "draft_programme";
    private static final String KEY_DRAFT_PHONE = "draft_phone";
    private static final String KEY_DRAFT_EMAIL = "draft_email";
    private static final String KEY_DRAFT_YEAR = "draft_year";
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

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
                overridePendingTransition(0, 0);
            });
        }

        View btnGoToLogin = findViewById(R.id.btnGoToLogin);
        if (btnGoToLogin != null) {
            btnGoToLogin.setOnClickListener(v -> {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            });
        }

        View btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        if (btnRegisterSubmit != null) {
            btnRegisterSubmit.setOnClickListener(v -> attemptRegister());
        }

        View btnSaveDraft = findViewById(R.id.btnSaveDraft);
        if (btnSaveDraft != null) {
            btnSaveDraft.setOnClickListener(v -> {
                saveDraft();
                Toast.makeText(this, "Registration draft saved on this device", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void bindViews() {
        tilStudentName = findViewById(R.id.tilStudentName);
        tilStudentId = findViewById(R.id.tilStudentId);
        tilProgramme = findViewById(R.id.tilProgramme);
        tilYearOfStudy = findViewById(R.id.tilYearOfStudy);
        tilLabGroup = findViewById(R.id.tilLabGroup);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        tilPhone = findViewById(R.id.tilPhone);
        tilEmail = findViewById(R.id.tilEmail);

        etStudentName = findViewById(R.id.etStudentName);
        etStudentId = findViewById(R.id.etStudentId);
        etClaimCode = findViewById(R.id.etClaimCode);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);

        actProgramme = findViewById(R.id.actProgramme);
        actYearOfStudy = findViewById(R.id.actYearOfStudy);
        actLabGroup = findViewById(R.id.actLabGroup);

        sameNameBanner = findViewById(R.id.sameNameBanner);

        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        tvPasswordMatch = findViewById(R.id.tvPasswordMatch);

        strengthDot1 = findViewById(R.id.strengthDot1);
        strengthDot2 = findViewById(R.id.strengthDot2);
        strengthDot3 = findViewById(R.id.strengthDot3);
    }

    private void setupDropdowns() {
        if (actProgramme != null) {
            ArrayAdapter<CharSequence> programmeAdapter = ArrayAdapter.createFromResource(
                    this, R.array.programme_options, android.R.layout.simple_list_item_1);
            actProgramme.setAdapter(programmeAdapter);
            actProgramme.setOnItemClickListener((parent, view, position, id) -> {
                if (tilProgramme != null) tilProgramme.setError(null);
            });
        }

        if (actYearOfStudy != null) {
            ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(
                    this, R.array.year_of_study_options, android.R.layout.simple_list_item_1);
            actYearOfStudy.setAdapter(yearAdapter);
            actYearOfStudy.setOnItemClickListener((parent, view, position, id) -> {
                if (tilYearOfStudy != null) tilYearOfStudy.setError(null);
                saveDraft();
            });
        }

        if (actLabGroup != null) {
            ArrayAdapter<CharSequence> labGroupAdapter = ArrayAdapter.createFromResource(
                    this, R.array.lab_group_options, android.R.layout.simple_list_item_1);
            actLabGroup.setAdapter(labGroupAdapter);
            actLabGroup.setOnItemClickListener((parent, view, position, id) -> {
                if (tilLabGroup != null) tilLabGroup.setError(null);
                saveDraft();
            });
        }
    }

    private void setupValidationWatchers() {
        if (etStudentName != null) {
            etStudentName.addTextChangedListener(simpleWatcher(() -> {
                if (tilStudentName != null) tilStudentName.setError(null);
                saveDraft();
            }));
        }

        if (etStudentId != null) {
            etStudentId.addTextChangedListener(simpleWatcher(() -> {
                String id = getText(etStudentId);
                if (tilStudentId != null) {
                    if (id.length() == 9) {
                        tilStudentId.setBoxStrokeColor(ContextCompat.getColor(this, R.color.color_valid_green));
                        tilStudentId.setError(null);
                    } else {
                        tilStudentId.setBoxStrokeColor(ContextCompat.getColor(this, R.color.color_border_blue));
                    }
                }
                saveDraft();
            }));
        }

        if (etClaimCode != null) {
            etClaimCode.addTextChangedListener(simpleWatcher(this::saveDraft));
        }

        if (etPassword != null) {
            etPassword.addTextChangedListener(simpleWatcher(() -> {
                if (tilPassword != null) tilPassword.setError(null);
                updatePasswordStrength(getText(etPassword));
                updatePasswordMatch();
            }));
        }

        if (etConfirmPassword != null) {
            etConfirmPassword.addTextChangedListener(simpleWatcher(() -> {
                if (tilConfirmPassword != null) tilConfirmPassword.setError(null);
                updatePasswordMatch();
            }));
        }

        if (etPhone != null) {
            etPhone.addTextChangedListener(simpleWatcher(() -> {
                if (tilPhone != null) tilPhone.setError(null);
                saveDraft();
            }));
        }

        if (etEmail != null) {
            etEmail.addTextChangedListener(simpleWatcher(() -> {
                if (tilEmail != null) tilEmail.setError(null);
                saveDraft();
            }));
        }
    }

    private void updatePasswordStrength(String password) {
        if (tvPasswordStrength == null) return;

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
        if (tvPasswordMatch == null) return;
        String password = getText(etPassword);
        String confirm = getText(etConfirmPassword);

        if (confirm.isEmpty()) {
            tvPasswordMatch.setVisibility(View.INVISIBLE);
            return;
        }

        tvPasswordMatch.setVisibility(View.VISIBLE);
        if (password.equals(confirm)) {
            tvPasswordMatch.setText(R.string.password_matched);
            tvPasswordMatch.setTextColor(ContextCompat.getColor(this, R.color.cc_success));
            tvPasswordMatch.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ms_check_circle, 0, 0, 0);
            tintMatchIcon(R.color.cc_success);
        } else {
            tvPasswordMatch.setText(R.string.password_not_matched);
            tvPasswordMatch.setTextColor(ContextCompat.getColor(this, R.color.cc_crimson_dark));
            tvPasswordMatch.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ms_cancel, 0, 0, 0);
            tintMatchIcon(R.color.cc_crimson_dark);
        }
    }

    private void tintMatchIcon(int colorRes) {
        if (tvPasswordMatch == null) return;
        android.graphics.drawable.Drawable[] d = tvPasswordMatch.getCompoundDrawables();
        if (d.length > 0 && d[0] != null) {
            d[0].mutate().setTint(ContextCompat.getColor(this, colorRes));
        }
    }

    private void attemptRegister() {
        String name = getText(etStudentName);
        String studentId = getText(etStudentId);
        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);
        String programme = actProgramme != null ? actProgramme.getText().toString().trim() : "";
        String yearOfStudyStr = actYearOfStudy != null ? actYearOfStudy.getText().toString().trim() : "";
        String labGroup = actLabGroup != null ? actLabGroup.getText().toString().trim() : "";
        String phone = getText(etPhone);
        String email = getText(etEmail);
        String claimCode = getText(etClaimCode);

        boolean isValid = true;

        if (name.length() < 2 || name.length() > 100) {
            if (tilStudentName != null) tilStudentName.setError(getString(R.string.error_name_length));
            isValid = false;
        }

        if (studentId.length() != 9) {
            if (tilStudentId != null) tilStudentId.setError(getString(R.string.error_student_id_length));
            isValid = false;
        }

        if (programme.isEmpty()) {
            if (tilProgramme != null) tilProgramme.setError(getString(R.string.error_programme_required));
            isValid = false;
        }

        if (yearOfStudyStr.isEmpty()) {
            if (tilYearOfStudy != null) tilYearOfStudy.setError("Select a year of study");
            isValid = false;
        }

        if (labGroup.isEmpty()) {
            if (tilLabGroup != null) tilLabGroup.setError(getString(R.string.error_lab_group_required));
            isValid = false;
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (tilEmail != null) tilEmail.setError(getString(R.string.error_email_invalid));
            isValid = false;
        }

        // Server requires a 7–15 digit phone (optionally +); match that here.
        if (!phone.replaceAll("[^0-9+]", "").matches("\\+?\\d{7,15}")) {
            if (tilPhone != null) tilPhone.setError("Enter a valid phone number");
            isValid = false;
        }

        if (password.isEmpty()) {
            if (tilPassword != null) tilPassword.setError(getString(R.string.error_password_required));
            isValid = false;
        } else if (password.length() < 8) {
            if (tilPassword != null) tilPassword.setError("Password must be at least 8 characters");
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            if (tilConfirmPassword != null) tilConfirmPassword.setError(getString(R.string.error_confirm_password_required));
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            if (tilConfirmPassword != null) tilConfirmPassword.setError(getString(R.string.password_not_matched));
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        btnRegisterSetEnabled(false);

        String programmeCode = "CS";
        String progLower = programme.toLowerCase();
        if (progLower.contains("it") || progLower.contains("information")) {
            programmeCode = "IT";
        } else if (progLower.contains("ds") || progLower.contains("data")) {
            programmeCode = "DS";
        } else {
            programmeCode = "CS";
        }

        String phoneNormalized = phone.replaceAll("[^0-9+]", "");

        // Parse "Year 1" -> 1, "Year 2" -> 2, etc.
        int yearOfStudy = 0;
        if (!yearOfStudyStr.isEmpty()) {
            try {
                yearOfStudy = Integer.parseInt(yearOfStudyStr.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) { }
        }

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        RegisterRequest request = new RegisterRequest(name, studentId, programmeCode, email, phoneNormalized, password, claimCode, yearOfStudy, labGroup);

        apiService.register(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // The register endpoint only confirms the account was created; it
                    // issues no token. Sign the student straight in with the same
                    // credentials so they land in the app with a real session instead
                    // of the old fake "stub-token" (which 401s on every real API call).
                    signInAfterRegister(studentId, password);
                    return;
                }

                btnRegisterSetEnabled(true);
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
                        "Couldn't reach the server. Check your connection and try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Register has no token of its own, so log in with the just-created credentials
     *  to get a real session, then go straight to joining a lab group. */
    private void signInAfterRegister(String studentId, String password) {
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.login(LoginRequest.forStudent(studentId, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                btnRegisterSetEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    String role = auth.getRole() != null ? auth.getRole() : "student";
                    String name = auth.getName() != null ? auth.getName() : getText(etStudentName);
                    String sid = auth.getStudentId() != null ? auth.getStudentId() : studentId;
                    String token = auth.getToken() != null ? auth.getToken() : "";

                    SessionManager.getInstance(RegisterActivity.this)
                            .saveUserSession(role, name, sid, token, auth.isGroupLeader());

                    Toast.makeText(RegisterActivity.this, "Account created. Let's join your lab group.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, MyGroupActivity.class));
                    finish();
                } else {
                    goToLoginAfterRegister();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                btnRegisterSetEnabled(true);
                goToLoginAfterRegister();
            }
        });
    }

    private void goToLoginAfterRegister() {
        Toast.makeText(RegisterActivity.this, "Account created. Please sign in.", Toast.LENGTH_LONG).show();
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        finish();
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
                .putString(KEY_DRAFT_YEAR, actYearOfStudy != null ? actYearOfStudy.getText().toString() : "")
                .putString(KEY_DRAFT_LAB_GROUP, actLabGroup != null ? actLabGroup.getText().toString() : "")
                .putString(KEY_DRAFT_PHONE, getText(etPhone))
                .putString(KEY_DRAFT_EMAIL, getText(etEmail))
                .apply();
    }

    private void restoreDraft() {
        if (draftPrefs == null) return;
        if (etStudentName != null) etStudentName.setText(draftPrefs.getString(KEY_DRAFT_NAME, ""));
        if (etStudentId != null) etStudentId.setText(draftPrefs.getString(KEY_DRAFT_ID, ""));
        if (etClaimCode != null) etClaimCode.setText(draftPrefs.getString(KEY_DRAFT_CLAIM_CODE, ""));
        if (actProgramme != null) actProgramme.setText(draftPrefs.getString(KEY_DRAFT_PROGRAMME, ""), false);
        if (actYearOfStudy != null) actYearOfStudy.setText(draftPrefs.getString(KEY_DRAFT_YEAR, ""), false);
        if (actLabGroup != null) actLabGroup.setText(draftPrefs.getString(KEY_DRAFT_LAB_GROUP, ""), false);
        if (etPhone != null) etPhone.setText(draftPrefs.getString(KEY_DRAFT_PHONE, ""));
        if (etEmail != null) etEmail.setText(draftPrefs.getString(KEY_DRAFT_EMAIL, ""));
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
