package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ClassDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_detail);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        Button btnDownload = findViewById(R.id.btnDownloadManual);
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v ->
                    Toast.makeText(this, "Lab Manual downloading...", Toast.LENGTH_SHORT).show());
        }

        android.view.View btnMaterials = findViewById(R.id.btnCourseMaterials);
        if (btnMaterials != null) {
            btnMaterials.setOnClickListener(v -> {
                startActivity(new Intent(this, CourseMaterialsActivity.class));
                overridePendingTransition(0, 0);
            });
        }
    }
}
