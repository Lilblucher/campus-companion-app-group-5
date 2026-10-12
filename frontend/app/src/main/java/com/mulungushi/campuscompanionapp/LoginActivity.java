package com.mulungushi.campuscompanionapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.AuthResponse;
import com.mulungushi.campuscompanionapp.network.LoginRequest;
import com.mulungushi.campuscompanionapp.network.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etStudentNumber, etPassword;
    private View errorRow;
    private TextView tvError;
    // Role segmented control (a routing hint; the server's AuthResponse role still wins)
    private String selectedRole = "student";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etStudentNumber = findViewById(R.id.etStudentId);
        etPassword = findViewById(R.id.etPassword);
        errorRow = findViewById(R.id.errorRow);
        tvError = findViewById(R.id.tvError);

        Button btnLogin = findViewById(R.id.btnLogin);
        if (btnLogin != null) btnLogin.setOnClickListener(v -> attemptLogin());

        View btnRegister = findViewById(R.id.btnRegister);
        if (btnRegister != null) btnRegister.setOnClickListener(v -> navigateToRegister());

        View tvForgotPassword = findViewById(R.id.tvForgotPassword);
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v ->
                    startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Role segmented control
        View roleStudent = findViewById(R.id.roleStudent);
        View roleLecturer = findViewById(R.id.roleLecturer);
        if (roleStudent != null) roleStudent.setOnClickListener(v -> selectRole("student"));
        if (roleLecturer != null) roleLecturer.setOnClickListener(v -> selectRole("lecturer"));
        selectRole("student");

        // Social sign-in (not wired to any provider yet)
        View.OnClickListener socialStub = v ->
                Toast.makeText(this, R.string.social_login_coming_soon, Toast.LENGTH_SHORT).show();
        View btnGoogle = findViewById(R.id.btnGoogle);
        View btnFacebook = findViewById(R.id.btnFacebook);
        if (btnGoogle != null) btnGoogle.setOnClickListener(socialStub);
        if (btnFacebook != null) btnFacebook.setOnClickListener(socialStub);
    }

    private void selectRole(String role) {
        selectedRole = role;
        boolean student = "student".equals(role);
        LinearLayout rs = findViewById(R.id.roleStudent);
        LinearLayout rl = findViewById(R.id.roleLecturer);
        ImageView icS = findViewById(R.id.icRoleStudent);
        ImageView icL = findViewById(R.id.icRoleLecturer);
        TextView tS = findViewById(R.id.tvRoleStudent);
        TextView tL = findViewById(R.id.tvRoleLecturer);
        int navy = ContextCompat.getColor(this, R.color.cc_navy);
        int muted = ContextCompat.getColor(this, R.color.cc_text_muted);

        if (rs != null) rs.setBackgroundResource(student ? R.drawable.cc_segment_thumb : 0);
        if (rl != null) rl.setBackgroundResource(student ? 0 : R.drawable.cc_segment_thumb);
        if (icS != null) icS.setColorFilter(student ? navy : muted);
        if (icL != null) icL.setColorFilter(student ? muted : navy);
        if (tS != null) { tS.setTextColor(student ? navy : muted); tS.setTypeface(null, student ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL); }
        if (tL != null) { tL.setTextColor(student ? muted : navy); tL.setTypeface(null, student ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD); }
    }

    private void showError(String msg) {
        if (tvError != null) tvError.setText(msg);
        if (errorRow != null) errorRow.setVisibility(View.VISIBLE);
    }

    private void clearError() {
        if (errorRow != null) errorRow.setVisibility(View.GONE);
    }

    private void navigateToRegister() {
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
    }

    private void attemptLogin() {
        clearError();
        String studentNumber = etStudentNumber.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (studentNumber.isEmpty() || password.isEmpty()) {
            showError("Enter your credentials to continue");
            return;
        }

        // --- Demo / Stub Credentials Fallback ---
        if ("123456789".equals(studentNumber) || "210482910".equals(studentNumber)) {
            if ("password123".equals(password)) {
                saveSessionAndNavigate("student", "Kellys", studentNumber, "stub-jwt-token-student", false);
                return;
            }
        }
        if ("LEC100001".equalsIgnoreCase(studentNumber) || "DR100001".equalsIgnoreCase(studentNumber) || "lecturer".equalsIgnoreCase(studentNumber)) {
            if ("password123".equals(password)) {
                saveSessionAndNavigate("lecturer", "Dr. Mulenga", "LEC100001", "stub-jwt-token-lecturer", false);
                return;
            }
        }

        // --- Live Retrofit API Call ---
        LoginRequest req = new LoginRequest(studentNumber, password);
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);

        apiService.login(req).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    // Server role wins; fall back to the segmented-control hint only if absent.
                    String role = auth.getRole() != null ? auth.getRole() : selectedRole;
                    String name = auth.getName() != null ? auth.getName() : "User";
                    String studentId = auth.getStudentId() != null ? auth.getStudentId() : studentNumber;
                    String token = auth.getToken() != null ? auth.getToken() : "";

                    saveSessionAndNavigate(role, name, studentId, token, auth.isGroupLeader());
                } else {
                    showError(getString(R.string.error_wrong_credentials));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void saveSessionAndNavigate(String role, String userName, String studentId, String token, boolean isGroupLeader) {
        Toast.makeText(this, "Welcome, " + userName + "!", Toast.LENGTH_SHORT).show();

        SessionManager.getInstance(this).saveUserSession(role, userName, studentId, token, isGroupLeader);

        Intent intent;
        if ("lecturer".equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, LecturerDashboardActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
