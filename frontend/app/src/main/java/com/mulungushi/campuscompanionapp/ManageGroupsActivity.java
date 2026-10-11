package com.mulungushi.campuscompanionapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ManageGroupsActivity extends AppCompatActivity {

    public static class GroupItem {
        public String groupCode;
        public String department;
        public int currentMembers;
        public int maxMembers;

        public GroupItem(String groupCode, String department, int currentMembers, int maxMembers) {
            this.groupCode = groupCode;
            this.department = department;
            this.currentMembers = currentMembers;
            this.maxMembers = maxMembers;
        }

        public String getCapacityText() {
            return currentMembers + "/" + maxMembers;
        }
    }

    private final List<GroupItem> allGroups = new ArrayList<>();
    private final List<GroupItem> filteredGroups = new ArrayList<>();
    private GroupCardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_groups);

        initSampleData();

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        EditText etSearchGroups = findViewById(R.id.etSearchGroups);
        ListView listView = findViewById(R.id.listViewGroups);
        Button btnCreateGroup = findViewById(R.id.btnCreateGroup);

        adapter = new GroupCardAdapter();
        listView.setAdapter(adapter);

        applyFilter("");

        etSearchGroups.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnCreateGroup.setOnClickListener(v ->
            Toast.makeText(this, "Create Group dialog opened", Toast.LENGTH_SHORT).show()
        );

        listView.setOnItemClickListener((parent, view, position, id) -> {
            GroupItem selected = filteredGroups.get(position);
            Toast.makeText(this, "Managing Group: " + selected.groupCode, Toast.LENGTH_SHORT).show();
        });
    }

    private void initSampleData() {
        allGroups.add(new GroupItem("CS-2024-1", "Computer Science", 24, 30));
        allGroups.add(new GroupItem("CS-2024-2", "Computer Science", 18, 30));
        allGroups.add(new GroupItem("IT-2024-1", "Information Technology", 20, 30));
        allGroups.add(new GroupItem("SE-2024-1", "Software Engineering", 28, 30));
        allGroups.add(new GroupItem("IS-2024-1", "Information Systems", 15, 25));
    }

    private void applyFilter(String query) {
        filteredGroups.clear();
        String lowerQuery = query.toLowerCase();
        for (GroupItem item : allGroups) {
            if (lowerQuery.isEmpty()
                    || item.groupCode.toLowerCase().contains(lowerQuery)
                    || item.department.toLowerCase().contains(lowerQuery)) {
                filteredGroups.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private class GroupCardAdapter extends BaseAdapter {
        @Override public int getCount() { return filteredGroups.size(); }
        @Override public Object getItem(int position) { return filteredGroups.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ManageGroupsActivity.this)
                        .inflate(R.layout.item_group_card, parent, false);
            }

            GroupItem item = filteredGroups.get(position);

            TextView tvGroupCodeBadge = convertView.findViewById(R.id.tvGroupCodeBadge);
            TextView tvGroupDepartment = convertView.findViewById(R.id.tvGroupDepartment);
            TextView tvMemberCapacity = convertView.findViewById(R.id.tvMemberCapacity);

            tvGroupCodeBadge.setText(item.groupCode);
            tvGroupDepartment.setText(item.department);
            tvMemberCapacity.setText(item.getCapacityText());

            return convertView;
        }
    }
}