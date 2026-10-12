package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanionapp.network.ApiBodies;
import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.MessageResponse;
import com.mulungushi.campuscompanionapp.network.SessionManager;
import com.mulungushi.campuscompanionapp.network.StudentProfile;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "campus_companion_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private View navDashboard, navGroup, navProfile, navAlerts, navSettings;
    // Latest profile values, used to pre-fill the edit dialog.
    private String curName = "", curEmail = "", curPhone = "", curNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
        findViewById(R.id.btnSignOut).setOnClickListener(v -> showSignOutDialog());

        View btnEdit = findViewById(R.id.btnEditProfile);
        if (btnEdit != null) btnEdit.setOnClickListener(v -> showEditDialog());

        View rowSettings = findViewById(R.id.rowSettings);
        if (rowSettings != null) rowSettings.setOnClickListener(v -> navigateTo(SettingsActivity.class));

        View correction = findViewById(R.id.tvRequestCorrection);
        if (correction != null) correction.setOnClickListener(v -> showCorrectionDialog());

        setupBottomNav();
        loadProfile();
    }

    private void setInitials(String name) {
        TextView tv = findViewById(R.id.tvAvatarInitials);
        if (tv == null) return;
        if (name == null || name.trim().isEmpty()) { tv.setText("?"); return; }
        String[] parts = name.trim().split("\\s+");
        String s = parts[0].substring(0, 1);
        if (parts.length > 1) s += parts[parts.length - 1].substring(0, 1);
        tv.setText(s.toUpperCase());
    }

    private void showEditDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Math.round(20 * getResources().getDisplayMetrics().density);
        box.setPadding(p, p / 2, p, 0);

        final EditText name = new EditText(this); name.setHint("Full name"); name.setText(curName);
        final EditText email = new EditText(this); email.setHint("Email"); email.setText(curEmail);
        email.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        final EditText phone = new EditText(this); phone.setHint("Phone"); phone.setText(curPhone);
        phone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);

        TextView note = new TextView(this);
        note.setText("Student number changes need lecturer approval.");
        note.setTextColor(0xFF64748B);
        note.setTextSize(12);

        box.addView(name); box.addView(email); box.addView(phone); box.addView(note);

        new AlertDialog.Builder(this)
                .setTitle("Edit profile")
                .setView(box)
                .setPositiveButton("Save", (d, w) -> {
                    ApiService api = ApiClient.getClient(this).create(ApiService.class);
                    api.updateMe(new ApiBodies.UpdateSelf(
                            name.getText().toString().trim(),
                            email.getText().toString().trim(),
                            phone.getText().toString().trim()))
                            .enqueue(new Callback<StudentProfile>() {
                                @Override public void onResponse(Call<StudentProfile> c, Response<StudentProfile> r) {
                                    if (r.isSuccessful() && r.body() != null) { fillProfile(r.body()); Toast.makeText(ProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show(); }
                                    else Toast.makeText(ProfileActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onFailure(Call<StudentProfile> c, Throwable t) { Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
                            });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showCorrectionDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Math.round(20 * getResources().getDisplayMetrics().density);
        box.setPadding(p, p / 2, p, 0);
        final EditText correct = new EditText(this); correct.setHint("Correct student number (9 digits)");
        correct.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        final EditText reason = new EditText(this); reason.setHint("Reason (optional)");
        box.addView(correct); box.addView(reason);

        new AlertDialog.Builder(this)
                .setTitle("Request number correction")
                .setView(box)
                .setPositiveButton("Send request", (d, w) -> {
                    ApiService api = ApiClient.getClient(this).create(ApiService.class);
                    String r = reason.getText().toString().trim();
                    api.requestNumberCorrection(new ApiBodies.NumberCorrection(
                            correct.getText().toString().trim(), r.isEmpty() ? null : r))
                            .enqueue(new Callback<MessageResponse>() {
                                @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> resp) {
                                    Toast.makeText(ProfileActivity.this, "Correction request sent to lecturer", Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onFailure(Call<MessageResponse> c, Throwable t) { Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
                            });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void fillProfile(StudentProfile p) {
        TextView tvName = findViewById(R.id.tvProfileName);
        TextView tvId = findViewById(R.id.tvProfileId);
        TextView tvEmail = findViewById(R.id.tvEmail);
        TextView tvProgramme = findViewById(R.id.tvProgrammeYear);
        TextView tvLabGroup = findViewById(R.id.tvLabGroup);
        TextView tvNumber = findViewById(R.id.tvStudentNumber);

        curName = p.getName() == null ? "" : p.getName();
        curEmail = p.getEmail() == null ? "" : p.getEmail();
        curPhone = p.getPhone() == null ? "" : p.getPhone();
        curNumber = p.getStudentNumber() == null ? "" : p.getStudentNumber();

        if (tvName != null) tvName.setText(curName);
        setInitials(curName);
        if (tvId != null) tvId.setText("Student · " + curNumber);
        if (tvEmail != null) tvEmail.setText(curEmail);
        if (tvProgramme != null) tvProgramme.setText(p.getProgrammeName());
        if (tvNumber != null) tvNumber.setText(curNumber);
        if (tvLabGroup != null) {
            String g = p.getLabGroup();
            tvLabGroup.setText(g == null || g.isEmpty() ? "Unassigned" : "Group " + g);
        }
    }

    /** Fill the profile from the saved session first, then refine with GET /api/students/me. */
    private void loadProfile() {
        final TextView tvName = findViewById(R.id.tvProfileName);

        // Instant values from the login session, overwritten by the server below.
        String savedName = SessionManager.getInstance(this).getUserName();
        if (savedName != null && !savedName.isEmpty()) {
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
                // Offline / unreachable: keep the saved-session values already shown.
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
