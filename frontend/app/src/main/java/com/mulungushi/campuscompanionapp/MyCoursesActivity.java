package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MyCoursesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_courses);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        View.OnClickListener openDetail = v -> {
            startActivity(new Intent(this, ClassDetailActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        };
        int[] cards = {R.id.cardCourse1, R.id.cardCourse2, R.id.cardCourse3};
        for (int id : cards) {
            View c = findViewById(id);
            if (c != null) c.setOnClickListener(openDetail);
        }
    }
}
