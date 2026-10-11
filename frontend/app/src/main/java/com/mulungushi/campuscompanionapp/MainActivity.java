package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Bundle;
<<<<<<< Updated upstream
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

=======
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final String STUB_STUDENT_FIRST_NAME = "Kellys";
    private static final String STUB_PROGRAMME = "Computer Science";
    private static final String STUB_YEAR = "Year 3";
    private static final String STUB_NEXT_CLASS_NAME = "ICT 361 · Software Engineering";
    private static final String STUB_NEXT_CLASS_DETAILS = "10:00 AM · Computer Lab 2 · PC-14";

    private View navDashboard, navGroup, navProfile, navAlerts, navSettings;
    private SwipeRefreshLayout swipeRefreshLayout;

>>>>>>> Stashed changes
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

<<<<<<< Updated upstream
        Button btnGetStarted = findViewById(R.id.btnGetStarted);
        if (btnGetStarted != null) {
            btnGetStarted.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            });
        }
    }
=======
        bindViews();
        populateContent();
        setupPullToRefresh();
        setupClickListeners();
    }

    private void bindViews() {
        navDashboard = findViewById(R.id.navDashboard);
        navGroup = findViewById(R.id.navGroup);
        navProfile = findViewById(R.id.navProfile);
        navAlerts = findViewById(R.id.navAlerts);
        navSettings = findViewById(R.id.navSettings);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void populateContent() {
        View tvWelcome = findViewById(R.id.tvWelcome);
        if (tvWelcome instanceof TextView) {
            String timeGreeting = getTimeOfDayGreeting();
            ((TextView) tvWelcome).setText(timeGreeting + ", " + STUB_STUDENT_FIRST_NAME);
        }

        View tvProgrammeYear = findViewById(R.id.tvProgrammeYear);
        if (tvProgrammeYear instanceof TextView) {
            ((TextView) tvProgrammeYear).setText(STUB_PROGRAMME + " \u00b7 " + STUB_YEAR);
        }

        View tvBadge = findViewById(R.id.tvRegistrationBadge);
        if (tvBadge instanceof TextView) {
            ((TextView) tvBadge).setText("Verified");
        }

        View tvNextClassName = findViewById(R.id.tvNextClassName);
        if (tvNextClassName instanceof TextView) {
            ((TextView) tvNextClassName).setText(STUB_NEXT_CLASS_NAME);
        }

        View tvNextClassDetails = findViewById(R.id.tvNextClassDetails);
        if (tvNextClassDetails instanceof TextView) {
            ((TextView) tvNextClassDetails).setText(STUB_NEXT_CLASS_DETAILS);
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
                populateContent();
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, "Dashboard updated", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupClickListeners() {
        View avatar = findViewById(R.id.avatarContainer);
        if (avatar != null) {
            avatar.setOnClickListener(v -> navigateInternal(ProfileActivity.class));
        }

        View cardRegistration = findViewById(R.id.cardRegistration);
        if (cardRegistration != null) {
            cardRegistration.setOnClickListener(v -> navigateInternal(RegistrationProgressActivity.class));
        }

        View cardNextClass = findViewById(R.id.cardNextClass);
        if (cardNextClass != null) {
            cardNextClass.setOnClickListener(v -> navigateInternal(ClassDetailActivity.class));
        }

        View tileProgramme = findViewById(R.id.tileProgramme);
        if (tileProgramme != null) {
            tileProgramme.setOnClickListener(v -> navigateInternal(MyCoursesActivity.class));
        }

        View tileTimetable = findViewById(R.id.tileTimetable);
        if (tileTimetable != null) {
            tileTimetable.setOnClickListener(v -> navigateInternal(MyGroupActivity.class));
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
        }
    }
>>>>>>> Stashed changes
}
