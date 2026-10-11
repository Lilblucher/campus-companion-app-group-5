package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanionapp.network.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "campus_companion_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private View navDashboard, navGroup, navProfile, navAlerts, navSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
        findViewById(R.id.btnSignOut).setOnClickListener(v -> showSignOutDialog());

        setupBottomNav();
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    SessionManager.getInstance(this).clearSession();

                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setupBottomNav() {
        navDashboard = findViewById(R.id.navDashboard);
        navGroup = findViewById(R.id.navGroup);
        navProfile = findViewById(R.id.navProfile);
        navAlerts = findViewById(R.id.navAlerts);
        navSettings = findViewById(R.id.navSettings);

        if (navDashboard != null) navDashboard.setOnClickListener(v -> navigateTo(MainActivity.class));
        if (navGroup != null) navGroup.setOnClickListener(v -> navigateTo(MyGroupActivity.class));
        if (navProfile != null) selectNav(navProfile);
        if (navAlerts != null) navAlerts.setOnClickListener(v -> navigateTo(NotificationsActivity.class));
        if (navSettings != null) navSettings.setOnClickListener(v -> navigateTo(SettingsActivity.class));

        selectNav(navProfile);
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
