package com.mulungushi.campuscompanionapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegistrationProgressActivity extends BaseActivity {

    private EditText etClaimCodeInput;
    private Button btnRedeemClaimCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_progress);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        etClaimCodeInput = findViewById(R.id.etClaimCodeInput);
        btnRedeemClaimCode = findViewById(R.id.btnRedeemClaimCode);

        if (btnRedeemClaimCode != null) {
            btnRedeemClaimCode.setOnClickListener(v -> attemptRedeemClaimCode());
        }
    }

    private void attemptRedeemClaimCode() {
        if (etClaimCodeInput == null) return;
        String code = etClaimCodeInput.getText().toString().trim();

        if (code.isEmpty()) {
            Toast.makeText(this, "Please enter a claim code", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Claim code " + code + " redeemed successfully!", Toast.LENGTH_LONG).show();
        etClaimCodeInput.setText("");
    }
}
