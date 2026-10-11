package com.mulungushi.campuscompanionapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SearchStudentActivity extends AppCompatActivity {

    public static class StudentItem {
        public String name;
        public String studentId;
        public String group;
        public String program;
        public boolean isActive;

        public StudentItem(String name, String studentId, String group, String program, boolean isActive) {
            this.name = name;
            this.studentId = studentId;
            this.group = group;
            this.program = program;
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

    private final List<StudentItem> allStudents = new ArrayList<>();
    private final List<StudentItem> filteredStudents = new ArrayList<>();

    private StudentCardAdapter adapter;
    private TextView tvStudentListHeader;

    private String selectedProgram = "All Programs";
    private String selectedGroup = "All Groups";
    private String currentSearchText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_student);

        initSampleData();

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        EditText etSearchQuery = findViewById(R.id.etSearchQuery);
        Spinner spinnerProgram = findViewById(R.id.spinnerProgram);
        Spinner spinnerLabGroup = findViewById(R.id.spinnerLabGroup);
        tvStudentListHeader = findViewById(R.id.tvStudentListHeader);
        ListView listView = findViewById(R.id.listViewSearchResults);
        ImageButton btnFilterFab = findViewById(R.id.btnFilterFab);

        // Setup Spinners
        String[] programs = {"All Programs", "Computer Science", "Information Technology", "Software Engineering"};
        ArrayAdapter<String> programAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, programs);
        programAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProgram.setAdapter(programAdapter);

        String[] groups = {"All Groups", "CS-2024-1", "CS-2024-2", "IT-2024-1"};
        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, groups);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLabGroup.setAdapter(groupAdapter);

        adapter = new StudentCardAdapter();
        listView.setAdapter(adapter);

        applyFilters();

        etSearchQuery.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchText = s.toString().trim().toLowerCase();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        spinnerProgram.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedProgram = programs[position];
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerLabGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGroup = groups[position];
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnFilterFab.setOnClickListener(v ->
            Toast.makeText(this, "Filters applied: " + selectedProgram + ", " + selectedGroup, Toast.LENGTH_SHORT).show()
        );

        listView.setOnItemClickListener((parent, view, position, id) -> {
            StudentItem selected = filteredStudents.get(position);
            Toast.makeText(this, "Selected: " + selected.name + " (" + selected.studentId + ")", Toast.LENGTH_SHORT).show();
        });
    }

    private void initSampleData() {
        allStudents.add(new StudentItem("Chishala Kelly", "MU123456", "CS-2024-1", "Computer Science", true));
        allStudents.add(new StudentItem("Mwansa Shapi", "MU123457", "CS-2024-1", "Computer Science", true));
        allStudents.add(new StudentItem("Davis Mukenga", "MU123458", "CS-2024-1", "Computer Science", true));
        allStudents.add(new StudentItem("Twambo Chisanga", "MU123459", "CS-2024-1", "Computer Science", true));
        allStudents.add(new StudentItem("Grace Mwale", "MU123460", "CS-2024-2", "Information Technology", false));
        allStudents.add(new StudentItem("John Banda", "MU123461", "IT-2024-1", "Information Technology", true));
        allStudents.add(new StudentItem("Natasha Zimba", "MU123462", "CS-2024-2", "Software Engineering", true));
        allStudents.add(new StudentItem("Kelvin Mulenga", "MU123463", "IT-2024-1", "Information Technology", true));
    }

    private void applyFilters() {
        filteredStudents.clear();
        for (StudentItem item : allStudents) {
            boolean matchesSearch = currentSearchText.isEmpty()
                    || item.name.toLowerCase().contains(currentSearchText)
                    || item.studentId.toLowerCase().contains(currentSearchText)
                    || item.program.toLowerCase().contains(currentSearchText);

            boolean matchesProgram = selectedProgram.equals("All Programs") || item.program.equalsIgnoreCase(selectedProgram);
            boolean matchesGroup = selectedGroup.equals("All Groups") || item.group.equalsIgnoreCase(selectedGroup);

            if (matchesSearch && matchesProgram && matchesGroup) {
                filteredStudents.add(item);
            }
        }
        tvStudentListHeader.setText("Student List (" + filteredStudents.size() + ")");
        adapter.notifyDataSetChanged();
    }

    private class StudentCardAdapter extends BaseAdapter {
        @Override public int getCount() { return filteredStudents.size(); }
        @Override public Object getItem(int position) { return filteredStudents.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(SearchStudentActivity.this)
                        .inflate(R.layout.item_student_card, parent, false);
            }

            StudentItem student = filteredStudents.get(position);

            TextView tvAvatar = convertView.findViewById(R.id.tvAvatar);
            TextView tvStudentName = convertView.findViewById(R.id.tvStudentName);
            TextView tvStudentSubtext = convertView.findViewById(R.id.tvStudentSubtext);
            TextView tvStatusTag = convertView.findViewById(R.id.tvStatusTag);

            tvAvatar.setText(student.getInitials());
            tvStudentName.setText(student.name);
            tvStudentSubtext.setText(student.studentId + " • " + student.group);

            if (student.isActive) {
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