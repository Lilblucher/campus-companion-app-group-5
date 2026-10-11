package com.mulungushi.campuscompanionapp;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

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