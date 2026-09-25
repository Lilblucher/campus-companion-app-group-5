package com.yourpackage.campuscompanion; // TODO: replace with your actual package name

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    // TODO: replace this whole draft mechanism with Room once Activity B/E's local
    // database exists. SharedPreferences is a temporary stand-in so typed input
    // survives rotation/app-kill today, per the brief's "preserve user work" requirement.
    private static final String DRAFT_PREFS_NAME = "registration_draft_prefs";
    private static final String KEY_DRAFT_NAME = "draft_student_name";
    private static final String KEY_DRAFT_ID = "draft_student_id";
    private static final String KEY_DRAFT_CLAIM_CODE = "draft_claim_code";
    private static final String KEY_DRAFT_PROGRAMME = "draft_programme";
    private static final String KEY_DRAFT_LAB_GROUP = "draft_lab_group";
    // Password fields are intentionally NOT persisted to the draft, even locally —
    // don't save passwords on the phone (per the brief's auth/permissions section).

    private static final Pattern UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*[0-9].*");
    private static final Pattern SYMBOL = Pattern.compile(".*[^A-Za-z0-9].*");

    private TextInputLayout tilStudentName, tilStudentId, tilProgramme, tilLabGroup,
            tilPassword, tilConfirmPassword;
    private TextInputEditText etStudentName, etStudentId, etClaimCode, etPassword, etConfirmPassword;
    private AutoCompleteTextView actProgramme, actLabGroup;
    private TextView tvPasswordStrength, tvPasswordMatch;
    private android.view.View strengthDot1, strengthDot2, strengthDot3;

    private SharedPreferences draftPrefs;

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
        String name = getText(etStudentName);
        String studentId = getText(etStudentId);
        String claimCode = getText(etClaimCode);
        String programme = actProgramme.getText().toString().trim();
        String labGroup = actLabGroup.getText().toString().trim();
        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);

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

        // TODO: replace with a real Retrofit call to POST /register, sending:
        // name, studentId, claimCode (nullable), programme, labGroup, password.
        // On success: clear the draft, then navigate to LoginActivity (or straight
        // to MainActivity if your backend returns a session token immediately).
        // On GROUP_FULL / duplicate student number: surface that as a field error
        // rather than a generic toast, per the brief's validation requirements.
        clearDraft();
        Toast.makeText(this, "Registration submitted (stub — no backend yet)", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        finish();
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
