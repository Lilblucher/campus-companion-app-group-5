package com.mulungushi.campuscompanionapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.AuthResponse;
import com.mulungushi.campuscompanionapp.network.LoginRequest;

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
        TextView tvSignUpLink = findViewById(R.id.tvNoAccount);

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvSignUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnRegister).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        findViewById(R.id.tvForgotPassword).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        findViewById(R.id.btnFacebookLogin).setOnClickListener(v -> showSocialLoginPlaceholder());
        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> showSocialLoginPlaceholder());
    }

    private void attemptLogin() {
        String studentNumber = etStudentNumber.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (studentNumber.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        LoginRequest req = new LoginRequest(studentNumber, password);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.login(req).enqueue(new retrofit2.Callback<AuthResponse>() {
            @Override
            public void onResponse(retrofit2.Call<AuthResponse> call, retrofit2.Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(LoginActivity.this, "Welcome, " + response.body().getName(), Toast.LENGTH_SHORT).show();
                    // TODO: save token (SharedPreferences), then navigate to dashboard
                    // Intent intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                    // startActivity(intent);
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<AuthResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSocialLoginPlaceholder() {
        Toast.makeText(this, "Social login coming soon", Toast.LENGTH_SHORT).show();
    }
}