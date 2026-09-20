package com.mulungushi.campuscompanionapp; // adjust to your actual package

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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

        tvSignUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String studentNumber = etStudentNumber.getText().toString().trim();
        String password = etPassword.getText().toString();

        // TODO: hand off to backend/architecture team's auth logic (Retrofit call, validation, etc.)
        // Placeholder UI-only check for now:
        if (studentNumber.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Temporary: just navigate forward so the flow is testable
      //  Intent intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
       // startActivity(intent);
    }
}