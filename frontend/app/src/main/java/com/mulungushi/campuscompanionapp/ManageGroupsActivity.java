package com.mulungushi.campuscompanionapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ManageGroupsActivity extends AppCompatActivity {

    private final String[] groups = {
            "Group A - ICT361 Project (5 members)",
            "Group B - ICT361 Project (5 members)",
            "Group C - ICT311 Lab Assignment (4 members)",
            "Group D - ICT412 Database Design (6 members)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_groups);

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ListView listView = findViewById(R.id.listViewGroups);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_list_row, groups);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = groups[position];
            Toast.makeText(this, "Managing: " + selected, Toast.LENGTH_SHORT).show();
        });
    }
}
