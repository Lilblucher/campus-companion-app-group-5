package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

public class MyCoursesActivity extends AppCompatActivity {

    private View navDashboard, navGroup, navProfile, navCourses, navSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_courses);

        findViewById(R.id.btnBell).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(0, 0);
        });

        View.OnClickListener openDetail = v -> {
            startActivity(new Intent(this, ClassDetailActivity.class));
            overridePendingTransition(0, 0);
        };
        int[] cards = {R.id.cardCourse1, R.id.cardCourse2, R.id.cardCourse3};
        for (int id : cards) {
            View c = findViewById(id);
            if (c != null) c.setOnClickListener(openDetail);
        }

        View btnMaterials = findViewById(R.id.btnViewMaterials);
        if (btnMaterials != null) {
            btnMaterials.setOnClickListener(v -> {
                startActivity(new Intent(this, CourseMaterialsActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.mulungushi.campuscompanionapp.util.NavBadge.refresh(this);
    }

    private void setupBottomNav() {
        navDashboard = findViewById(R.id.navDashboard);
        navGroup = findViewById(R.id.navGroup);
        navProfile = findViewById(R.id.navProfile);
        navCourses = findViewById(R.id.navCourses);
        navSettings = findViewById(R.id.navSettings);

        if (navDashboard != null) navDashboard.setOnClickListener(v -> navigateTo(MainActivity.class));
        if (navGroup != null) navGroup.setOnClickListener(v -> navigateTo(MyGroupActivity.class));
        if (navCourses != null) selectNav(navCourses);
        if (navProfile != null) navProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));
        if (navSettings != null) navSettings.setOnClickListener(v -> navigateTo(SettingsActivity.class));

        selectNav(navCourses);
    }

    private void navigateTo(Class<?> targetActivity) {
        startActivity(new Intent(this, targetActivity));
        overridePendingTransition(0, 0);
        finish();
    }

    private void selectNav(View selected) {
        View[] allNavItems = {navDashboard, navGroup, navCourses, navProfile, navSettings};
        for (View item : allNavItems) {
            if (item == null) continue;
            boolean isSelected = (item == selected);
            item.setSelected(isSelected);
            if (item instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) item;
                if (group.getChildCount() >= 2) {
                    View icon = group.getChildAt(0);
                    View label = group.getChildAt(1);
                    if (icon != null) icon.setSelected(isSelected);
                    if (label != null) label.setSelected(isSelected);
                }
            }
        }
    }
}
