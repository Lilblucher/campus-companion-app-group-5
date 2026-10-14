package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.mulungushi.campuscompanionapp.data.local.CachedNotification;
import com.mulungushi.campuscompanionapp.data.repo.CacheRepo;
import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.ConnectivityMonitor;
import com.mulungushi.campuscompanionapp.util.NetBanner;
import com.mulungushi.campuscompanionapp.util.Skeleton;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private View navDashboard, navGroup, navCourses, navProfile, navSettings;
    private final List<CachedNotification> allItems = new ArrayList<>();
    private String filter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        Skeleton.start(findViewById(R.id.skeletonList));
        setupChips();
        setupBottomNav();
        loadNotifications();
    }

    private void setupChips() {
        View a = findViewById(R.id.chipAll);
        View b = findViewById(R.id.chipAnnouncements);
        View c = findViewById(R.id.chipTimetable);
        if (a != null) a.setOnClickListener(v -> { filter = "all"; selectChip(); renderList(); });
        if (b != null) b.setOnClickListener(v -> { filter = "announcements"; selectChip(); renderList(); });
        if (c != null) c.setOnClickListener(v -> { filter = "timetable"; selectChip(); renderList(); });
        selectChip();
    }

    private void selectChip() {
        setChip(findViewById(R.id.chipAll), "all".equals(filter));
        setChip(findViewById(R.id.chipAnnouncements), "announcements".equals(filter));
        setChip(findViewById(R.id.chipTimetable), "timetable".equals(filter));
    }

    private void setChip(TextView chip, boolean selected) {
        if (chip == null) return;
        chip.setBackgroundResource(selected ? R.drawable.cc_chip_selected : R.drawable.cc_chip_unselected);
        chip.setTextColor(ContextCompat.getColor(this, selected ? R.color.white : R.color.cc_text_secondary));
    }

    private boolean matchesFilter(CachedNotification n) {
        if ("all".equals(filter)) return true;
        String t = n.type == null ? "" : n.type.toUpperCase();
        if ("announcements".equals(filter)) return t.contains("ANNOUNCE");
        if ("timetable".equals(filter)) return t.contains("TIMETABLE") || t.contains("CHANGE") || t.contains("REMIND");
        return true;
    }

    private void renderList() {
        final LinearLayout container = findViewById(R.id.containerAlerts);
        if (container == null) return;
        container.removeAllViews();
        int shown = 0;
        for (CachedNotification n : allItems) {
            if (!matchesFilter(n)) continue;
            container.addView(notificationRow(n));
            shown++;
        }
        if (shown == 0) container.addView(placeholder("No notifications here yet."));
        if (!ConnectivityMonitor.getInstance(this).isOnline()) {
            NetBanner.show(this, container, ConnectivityMonitor.State.OFFLINE);
        }
    }

    /** Replace the hardcoded sample alerts with GET /api/notifications. */
    private void loadNotifications() {
        final LinearLayout container = findViewById(R.id.containerAlerts);
        if (container == null) return;

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        CacheRepo.loadNotifications(this, api, new CacheRepo.Cb<CachedNotification>() {
            @Override public void onData(List<CachedNotification> items, boolean fromCache) {
                Skeleton.finish(findViewById(R.id.skeletonList), container);
                allItems.clear();
                if (items != null) allItems.addAll(items);
                renderList();
            }
            @Override public void onError(String m) {
                Skeleton.finish(findViewById(R.id.skeletonList), container);
                renderList();
            }
        });
    }

    /** Pick an icon + tint for a notification type. Returns {iconRes, chipBgRes, tintColorRes}. */
    private int[] visualsFor(String type) {
        String t = type == null ? "" : type.toUpperCase();
        if (t.contains("ANNOUNCE"))
            return new int[]{R.drawable.ic_ms_campaign, R.drawable.cc_iconchip_blue, R.color.cc_accent_blue};
        if (t.contains("TIMETABLE") || t.contains("CHANGE"))
            return new int[]{R.drawable.ic_ms_event_repeat, R.drawable.cc_iconchip_amber, R.color.cc_warning_strong};
        if (t.contains("APPROV") || t.contains("ASSIGN"))
            return new int[]{R.drawable.ic_ms_task_alt, R.drawable.cc_iconchip_green, R.color.cc_success};
        if (t.contains("REJECT") || t.contains("REMOV"))
            return new int[]{R.drawable.ic_ms_fact_check, R.drawable.cc_iconchip_crimson, R.color.cc_crimson};
        return new int[]{R.drawable.ic_ms_fact_check, R.drawable.cc_iconchip_blue, R.color.cc_accent_blue};
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView placeholder(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF64748B);
        tv.setTextSize(13);
        tv.setPadding(dp(4), dp(8), dp(4), dp(8));
        return tv;
    }

    private View notificationRow(CachedNotification it) {
        int[] vis = visualsFor(it.type);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        row.setLayoutParams(lp);
        row.setBackgroundResource(R.drawable.cc_card_bordered);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));

        // Tinted icon chip
        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(40), dp(40));
        icon.setLayoutParams(ilp);
        icon.setBackgroundResource(vis[1]);
        icon.setPadding(dp(9), dp(9), dp(9), dp(9));
        icon.setImageResource(vis[0]);
        icon.setColorFilter(ContextCompat.getColor(this, vis[2]));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(12);
        texts.setLayoutParams(tlp);

        // Title row with unread dot
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(it.title);
        title.setTextColor(0xFF0F172A);
        title.setTextSize(14);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleLp);
        titleRow.addView(title);

        if (!it.is_read) {
            View dot = new View(this);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(8), dp(8));
            dlp.leftMargin = dp(8);
            dot.setLayoutParams(dlp);
            dot.setBackgroundResource(R.drawable.cc_unread_dot);
            titleRow.addView(dot);
        }

        TextView body = new TextView(this);
        body.setText(it.body);
        body.setTextColor(0xFF64748B);
        body.setTextSize(12);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(4);
        body.setLayoutParams(blp);

        TextView ts = new TextView(this);
        ts.setText(formatTime(it.created_at));
        ts.setTextColor(0xFF64748B);
        ts.setTextSize(11);
        LinearLayout.LayoutParams tslp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tslp.topMargin = dp(6);
        ts.setLayoutParams(tslp);

        texts.addView(titleRow);
        texts.addView(body);
        texts.addView(ts);

        row.addView(icon);
        row.addView(texts);
        return row;
    }

    /** "2026-10-11T17:59:50.000Z" -> "2026-10-11 17:59:50" */
    private static String formatTime(String iso) {
        if (iso == null) return "";
        String s = iso.replace('T', ' ');
        int dot = s.indexOf('.');
        if (dot > 0) s = s.substring(0, dot);
        return s.replace("Z", "").trim();
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
        if (navSettings != null) navSettings.setOnClickListener(v -> navigateTo(SettingsActivity.class));
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
