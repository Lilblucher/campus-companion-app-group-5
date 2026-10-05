package zm.ac.mulungushi.ict361;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LecturerDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lecturer_dashboard);

        setupWindowInsets();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupClickListeners();
    }

    private void setupWindowInsets() {
        View headerProfile = findViewById(R.id.headerProfile);
        if (headerProfile != null) {
            ViewCompat.setOnApplyWindowInsetsListener(headerProfile, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        View bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
        }
    }

    private void setupClickListeners() {
        // Header Profile
        View headerProfile = findViewById(R.id.headerProfile);
        if (headerProfile != null) {
            headerProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        // Stat Cards
        View cardTotalStudents = findViewById(R.id.cardTotalStudents);
        if (cardTotalStudents != null) {
            cardTotalStudents.setOnClickListener(v -> startActivity(new Intent(this, StudentsActivity.class)));
        }

        View cardMyCourses = findViewById(R.id.cardMyCourses);
        if (cardMyCourses != null) {
            cardMyCourses.setOnClickListener(v -> startActivity(new Intent(this, CoursesActivity.class)));
        }

        View cardPendingRequests = findViewById(R.id.cardPendingRequests);
        if (cardPendingRequests != null) {
            cardPendingRequests.setOnClickListener(v -> startActivity(new Intent(this, PendingRequestsActivity.class)));
        }

        View cardTodaysClasses = findViewById(R.id.cardTodaysClasses);
        if (cardTodaysClasses != null) {
            cardTodaysClasses.setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        }

        // Quick Action Buttons
        View btnStudents = findViewById(R.id.btnStudents);
        if (btnStudents != null) {
            btnStudents.setOnClickListener(v -> startActivity(new Intent(this, StudentsActivity.class)));
        }

        View btnManageGroups = findViewById(R.id.btnManageGroups);
        if (btnManageGroups != null) {
            btnManageGroups.setOnClickListener(v -> startActivity(new Intent(this, ManageGroupsActivity.class)));
        }

        View btnSearchStudent = findViewById(R.id.btnSearchStudent);
        if (btnSearchStudent != null) {
            btnSearchStudent.setOnClickListener(v -> startActivity(new Intent(this, SearchStudentActivity.class)));
        }

        View btnMore = findViewById(R.id.btnMore);
        if (btnMore != null) {
            btnMore.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        }

        // Bottom Navigation
        TextView navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> Toast.makeText(this, "You are on the Home Dashboard", Toast.LENGTH_SHORT).show());
        }

        TextView navStudents = findViewById(R.id.navStudents);
        if (navStudents != null) {
            navStudents.setOnClickListener(v -> startActivity(new Intent(this, StudentsActivity.class)));
        }

        TextView navCourses = findViewById(R.id.navCourses);
        if (navCourses != null) {
            navCourses.setOnClickListener(v -> startActivity(new Intent(this, CoursesActivity.class)));
        }

        TextView navNotifications = findViewById(R.id.navNotifications);
        if (navNotifications != null) {
            navNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        }

        TextView navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }
    }
}
