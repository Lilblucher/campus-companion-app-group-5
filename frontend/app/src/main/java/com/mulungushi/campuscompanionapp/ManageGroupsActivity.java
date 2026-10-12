package com.mulungushi.campuscompanionapp;

import android.content.Intent;
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
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.GroupsResponse;
import com.mulungushi.campuscompanionapp.network.RequestsResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageGroupsActivity extends AppCompatActivity {

    private final List<GroupsResponse.Group> groups = new ArrayList<>();
    private GroupCardAdapter adapter;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_groups);

        api = ApiClient.getClient(this).create(ApiService.class);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        View btnInbox = findViewById(R.id.btnInbox);
        if (btnInbox != null) btnInbox.setOnClickListener(v -> startActivity(new Intent(this, PendingRequestsActivity.class)));

        View btnAssign = findViewById(R.id.btnCreateGroup);
        if (btnAssign != null) btnAssign.setOnClickListener(v -> startActivity(new Intent(this, LecturerStudentsActivity.class)));

        ListView listView = findViewById(R.id.listViewGroups);
        adapter = new GroupCardAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) ->
                startActivity(new Intent(this, LecturerStudentsActivity.class)));

        loadGroups();
        loadPendingBadge();
    }

    private void loadGroups() {
        api.getGroups().enqueue(new Callback<GroupsResponse>() {
            @Override public void onResponse(Call<GroupsResponse> c, Response<GroupsResponse> r) {
                groups.clear();
                if (r.isSuccessful() && r.body() != null && r.body().groups != null) groups.addAll(r.body().groups);
                adapter.notifyDataSetChanged();
            }
            @Override public void onFailure(Call<GroupsResponse> c, Throwable t) {
                Toast.makeText(ManageGroupsActivity.this, "Couldn't load groups right now", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPendingBadge() {
        api.getRequests("pending").enqueue(new Callback<RequestsResponse>() {
            @Override public void onResponse(Call<RequestsResponse> c, Response<RequestsResponse> r) {
                int n = (r.isSuccessful() && r.body() != null && r.body().requests != null) ? r.body().requests.size() : 0;
                TextView badge = findViewById(R.id.tvInboxBadge);
                if (badge != null) {
                    badge.setText(String.valueOf(n));
                    badge.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void onFailure(Call<RequestsResponse> c, Throwable t) {}
        });
    }

    private class GroupCardAdapter extends BaseAdapter {
        @Override public int getCount() { return groups.size(); }
        @Override public Object getItem(int position) { return groups.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ManageGroupsActivity.this)
                        .inflate(R.layout.item_group_card, parent, false);
            }

            GroupsResponse.Group g = groups.get(position);

            TextView name = convertView.findViewById(R.id.tvGroupCodeBadge);
            TextView cap = convertView.findViewById(R.id.tvMemberCapacity);
            TextView seats = convertView.findViewById(R.id.tvGroupDepartment);
            LinearProgressIndicator bar = convertView.findViewById(R.id.progressGroup);

            name.setText(g.group_name);
            cap.setText(g.active_members + "/" + g.max_members);
            bar.setMax(g.max_members);
            bar.setProgressCompat(g.active_members, false);

            int left = g.places_left;
            int colorRes;
            if (left <= 0) {
                seats.setText("Full");
                colorRes = R.color.cc_crimson;
            } else {
                seats.setText(left + (left == 1 ? " seat left" : " seats left"));
                colorRes = g.active_members >= 12 ? R.color.cc_warning_strong : R.color.cc_accent_blue;
            }
            bar.setIndicatorColor(ContextCompat.getColor(ManageGroupsActivity.this, colorRes));

            return convertView;
        }
    }
}
