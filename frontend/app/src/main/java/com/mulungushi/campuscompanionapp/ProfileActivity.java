package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.SessionManager;
import com.mulungushi.campuscompanionapp.network.StudentProfile;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends BaseActivity {

    private View navDashboard, navGroup, navProfile, navCourses, navSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        findViewById(R.id.btnBell).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.rowAccounts).setOnClickListener(v -> {
            startActivity(new Intent(this, AccountsActivity.class));
            overridePendingTransition(0, 0);
        });

        View btnViewFees = findViewById(R.id.btnViewFees);
        if (btnViewFees != null) {
            btnViewFees.setOnClickListener(v -> {
                startActivity(new Intent(this, AccountsActivity.class));
                overridePendingTransition(0, 0);
            });
        }

        findViewById(R.id.btnSignOut).setOnClickListener(v -> showSignOutDialog());

        setupBottomNav();
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.mulungushi.campuscompanionapp.util.NavBadge.refresh(this);
        loadProfile();
    }

    private void setInitials(String name) {
        TextView tvAvatar = findViewById(R.id.tvAvatarInitials);
        TextView tvIdCard = findViewById(R.id.tvIdCardInitials);
        if (name == null || name.trim().isEmpty()) {
            if (tvAvatar != null) tvAvatar.setText("?");
            if (tvIdCard != null) tvIdCard.setText("?");
            return;
        }
        String[] parts = name.trim().split("\\s+");
        String s = parts[0].substring(0, 1);
        if (parts.length > 1) s += parts[parts.length - 1].substring(0, 1);
        String initials = s.toUpperCase();
        if (tvAvatar != null) tvAvatar.setText(initials);
        if (tvIdCard != null) tvIdCard.setText(initials);
    }

    private void fillProfile(StudentProfile p) {
        String name = p.getName() == null ? "" : p.getName();
        String email = p.getEmail() == null ? "" : p.getEmail();
        String number = p.getStudentNumber() == null ? "" : p.getStudentNumber();
        String programme = p.getProgrammeName() == null ? "Computer Science" : p.getProgrammeName();
        int yearOfStudy = p.getYearOfStudy();
        String labGroup = p.getLabGroup();
        int feesPaidPct = p.getFeesPaidPct();
        String examNumber = p.getExamNumber();
        String portalUsername = p.getPortalUsername();

        // Name and subtitle
        TextView tvName = findViewById(R.id.tvProfileName);
        if (tvName != null) tvName.setText(name);
        setInitials(name);

        TextView tvSubtitle = findViewById(R.id.tvProfileSubtitle);
        if (tvSubtitle != null) tvSubtitle.setText(programme + " · Year " + yearOfStudy);

        // Personal info
        TextView tvEmail = findViewById(R.id.tvEmail);
        if (tvEmail != null) tvEmail.setText(email);

        TextView tvProgrammeYear = findViewById(R.id.tvProgrammeYear);
        if (tvProgrammeYear != null) tvProgrammeYear.setText(programme + " · Year " + yearOfStudy);

        TextView tvLabGroup = findViewById(R.id.tvLabGroup);
        if (tvLabGroup != null) {
            tvLabGroup.setText(labGroup == null || labGroup.isEmpty()
                    ? "Unassigned" : "G" + labGroup + " · Computer Lab 2");
        }

        // Student credentials - gated on 50% fees
        boolean unlocked = feesPaidPct >= 50;

        View credentialsUnlocked = findViewById(R.id.credentialsUnlocked);
        View credentialsLocked = findViewById(R.id.credentialsLocked);
        LinearLayout credentialChip = findViewById(R.id.credentialChip);
        ImageView ivChipIcon = findViewById(R.id.ivCredentialChipIcon);
        TextView tvChipLabel = findViewById(R.id.tvCredentialChipLabel);

        if (unlocked) {
            if (credentialsUnlocked != null) credentialsUnlocked.setVisibility(View.VISIBLE);
            if (credentialsLocked != null) credentialsLocked.setVisibility(View.GONE);

            if (credentialChip != null) credentialChip.setBackgroundResource(R.drawable.cc_banner_success);
            if (ivChipIcon != null) {
                ivChipIcon.setImageResource(R.drawable.ic_ms_lock_open);
                ivChipIcon.setColorFilter(ContextCompat.getColor(this, R.color.cc_green_fg));
            }
            if (tvChipLabel != null) {
                tvChipLabel.setText(R.string.credentials_unlocked);
                tvChipLabel.setTextColor(ContextCompat.getColor(this, R.color.cc_green_fg));
            }

            // Fill ID card
            TextView tvIdCardName = findViewById(R.id.tvIdCardName);
            if (tvIdCardName != null) tvIdCardName.setText(name);

            TextView tvIdCardMeta = findViewById(R.id.tvIdCardMeta);
            if (tvIdCardMeta != null) {
                tvIdCardMeta.setText(number + " · " + programme.substring(0, Math.min(2, programme.length())).toUpperCase()
                        + " · Year " + yearOfStudy + " · Valid to Dec 2026");
            }

            // Fill credential rows
            TextView tvStudentNumber = findViewById(R.id.tvStudentNumber);
            if (tvStudentNumber != null) tvStudentNumber.setText(number);

            TextView tvExamNumber = findViewById(R.id.tvExamNumber);
            if (tvExamNumber != null) tvExamNumber.setText(examNumber == null || examNumber.isEmpty()
                    ? "EX-2026-" + (number.length() >= 4 ? number.substring(number.length() - 4) : number)
                    : examNumber);

            TextView tvPortalUsername = findViewById(R.id.tvPortalUsername);
            if (tvPortalUsername != null) tvPortalUsername.setText(portalUsername == null || portalUsername.isEmpty()
                    ? "cm" + number : portalUsername);
        } else {
            if (credentialsUnlocked != null) credentialsUnlocked.setVisibility(View.GONE);
            if (credentialsLocked != null) credentialsLocked.setVisibility(View.VISIBLE);

            if (credentialChip != null) credentialChip.setBackgroundResource(R.drawable.cc_banner_warning);
            if (ivChipIcon != null) {
                ivChipIcon.setImageResource(R.drawable.ic_ms_lock);
                ivChipIcon.setColorFilter(ContextCompat.getColor(this, R.color.cc_amber_fg));
            }
            if (tvChipLabel != null) {
                tvChipLabel.setText("Locked");
                tvChipLabel.setTextColor(ContextCompat.getColor(this, R.color.cc_amber_fg));
            }

            TextView tvLockedPct = findViewById(R.id.tvLockedPct);
            if (tvLockedPct != null) {
                tvLockedPct.setText("You've paid " + feesPaidPct + "% of your tuition. Reach 50% to unlock.");
            }
        }
    }

    private void loadProfile() {
        String savedName = SessionManager.getInstance(this).getUserName();
        if (savedName != null && !savedName.isEmpty()) {
            TextView tvName = findViewById(R.id.tvProfileName);
            if (tvName != null) tvName.setText(savedName);
            setInitials(savedName);
        }

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getMe().enqueue(new Callback<StudentProfile>() {
            @Override
            public void onResponse(Call<StudentProfile> call, Response<StudentProfile> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                fillProfile(response.body());
            }

            @Override
            public void onFailure(Call<StudentProfile> call, Throwable t) {
            }
        });
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
        navCourses = findViewById(R.id.navCourses);
        navSettings = findViewById(R.id.navSettings);

        if (navDashboard != null) navDashboard.setOnClickListener(v -> navigateTo(MainActivity.class));
        if (navGroup != null) navGroup.setOnClickListener(v -> navigateTo(MyGroupActivity.class));
        if (navCourses != null) navCourses.setOnClickListener(v -> navigateTo(MyCoursesActivity.class));
        if (navProfile != null) selectNav(navProfile);
        if (navSettings != null) navSettings.setOnClickListener(v -> navigateTo(SettingsActivity.class));

        selectNav(navProfile);
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
