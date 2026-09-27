package com.mulungushi.campuscompanionapp; // TODO: replace with your actual package name

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mulungushi.campuscompanionapp.ForgotPasswordActivity;
import com.mulungushi.campuscompanionapp.MainActivity;
import com.mulungushi.campuscompanionapp.R;
import com.mulungushi.campuscompanionapp.RegisterActivity;
public class LoginActivity extends AppCompatActivity {

    private EditText etStudentNumber, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etStudentNumber = findViewById(R.id.etStudentNumber);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvSignUpLink = findViewById(R.id.tvSignUpLink);

        btnLogin.setOnClickListener(v -> attemptLogin());

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
        });
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
}