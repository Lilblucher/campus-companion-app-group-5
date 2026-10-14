package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mulungushi.campuscompanionapp.util.Skeleton;
import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.NextSessionResponse;
import com.mulungushi.campuscompanionapp.network.SessionManager;
import com.mulungushi.campuscompanionapp.network.StudentProfile;
import com.mulungushi.campuscompanionapp.network.TimetableResponse;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity {

    // Timetable / next-class data isn't in the backend yet, so these stay placeholders.
    private static final String STUB_NEXT_CLASS_NAME = "ICT 361 · Software Engineering";
    private static final String STUB_NEXT_CLASS_DETAILS = "10:00 AM · Computer Lab 2 · PC-14";

    private View navDashboard, navGroup, navProfile, navCourses, navSettings;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        Skeleton.start(findViewById(R.id.skeletonDashboard));
        populateContent();
        loadStudent();
        loadNextClass();
        setupPullToRefresh();
        setupClickListeners();
    }

    private void revealContent() {
        Skeleton.finish(findViewById(R.id.skeletonDashboard), findViewById(R.id.dashboardContent));
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.mulungushi.campuscompanionapp.util.NavBadge.refresh(this);
    }

    /** Replace the placeholder next-class card with the real GET /api/timetable/next. */
    private void loadNextClass() {
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getNextSession().enqueue(new Callback<NextSessionResponse>() {
            @Override
            public void onResponse(Call<NextSessionResponse> c, Response<NextSessionResponse> r) {
                if (!r.isSuccessful() || r.body() == null || r.body().next == null) return;
                TimetableResponse.Session s = r.body().next;
                View nm = findViewById(R.id.tvNextClassName);
                if (nm instanceof TextView) ((TextView) nm).setText(s.course_code + " · " + s.title);
                View dt = findViewById(R.id.tvNextClassDetails);
                if (dt instanceof TextView) {
                    String day = (s.day_of_week >= 0 && s.day_of_week < 7) ? TimetableActivity.DAYS[s.day_of_week] : "";
                    String t = s.start_time == null ? "" : s.start_time.substring(0, Math.min(5, s.start_time.length()));
                    String where = (s.venue == null ? "" : " · " + s.venue) + (s.room == null ? "" : " · " + s.room);
                    ((TextView) dt).setText(day + " " + t + where);
                }
            }
            @Override public void onFailure(Call<NextSessionResponse> c, Throwable t) { /* keep placeholder */ }
        });
    }

    private void bindViews() {
        navDashboard = findViewById(R.id.navDashboard);
        navGroup = findViewById(R.id.navGroup);
        navProfile = findViewById(R.id.navProfile);
        navCourses = findViewById(R.id.navCourses);
        navSettings = findViewById(R.id.navSettings);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void populateContent() {
        // Greeting uses the saved session name immediately; loadStudent() refines
        // the name and fills programme/status from the server.
        String savedName = SessionManager.getInstance(this).getUserName();

        View tvGreeting = findViewById(R.id.tvGreeting);
        if (tvGreeting instanceof TextView) {
            ((TextView) tvGreeting).setText(getTimeOfDayGreeting() + ",");
        }

        View tvWelcome = findViewById(R.id.tvWelcome);
        if (tvWelcome instanceof TextView) {
            ((TextView) tvWelcome).setText(displayName(savedName));
        }

        View tvInitials = findViewById(R.id.tvAvatarInitials);
        if (tvInitials instanceof TextView) {
            ((TextView) tvInitials).setText(initials(savedName));
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

    /** Load the signed-in student's real name, programme and status from the backend. */
    private void loadStudent() {
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getMe().enqueue(new Callback<StudentProfile>() {
            @Override
            public void onResponse(Call<StudentProfile> call, Response<StudentProfile> response) {
                revealContent();
                if (!response.isSuccessful() || response.body() == null) return;
                StudentProfile p = response.body();

                View tvWelcome = findViewById(R.id.tvWelcome);
                if (tvWelcome instanceof TextView) {
                    ((TextView) tvWelcome).setText(displayName(p.getName()));
                }
                View tvInitials = findViewById(R.id.tvAvatarInitials);
                if (tvInitials instanceof TextView) {
                    ((TextView) tvInitials).setText(initials(p.getName()));
                }
                // Registration status badge
                View tvBadge = findViewById(R.id.tvRegistrationBadge);
                if (tvBadge instanceof TextView && p.getStatus() != null && !p.getStatus().isEmpty()) {
                    ((TextView) tvBadge).setText(prettyStatus(p.getStatus()));
                }
                // Lab-group tile subtitle
                View tvGroupSub = findViewById(R.id.tvTileGroupSub);
                if (tvGroupSub instanceof TextView && p.getLabGroup() != null && !p.getLabGroup().isEmpty()) {
                    ((TextView) tvGroupSub).setText(p.getLabGroup());
                }
            }

            @Override
            public void onFailure(Call<StudentProfile> call, Throwable t) {
                // Offline: keep the saved-session greeting already shown.
                revealContent();
            }
        });
    }

    private static String displayName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "there";
        return fullName.trim();
    }

    private static String initials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        String s = parts[0].substring(0, 1);
        if (parts.length > 1) s += parts[parts.length - 1].substring(0, 1);
        return s.toUpperCase();
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "there";
        return fullName.trim().split("\\s+")[0];
    }

    private static String prettyStatus(String status) {
        if (status == null || status.isEmpty()) return "";
        return status.substring(0, 1).toUpperCase() + status.substring(1);
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
                loadStudent();
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

        View tileTimetable = findViewById(R.id.tileTimetable);
        if (tileTimetable != null) {
            tileTimetable.setOnClickListener(v -> navigateInternal(TimetableActivity.class));
        }

        View tileCourses = findViewById(R.id.tileCourses);
        if (tileCourses != null) {
            tileCourses.setOnClickListener(v -> navigateInternal(MyCoursesActivity.class));
        }

        View tileMaterials = findViewById(R.id.tileMaterials);
        if (tileMaterials != null) {
            tileMaterials.setOnClickListener(v -> navigateInternal(CourseMaterialsActivity.class));
        }

        View tileAccounts = findViewById(R.id.tileAccounts);
        if (tileAccounts != null) {
            tileAccounts.setOnClickListener(v -> navigateInternal(AccountsActivity.class));
        }

        View cardLabRegistration = findViewById(R.id.cardLabRegistration);
        if (cardLabRegistration != null) {
            cardLabRegistration.setOnClickListener(v -> navigateInternal(LabRegistrationActivity.class));
        }

        View btnBell = findViewById(R.id.btnBell);
        if (btnBell != null) {
            btnBell.setOnClickListener(v -> navigatePage(NotificationsActivity.class));
        }

        View searchPill = findViewById(R.id.searchPill);
        if (searchPill != null) {
            searchPill.setOnClickListener(v -> navigateInternal(SearchActivity.class));
        }

        View cardAnnouncement = findViewById(R.id.cardAnnouncement);
        if (cardAnnouncement != null) {
            cardAnnouncement.setOnClickListener(v -> navigatePage(NotificationsActivity.class));
        }

        View tvSeeAll = findViewById(R.id.tvAnnouncementsSeeAll);
        if (tvSeeAll != null) {
            tvSeeAll.setOnClickListener(v -> navigatePage(NotificationsActivity.class));
        }

        View syncTile = findViewById(R.id.syncTile);
        if (syncTile != null) {
            syncTile.setOnClickListener(v -> {
                View tv = findViewById(R.id.tvSyncStatus);
                if (tv instanceof TextView) ((TextView) tv).setText("Synced just now");
                Toast.makeText(this, "Synced", Toast.LENGTH_SHORT).show();
            });
        }

        if (navDashboard != null) navDashboard.setOnClickListener(v -> selectNav(navDashboard));
        if (navGroup != null) navGroup.setOnClickListener(v -> navigatePage(MyGroupActivity.class));
        View navCourses = findViewById(R.id.navCourses);
        if (navCourses != null) navCourses.setOnClickListener(v -> navigatePage(MyCoursesActivity.class));
        if (navProfile != null) navProfile.setOnClickListener(v -> navigatePage(ProfileActivity.class));
        if (navSettings != null) navSettings.setOnClickListener(v -> navigatePage(SettingsActivity.class));

        selectNav(navDashboard);
    }

    private void navigatePage(Class<?> targetActivity) {
        startActivity(new Intent(this, targetActivity));
    }

    private void navigateInternal(Class<?> targetActivity) {
        startActivity(new Intent(this, targetActivity));
        overridePendingTransition(0, 0);
    }

    private void selectNav(View selected) {
        View[] allNavItems = {navDashboard, navGroup, navCourses, navProfile, navSettings};
        for (View item : allNavItems) {
            if (item == null) continue;
            boolean isSelected = (item == selected);
            item.setSelected(isSelected);
        }
    }
}
