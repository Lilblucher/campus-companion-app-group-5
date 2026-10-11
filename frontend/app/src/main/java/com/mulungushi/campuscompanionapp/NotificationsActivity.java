package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

public class NotificationsActivity extends AppCompatActivity {

    private View navDashboard, navGroup, navProfile, navAlerts, navSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        setupBottomNav();
    }

    private void setupBottomNav() {
        navDashboard = findViewById(R.id.navDashboard);
        navGroup = findViewById(R.id.navGroup);
        navProfile = findViewById(R.id.navProfile);
        navAlerts = findViewById(R.id.navAlerts);
        navSettings = findViewById(R.id.navSettings);

        if (navDashboard != null) navDashboard.setOnClickListener(v -> navigateTo(MainActivity.class));
        if (navGroup != null) navGroup.setOnClickListener(v -> navigateTo(MyGroupActivity.class));
        if (navProfile != null) navProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));
        if (navAlerts != null) selectNav(navAlerts);
        if (navSettings != null) navSettings.setOnClickListener(v -> navigateTo(SettingsActivity.class));

        selectNav(navAlerts);
    }

    private void navigateTo(Class<?> targetActivity) {
        startActivity(new Intent(this, targetActivity));
        finish();
    }

    private void selectNav(View selected) {
        View[] allNavItems = {navDashboard, navGroup, navProfile, navAlerts, navSettings};
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
