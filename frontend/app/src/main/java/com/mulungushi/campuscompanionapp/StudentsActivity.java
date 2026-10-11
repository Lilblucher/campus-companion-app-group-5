package com.mulungushi.campuscompanionapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class StudentsActivity extends AppCompatActivity {

    public static class StudentListItem {
        public String name;
        public String studentId;
        public boolean isActive;

        public StudentListItem(String name, String studentId, boolean isActive) {
            this.name = name;
            this.studentId = studentId;
            this.isActive = isActive;
        }

        public String getInitials() {
            if (name == null || name.trim().isEmpty()) return "ST";
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
            }
            return ("" + parts[0].charAt(0)).toUpperCase();
        }
    }

    private final List<StudentListItem> allStudents = new ArrayList<>();
    private final List<StudentListItem> filteredStudents = new ArrayList<>();
    private StudentListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_students);

        initSampleData();

        TextView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        EditText etSearch = findViewById(R.id.etSearchStudents);
        if (etSearch == null) {
            etSearch = findViewById(R.id.etSearchStudent);
        }

        ListView listView = findViewById(R.id.listViewStudents);

        adapter = new StudentListAdapter();
        listView.setAdapter(adapter);

        applyFilter("");

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFilter(s.toString().trim());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        listView.setOnItemClickListener((parent, view, position, id) -> {
            StudentListItem selected = filteredStudents.get(position);
            Toast.makeText(this, "Student: " + selected.name + " (" + selected.studentId + ")", Toast.LENGTH_SHORT).show();
        });
    }

    private void initSampleData() {
        allStudents.add(new StudentListItem("Chishala Kelly", "MU123456", true));
        allStudents.add(new StudentListItem("Mwansa Shapi", "MU123457", true));
        allStudents.add(new StudentListItem("Davis Mukenga", "MU123458", true));
        allStudents.add(new StudentListItem("Twambo Chisanga", "MU123459", true));
        allStudents.add(new StudentListItem("Grace Mwale", "MU123460", false));
    }

    private void applyFilter(String query) {
        filteredStudents.clear();
        String lowerQuery = query.toLowerCase();
        for (StudentListItem item : allStudents) {
            if (lowerQuery.isEmpty()
                    || item.name.toLowerCase().contains(lowerQuery)
                    || item.studentId.toLowerCase().contains(lowerQuery)) {
                filteredStudents.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private class StudentListAdapter extends BaseAdapter {
        @Override public int getCount() { return filteredStudents.size(); }
        @Override public Object getItem(int position) { return filteredStudents.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(StudentsActivity.this)
                        .inflate(R.layout.item_student_card, parent, false);
            }

            StudentListItem item = filteredStudents.get(position);

            TextView tvAvatar = convertView.findViewById(R.id.tvAvatar);
            TextView tvStudentName = convertView.findViewById(R.id.tvStudentName);
            TextView tvStudentSubtext = convertView.findViewById(R.id.tvStudentSubtext);
            TextView tvStatusTag = convertView.findViewById(R.id.tvStatusTag);

            tvAvatar.setText(item.getInitials());
            tvStudentName.setText(item.name);
            tvStudentSubtext.setText(item.studentId);

            if (item.isActive) {
                tvStatusTag.setText("Active");
                tvStatusTag.setBackgroundResource(R.drawable.bg_status_active);
                tvStatusTag.setTextColor(0xFF137333);
            } else {
                tvStatusTag.setText("Inactive");
                tvStatusTag.setBackgroundResource(R.drawable.bg_status_inactive);
                tvStatusTag.setTextColor(0xFFC5221F);
            }

            return convertView;
        }
    }
}