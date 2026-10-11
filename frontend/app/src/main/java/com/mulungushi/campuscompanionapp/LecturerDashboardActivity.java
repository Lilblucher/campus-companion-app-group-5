package com.mulungushi.campuscompanionapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.Calendar;

import java.util.Locale;
import java.util.Random;

public class LecturerDashboardActivity extends AppCompatActivity {

    private View navDashboard, navGroup, navProfile, navAlerts, navSettings;
    private View tileGenerateClaimCode, tileMyCourses, tileLabGroups, tilePostAnnouncement;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_dashboard);

        initViews();
        setupHeader();
        setupPullToRefresh();
        setupQuickAccess();
        setupBottomNav();
    }

    private void initViews() {
        navDashboard = findViewById(R.id.navDashboard);
        navGroup = findViewById(R.id.navGroup);
        navProfile = findViewById(R.id.navProfile);
        navAlerts = findViewById(R.id.navAlerts);
        navSettings = findViewById(R.id.navSettings);

        tileGenerateClaimCode = findViewById(R.id.tileGenerateClaimCode);
        tileMyCourses = findViewById(R.id.tileMyCourses);
        tileLabGroups = findViewById(R.id.tileLabGroups);
        tilePostAnnouncement = findViewById(R.id.tilePostAnnouncement);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupHeader() {
        View tvWelcome = findViewById(R.id.tvWelcome);

        if (tvWelcome instanceof TextView) {
            String timeGreeting = getTimeOfDayGreeting();
            ((TextView) tvWelcome).setText(timeGreeting + ", Dr. Mulenga");
        }
    }

    private String getTimeOfDayGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) {
            return "Good Morning";
        } else if (hour >= 12 && hour < 17) {
            return "Good Afternoon";
        } else if (hour >= 17 && hour < 22) {
            return "Good Evening";
        } else {
            return "Good Night";
        }
    }

    private void setupPullToRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(0xFFE51937, 0xFF022F77);
            swipeRefreshLayout.setOnRefreshListener(() -> {
                setupHeader();
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, "Lecturer Dashboard updated", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupQuickAccess() {
        if (tileGenerateClaimCode != null) {
            tileGenerateClaimCode.setOnClickListener(v -> showGenerateClaimCodeDialog());
        }

        if (tileMyCourses != null) {
            tileMyCourses.setOnClickListener(v -> navigateInternal(MyCoursesActivity.class));
        }

        if (tileLabGroups != null) {
            tileLabGroups.setOnClickListener(v -> navigateInternal(MyGroupActivity.class));
        }

        if (tilePostAnnouncement != null) {
            tilePostAnnouncement.setOnClickListener(v -> showPostAnnouncementDialog());
        }
    }

    private void showGenerateClaimCodeDialog() {
        String generatedCode = "ICT361-" + String.format(Locale.US, "%04d", new Random().nextInt(10000));

        new AlertDialog.Builder(this)
                .setTitle(R.string.claim_code_generated_title)
                .setMessage("Generated Code for ICT 361 Lab Group:\n\n" + generatedCode)
                .setPositiveButton(R.string.copy_code, (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("Claim Code", generatedCode);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, R.string.code_copied_toast, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showPostAnnouncementDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter announcement text...");

        new AlertDialog.Builder(this)
                .setTitle(R.string.post_announcement)
                .setView(input)
                .setPositiveButton("Post Broadcast", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        Toast.makeText(this, "Announcement posted to Lab Groups G01–G04!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Announcement text cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setupBottomNav() {
        View avatar = findViewById(R.id.avatarContainer);
        if (avatar != null) {
            avatar.setOnClickListener(v -> navigateInternal(ProfileActivity.class));
        }

        if (navDashboard != null) navDashboard.setOnClickListener(v -> selectNav(navDashboard));
        if (navGroup != null) navGroup.setOnClickListener(v -> navigatePage(MyGroupActivity.class));
        if (navProfile != null) navProfile.setOnClickListener(v -> navigatePage(ProfileActivity.class));
        if (navAlerts != null) navAlerts.setOnClickListener(v -> navigatePage(NotificationsActivity.class));
        if (navSettings != null) navSettings.setOnClickListener(v -> navigatePage(SettingsActivity.class));

        selectNav(navDashboard);
    }

    private void navigatePage(Class<?> targetActivity) {
        startActivity(new Intent(this, targetActivity));
    }

    private void navigateInternal(Class<?> targetActivity) {
        startActivity(new Intent(this, targetActivity));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
