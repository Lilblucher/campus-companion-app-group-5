package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.mulungushi.campuscompanionapp.network.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * App-wide quick search. Tapping the dashboard search pill opens this; typing
 * filters the real, reachable destinations (role-aware) and tapping one navigates
 * there. This is a working search rather than a decorative field.
 */
public class SearchActivity extends AppCompatActivity {

    private static class Dest {
        final String title, sub, keywords;
        final int icon, chip, tint;
        final Class<?> target;
        Dest(String title, String sub, String keywords, int icon, int chip, int tint, Class<?> target) {
            this.title = title; this.sub = sub; this.keywords = keywords;
            this.icon = icon; this.chip = chip; this.tint = tint; this.target = target;
        }
    }

    private final List<Dest> all = new ArrayList<>();
    private final List<Dest> shown = new ArrayList<>();
    private ResultAdapter adapter;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_search);

        buildDestinations(SessionManager.getInstance(this).isLecturer());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        final EditText et = findViewById(R.id.etSearch);
        final View btnClear = findViewById(R.id.btnClear);
        ListView list = findViewById(R.id.listViewSearch);

        adapter = new ResultAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            startActivity(new Intent(this, shown.get(pos).target));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) {
                btnClear.setVisibility(c.length() > 0 ? View.VISIBLE : View.GONE);
                filter(c.toString());
            }
            @Override public void afterTextChanged(Editable e) {}
        });
        btnClear.setOnClickListener(v -> et.setText(""));

        filter("");

        // Focus + show the keyboard immediately.
        et.requestFocus();
        et.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
        }, 150);
    }

    private void buildDestinations(boolean lecturer) {
        all.clear();
        int blue = R.drawable.cc_iconchip_blue,   tBlue = R.color.cc_accent_blue;
        int green = R.drawable.cc_iconchip_green,  tGreen = R.color.cc_success;
        int amber = R.drawable.cc_iconchip_amber,  tAmber = R.color.cc_warning_strong;

        if (lecturer) {
            all.add(new Dest("Students", "View and manage student records", "students roster records manage people", R.drawable.ic_ms_school, blue, tBlue, LecturerStudentsActivity.class));
            all.add(new Dest("Lab Groups", "Manage groups G01–G04", "groups lab g01 g02 g03 g04 workspaces", R.drawable.ic_ms_workspaces, green, tGreen, ManageGroupsActivity.class));
            all.add(new Dest("Pending Requests", "Approve or decline group changes", "requests pending approve reject change inbox", R.drawable.ic_ms_inbox, amber, tAmber, PendingRequestsActivity.class));
            all.add(new Dest("Timetable", "Teaching schedule", "timetable schedule classes lectures", R.drawable.ic_ms_calendar_month, blue, tBlue, TimetableActivity.class));
            all.add(new Dest("Reminders", "Create and view reminders", "reminders notes announcements", R.drawable.ic_ms_notifications_active, amber, tAmber, RemindersActivity.class));
        } else {
            all.add(new Dest("My Courses", "3 courses active", "courses modules classes subjects", R.drawable.ic_ms_menu_book, blue, tBlue, MyCoursesActivity.class));
            all.add(new Dest("Timetable", "Weekly schedule", "timetable schedule classes next class", R.drawable.ic_ms_calendar_month, blue, tBlue, TimetableActivity.class));
            all.add(new Dest("My Lab Group", "Members, venue and slot", "group lab members venue workstation", R.drawable.ic_ms_groups, green, tGreen, MyGroupActivity.class));
            all.add(new Dest("Reminders", "Stay on track", "reminders notes tasks", R.drawable.ic_ms_notifications_active, amber, tAmber, RemindersActivity.class));
            all.add(new Dest("Registration Progress", "Steps and claim code", "registration progress claim code fee modules", R.drawable.ic_ms_fact_check, blue, tBlue, RegistrationProgressActivity.class));
            all.add(new Dest("Class Details", "Venue, lecturer and manual", "class details venue lecturer manual", R.drawable.ic_ms_code, blue, tBlue, ClassDetailActivity.class));
        }
        // Common to both roles
        all.add(new Dest("Alerts", "Notifications and announcements", "alerts notifications announcements updates", R.drawable.ic_ms_notifications, blue, tBlue, NotificationsActivity.class));
        all.add(new Dest("Profile", "Your account details", "profile account me details edit", R.drawable.ic_ms_person, green, tGreen, ProfileActivity.class));
        all.add(new Dest("Settings", "Account, security and sync", "settings security dark theme sync biometric", R.drawable.ic_ms_settings, blue, tBlue, SettingsActivity.class));
    }

    private void filter(String q) {
        String query = q.trim().toLowerCase();
        shown.clear();
        for (Dest d : all) {
            if (query.isEmpty()
                    || d.title.toLowerCase().contains(query)
                    || d.sub.toLowerCase().contains(query)
                    || d.keywords.contains(query)) {
                shown.add(d);
            }
        }
        adapter.notifyDataSetChanged();
        View empty = findViewById(R.id.tvEmpty);
        if (empty != null) empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private class ResultAdapter extends BaseAdapter {
        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int p) { return shown.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View cv, ViewGroup parent) {
            if (cv == null) cv = LayoutInflater.from(SearchActivity.this).inflate(R.layout.item_search_result, parent, false);
            Dest d = shown.get(p);
            ImageView icon = cv.findViewById(R.id.ivResultIcon);
            TextView title = cv.findViewById(R.id.tvResultTitle);
            TextView sub = cv.findViewById(R.id.tvResultSub);
            icon.setBackgroundResource(d.chip);
            icon.setImageResource(d.icon);
            icon.setColorFilter(ContextCompat.getColor(SearchActivity.this, d.tint));
            title.setText(d.title);
            sub.setText(d.sub);
            return cv;
        }
    }
}
