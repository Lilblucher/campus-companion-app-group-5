package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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

        etStudentName = findViewById(R.id.etStudentName);
        etStudentNumber = findViewById(R.id.etStudentNumber);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spProgramme = findViewById(R.id.spProgramme);

        ArrayAdapter<String> programmeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, PROGRAMME_CODES);
        programmeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProgramme.setAdapter(programmeAdapter);

        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvSignInLink = findViewById(R.id.tvSignInLink);

        btnRegister.setOnClickListener(v -> attemptRegister());

        tvSignInLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegister() {
        String name = etStudentName.getText().toString().trim();
        String studentNumber = etStudentNumber.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String programmeCode = (String) spProgramme.getSelectedItem();

        if (name.isEmpty() || studentNumber.isEmpty() || phone.isEmpty()
                || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
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