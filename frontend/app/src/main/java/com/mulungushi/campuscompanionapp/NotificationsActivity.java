package com.mulungushi.campuscompanionapp;

<<<<<<< Updated upstream
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    public enum NotificationType {
        SUCCESS,
        REJECTED,
        MESSAGE
    }

    public static class NotificationItem {
        public String title;
        public String description;
        public String timestamp;
        public String category; // "System" or "Requests"
        public NotificationType type;

        public NotificationItem(String title, String description, String timestamp, String category, NotificationType type) {
            this.title = title;
            this.description = description;
            this.timestamp = timestamp;
            this.category = category;
            this.type = type;
        }
    }

    private final List<NotificationItem> allNotifications = new ArrayList<>();
    private final List<NotificationItem> filteredNotifications = new ArrayList<>();

    private NotificationCardAdapter adapter;
    private TextView tabAll, tabSystem, tabRequests;
    private String selectedCategory = "All";
=======
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

public class NotificationsActivity extends AppCompatActivity {

    private View navDashboard, navGroup, navProfile, navAlerts, navSettings;
>>>>>>> Stashed changes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

<<<<<<< Updated upstream
        initSampleData();

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tabAll = findViewById(R.id.tabAll);
        tabSystem = findViewById(R.id.tabSystem);
        tabRequests = findViewById(R.id.tabRequests);

        ListView listView = findViewById(R.id.listViewNotifications);
        adapter = new NotificationCardAdapter();
        listView.setAdapter(adapter);

        tabAll.setOnClickListener(v -> selectTab("All"));
        tabSystem.setOnClickListener(v -> selectTab("System"));
        tabRequests.setOnClickListener(v -> selectTab("Requests"));

        selectTab("All");

        listView.setOnItemClickListener((parent, view, position, id) -> {
            NotificationItem item = filteredNotifications.get(position);
            Toast.makeText(this, "Notification: " + item.title, Toast.LENGTH_SHORT).show();
        });
    }

    private void initSampleData() {
        allNotifications.add(new NotificationItem(
                "Group change request approved",
                "Your request has been approved",
                "10 Apr 2025 - 09:34",
                "Requests",
                NotificationType.SUCCESS
        ));
        allNotifications.add(new NotificationItem(
                "New message from Lecturer",
                "You have a new message",
                "09 Apr 2025 - 14:52",
                "System",
                NotificationType.MESSAGE
        ));
        allNotifications.add(new NotificationItem(
                "Registration verification",
                "Your account has been verified",
                "08 Apr 2025 - 11:12",
                "System",
                NotificationType.SUCCESS
        ));
        allNotifications.add(new NotificationItem(
                "Group change request rejected",
                "Your request has been rejected",
                "06 Apr 2025 - 10:41",
                "Requests",
                NotificationType.REJECTED
        ));
    }

    private void selectTab(String category) {
        selectedCategory = category;

        updateTabStyle(tabAll, category.equals("All"));
        updateTabStyle(tabSystem, category.equals("System"));
        updateTabStyle(tabRequests, category.equals("Requests"));

        filteredNotifications.clear();
        for (NotificationItem item : allNotifications) {
            if (category.equals("All") || item.category.equalsIgnoreCase(category)) {
                filteredNotifications.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateTabStyle(TextView tab, boolean isSelected) {
        if (isSelected) {
            tab.setBackgroundResource(R.drawable.bg_tab_selected);
            tab.setTextColor(0xFFFFFFFF);
        } else {
            tab.setBackgroundResource(R.drawable.bg_tab_unselected);
            tab.setTextColor(0xFF374151);
        }
    }

    private class NotificationCardAdapter extends BaseAdapter {
        @Override public int getCount() { return filteredNotifications.size(); }
        @Override public Object getItem(int position) { return filteredNotifications.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(NotificationsActivity.this)
                        .inflate(R.layout.item_notification_card, parent, false);
            }

            NotificationItem item = filteredNotifications.get(position);

            ImageView ivIcon = convertView.findViewById(R.id.ivNotificationIcon);
            TextView tvTitle = convertView.findViewById(R.id.tvNotificationTitle);
            TextView tvDesc = convertView.findViewById(R.id.tvNotificationDesc);
            TextView tvTimestamp = convertView.findViewById(R.id.tvNotificationTimestamp);

            tvTitle.setText(item.title);
            tvDesc.setText(item.description);
            tvTimestamp.setText(item.timestamp);

            switch (item.type) {
                case SUCCESS:
                    ivIcon.setImageResource(R.drawable.ic_check);
                    break;
                case REJECTED:
                    ivIcon.setImageResource(R.drawable.ic_close_red);
                    break;
                case MESSAGE:
                    ivIcon.setImageResource(R.drawable.ic_message_blue);
                    break;
            }

            return convertView;
        }
    }
}
=======
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        setupBottomNav();
    }

    private void setupBottomNav() {
        navDashboard = findViewById(R.id.navDashboard);
        navGroup = findViewById(R.id.navGroup);
        navProfile = findViewById(R.id.navProfile);
        navAlerts = findViewById(R.id.navAlerts);
        navSettings = findViewById(R.id.navSettings);

        if (navDashboard != null) navDashboard.setOnClickListener(v -> navigateTo(MainActivity.class));
        if (navGroup != null) navGroup.setOnClickListener(v -> navigateTo(MyGroupActivity.class));
        if (navProfile != null) navProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));
        if (navAlerts != null) selectNav(navAlerts);
        if (navSettings != null) navSettings.setOnClickListener(v -> navigateTo(SettingsActivity.class));

        selectNav(navAlerts);
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
>>>>>>> Stashed changes
