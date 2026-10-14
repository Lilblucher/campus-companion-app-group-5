package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends BaseActivity {

    public static final String PREFS = "campus_companion_prefs";
    public static final String KEY_BIOMETRIC = "pref_biometric";
    public static final String KEY_NOTIFICATIONS = "pref_notifications";
    public static final String KEY_DARK = "pref_dark_theme";
    public static final String KEY_OFFLINE = "pref_offline_sync";

    private View navDashboard, navGroup, navCourses, navProfile, navSettings;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        setupSwitches();
        setupBottomNav();
    }

    private void setupSwitches() {
        MaterialSwitch bio = findViewById(R.id.switchBiometrics);
        MaterialSwitch notif = findViewById(R.id.switchNotifications);
        MaterialSwitch dark = findViewById(R.id.switchDarkTheme);
        MaterialSwitch offline = findViewById(R.id.switchOfflineSync);

        if (bio != null) {
            bio.setChecked(prefs.getBoolean(KEY_BIOMETRIC, false));
            bio.setOnCheckedChangeListener((b, c) -> prefs.edit().putBoolean(KEY_BIOMETRIC, c).apply());
        }
        if (notif != null) {
            notif.setChecked(prefs.getBoolean(KEY_NOTIFICATIONS, true));
            notif.setOnCheckedChangeListener((b, c) -> prefs.edit().putBoolean(KEY_NOTIFICATIONS, c).apply());
        }
        if (offline != null) {
            offline.setChecked(prefs.getBoolean(KEY_OFFLINE, true));
            offline.setOnCheckedChangeListener((b, c) -> prefs.edit().putBoolean(KEY_OFFLINE, c).apply());
        }
        if (dark != null) {
            dark.setChecked(prefs.getBoolean(KEY_DARK, false));
            dark.setOnCheckedChangeListener((b, c) -> {
                prefs.edit().putBoolean(KEY_DARK, c).apply();
                AppCompatDelegate.setDefaultNightMode(
                        c ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            });
        }

        View rowSyncNow = findViewById(R.id.rowSyncNow);
        if (rowSyncNow != null) {
            rowSyncNow.setOnClickListener(v -> {
                View tv = findViewById(R.id.tvLastSync);
                if (tv instanceof android.widget.TextView) ((android.widget.TextView) tv).setText("Last synced just now");
                Toast.makeText(this, "Syncing…", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupBottomNav() {
        navDashboard = findViewById(R.id.navDashboard);
        navGroup = findViewById(R.id.navGroup);
        navCourses = findViewById(R.id.navCourses);
        navProfile = findViewById(R.id.navProfile);
        navSettings = findViewById(R.id.navSettings);

        if (navDashboard != null) navDashboard.setOnClickListener(v -> navigateTo(MainActivity.class));
        if (navGroup != null) navGroup.setOnClickListener(v -> navigateTo(MyGroupActivity.class));
        if (navCourses != null) navCourses.setOnClickListener(v -> navigateTo(MyCoursesActivity.class));
        if (navProfile != null) navProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));
        if (navSettings != null) selectNav(navSettings);

        selectNav(navSettings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.mulungushi.campuscompanionapp.util.NavBadge.refresh(this);
    }

    private void navigateTo(Class<?> targetActivity) {
        startActivity(new Intent(this, targetActivity));
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
