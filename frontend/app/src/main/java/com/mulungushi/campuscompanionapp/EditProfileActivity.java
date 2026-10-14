package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class EditProfileActivity extends BaseActivity {

    private TextView segCS, segIT, segDS;
    private int selectedProgramme = 0; // 0=CS, 1=IT, 2=DS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Close (X) button
        findViewById(R.id.btnClose).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        // Bell
        findViewById(R.id.btnBell).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        // Pre-fill sample data
        TextInputEditText etName = findViewById(R.id.etName);
        TextInputEditText etNumber = findViewById(R.id.etNumber);
        etName.setText("Chileshe Mulenga");
        etNumber.setText("210482910");

        // Initials
        TextView tvInitials = findViewById(R.id.tvAvatarInitials);
        tvInitials.setText("CM");

        // Programme segments
        segCS = findViewById(R.id.segCS);
        segIT = findViewById(R.id.segIT);
        segDS = findViewById(R.id.segDS);

        segCS.setOnClickListener(v -> selectProgramme(0));
        segIT.setOnClickListener(v -> selectProgramme(1));
        segDS.setOnClickListener(v -> selectProgramme(2));
        selectProgramme(0);

        // Photo buttons (no-op for now)
        findViewById(R.id.btnCamera).setOnClickListener(v -> { /* future */ });
        findViewById(R.id.btnChoosePhoto).setOnClickListener(v -> { /* future */ });
        findViewById(R.id.btnRemovePhoto).setOnClickListener(v -> { /* future */ });

        // Save
        findViewById(R.id.btnSave).setOnClickListener(v ->
                Snackbar.make(v, "Profile saved", Snackbar.LENGTH_SHORT).show());
    }

    private void selectProgramme(int index) {
        selectedProgramme = index;
        TextView[] segs = {segCS, segIT, segDS};
        for (int i = 0; i < segs.length; i++) {
            if (i == index) {
                segs[i].setBackgroundResource(R.drawable.cc_segment_selected);
                segs[i].setTextColor(0xFFFFFFFF);
            } else {
                segs[i].setBackgroundResource(R.drawable.cc_segment_unselected);
                segs[i].setTextColor(getColor(R.color.cc_text_secondary));
            }
        }
    }
}
