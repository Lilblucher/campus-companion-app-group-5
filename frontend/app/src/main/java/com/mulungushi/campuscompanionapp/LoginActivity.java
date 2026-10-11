package com.mulungushi.campuscompanionapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etStudentNumber = findViewById(R.id.etStudentId);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> attemptLogin());
        }

        View btnRegister = findViewById(R.id.btnRegister);
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> navigateToRegister());
        }

        View tvForgotPassword = findViewById(R.id.tvForgotPassword);
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v ->
                    startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));
        }
    }

    private void navigateToRegister() {
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
    }

    private void attemptLogin() {
        String studentNumber = etStudentNumber.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (studentNumber.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Demo / Stub Credentials Fallback ---
        if ("123456789".equals(studentNumber) || "210482910".equals(studentNumber)) {
            if ("password123".equals(password)) {
                saveSessionAndNavigate("student", "Kellys", studentNumber, "stub-jwt-token-student");
                return;
            }
        }
        if ("LEC100001".equalsIgnoreCase(studentNumber) || "DR100001".equalsIgnoreCase(studentNumber) || "lecturer".equalsIgnoreCase(studentNumber)) {
            if ("password123".equals(password)) {
                saveSessionAndNavigate("lecturer", "Dr. Mulenga", "LEC100001", "stub-jwt-token-lecturer");
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
                    String role = auth.getRole() != null ? auth.getRole() : "student";
                    String name = auth.getName() != null ? auth.getName() : "User";
                    String studentId = auth.getStudentId() != null ? auth.getStudentId() : studentNumber;
                    String token = auth.getToken() != null ? auth.getToken() : "";

                    saveSessionAndNavigate(role, name, studentId, token);
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed: Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveSessionAndNavigate(String role, String userName, String studentId, String token) {
        Toast.makeText(this, "Welcome, " + userName + "!", Toast.LENGTH_SHORT).show();

        // Encrypted Session Persistence
        SessionManager.getInstance(this).saveUserSession(role, userName, studentId, token);

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
