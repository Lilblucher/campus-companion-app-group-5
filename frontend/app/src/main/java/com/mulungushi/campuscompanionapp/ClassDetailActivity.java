package com.mulungushi.campuscompanionapp;

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
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        Button btnDownload = findViewById(R.id.btnDownloadManual);
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v ->
                    Toast.makeText(this, "Lab Manual downloading...", Toast.LENGTH_SHORT).show());
        }
    }
}
