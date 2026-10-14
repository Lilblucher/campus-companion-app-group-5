package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.Gson;
import com.mulungushi.campuscompanionapp.network.ApiBodies;
import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.GroupMembersResponse;
import com.mulungushi.campuscompanionapp.network.GroupsResponse;
import com.mulungushi.campuscompanionapp.network.MessageResponse;
import com.mulungushi.campuscompanionapp.network.RequestsResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageGroupsActivity extends BaseActivity {

    private final List<GroupsResponse.Group> groups = new ArrayList<>();
    private GroupCardAdapter adapter;
    private ApiService api;
    private boolean actionInFlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_groups);

        api = ApiClient.getClient(this).create(ApiService.class);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        View btnInbox = findViewById(R.id.btnInbox);
        if (btnInbox != null) btnInbox.setOnClickListener(v -> startActivity(new Intent(this, PendingRequestsActivity.class)));

        View btnAssign = findViewById(R.id.btnCreateGroup);
        if (btnAssign != null) btnAssign.setOnClickListener(v -> showCreateGroupDialog());

        ListView listView = findViewById(R.id.listViewGroups);
        adapter = new GroupCardAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < groups.size()) showGroupOptionsDialog(groups.get(position));
        });

        loadGroups();
        loadPendingBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGroups();
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

    // ---- Create Group ----

    private void showCreateGroupDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        layout.setPadding(p, dp(12), p, 0);

        EditText etName = new EditText(this);
        etName.setHint("Group name (e.g. G05)");
        etName.setFilters(new android.text.InputFilter[]{ new android.text.InputFilter.LengthFilter(10) });
        layout.addView(etName);

        EditText etMax = new EditText(this);
        etMax.setHint("Max members (1-15)");
        etMax.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etMax.setText("15");
        layout.addView(etMax);

        new AlertDialog.Builder(this)
                .setTitle("Create new group")
                .setView(layout)
                .setPositiveButton("Create", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) { Toast.makeText(this, "Enter a group name", Toast.LENGTH_SHORT).show(); return; }
                    int max;
                    try { max = Integer.parseInt(etMax.getText().toString().trim()); }
                    catch (NumberFormatException e) { max = 15; }
                    if (max < 1 || max > 15) { Toast.makeText(this, "Max members must be 1-15", Toast.LENGTH_SHORT).show(); return; }
                    doCreateGroup(name, max);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void doCreateGroup(String name, int max) {
        if (actionInFlight) return;
        actionInFlight = true;
        api.createLecturerGroup(new ApiBodies.CreateGroup(name, max)).enqueue(new Callback<MessageResponse>() {
            @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                actionInFlight = false;
                if (r.isSuccessful()) {
                    Toast.makeText(ManageGroupsActivity.this, "Group " + name + " created", Toast.LENGTH_SHORT).show();
                    loadGroups();
                } else {
                    String msg = "Couldn't create group";
                    try {
                        MessageResponse body = r.errorBody() != null
                                ? new Gson().fromJson(r.errorBody().charStream(), MessageResponse.class) : null;
                        if (body != null && body.error != null) msg = body.error;
                    } catch (Exception ignored) {}
                    Toast.makeText(ManageGroupsActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<MessageResponse> c, Throwable t) {
                actionInFlight = false;
                Toast.makeText(ManageGroupsActivity.this, "Couldn't reach the server. Check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---- Group Options (tap a group card) ----

    private void showGroupOptionsDialog(GroupsResponse.Group grp) {
        String[] options = { "Assign group leader", "View members", "Delete group" };
        new AlertDialog.Builder(this)
                .setTitle("Lab Group " + grp.group_name)
                .setItems(options, (d, which) -> {
                    switch (which) {
                        case 0: showAssignLeaderDialog(grp); break;
                        case 1: showGroupMembersDialog(grp); break;
                        case 2: confirmDeleteGroup(grp); break;
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---- Delete Group ----

    private void confirmDeleteGroup(GroupsResponse.Group grp) {
        if (grp.active_members > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Can't delete " + grp.group_name)
                    .setMessage("This group still has " + grp.active_members + " active member"
                            + (grp.active_members == 1 ? "" : "s")
                            + ". Remove or reassign all members first.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete " + grp.group_name + "?")
                .setMessage("This will permanently delete the empty group.")
                .setPositiveButton("Delete", (d, w) -> doDeleteGroup(grp))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void doDeleteGroup(GroupsResponse.Group grp) {
        if (actionInFlight) return;
        actionInFlight = true;
        api.deleteLecturerGroup(grp.group_id).enqueue(new Callback<MessageResponse>() {
            @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                actionInFlight = false;
                if (r.isSuccessful()) {
                    Toast.makeText(ManageGroupsActivity.this, grp.group_name + " deleted", Toast.LENGTH_SHORT).show();
                    loadGroups();
                } else {
                    String msg = "Couldn't delete group";
                    try {
                        MessageResponse body = r.errorBody() != null
                                ? new Gson().fromJson(r.errorBody().charStream(), MessageResponse.class) : null;
                        if (body != null && body.error != null) msg = body.error;
                    } catch (Exception ignored) {}
                    Toast.makeText(ManageGroupsActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<MessageResponse> c, Throwable t) {
                actionInFlight = false;
                Toast.makeText(ManageGroupsActivity.this, "Couldn't reach the server. Check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---- View Members ----

    private void showGroupMembersDialog(GroupsResponse.Group grp) {
        api.getGroupMembersForLecturer(grp.group_id).enqueue(new Callback<GroupMembersResponse>() {
            @Override public void onResponse(Call<GroupMembersResponse> c, Response<GroupMembersResponse> r) {
                if (!r.isSuccessful() || r.body() == null || r.body().members == null || r.body().members.isEmpty()) {
                    Toast.makeText(ManageGroupsActivity.this, "No members in this group", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<GroupMembersResponse.Member> members = r.body().members;
                StringBuilder sb = new StringBuilder();
                for (GroupMembersResponse.Member m : members) {
                    if (m.isLeader()) sb.append("★ ");
                    sb.append(m.name).append("  (").append(m.student_number).append(")");
                    if (m.isLeader()) sb.append("  - Leader");
                    sb.append("\n");
                }
                new AlertDialog.Builder(ManageGroupsActivity.this)
                        .setTitle(grp.group_name + " Members (" + members.size() + "/" + grp.max_members + ")")
                        .setMessage(sb.toString().trim())
                        .setPositiveButton("OK", null)
                        .show();
            }
            @Override public void onFailure(Call<GroupMembersResponse> c, Throwable t) {
                Toast.makeText(ManageGroupsActivity.this, "Couldn't load members", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---- Assign Group Leader ----

    private void showAssignLeaderDialog(GroupsResponse.Group grp) {
        api.getGroupMembersForLecturer(grp.group_id).enqueue(new Callback<GroupMembersResponse>() {
            @Override public void onResponse(Call<GroupMembersResponse> c, Response<GroupMembersResponse> r) {
                if (!r.isSuccessful() || r.body() == null || r.body().members == null || r.body().members.isEmpty()) {
                    Toast.makeText(ManageGroupsActivity.this, "No members in this group to assign as leader", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<GroupMembersResponse.Member> members = r.body().members;
                String[] names = new String[members.size()];
                for (int i = 0; i < members.size(); i++) {
                    names[i] = members.get(i).name + " (" + members.get(i).student_number + ")"
                            + (members.get(i).isLeader() ? " - current leader" : "");
                }
                final int[] chosen = {-1};
                new AlertDialog.Builder(ManageGroupsActivity.this)
                        .setTitle("Assign leader for " + grp.group_name)
                        .setSingleChoiceItems(names, -1, (d, which) -> chosen[0] = which)
                        .setPositiveButton("Set as leader", (d, w) -> {
                            if (chosen[0] < 0) { Toast.makeText(ManageGroupsActivity.this, "Select a student first", Toast.LENGTH_SHORT).show(); return; }
                            GroupMembersResponse.Member m = members.get(chosen[0]);
                            doSetLeader(grp, m);
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
            @Override public void onFailure(Call<GroupMembersResponse> c, Throwable t) {
                Toast.makeText(ManageGroupsActivity.this, "Couldn't load members", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doSetLeader(GroupsResponse.Group grp, GroupMembersResponse.Member m) {
        if (actionInFlight) return;
        actionInFlight = true;
        api.setGroupLeader(grp.group_id, new ApiBodies.SetLeader(m.student_id)).enqueue(new Callback<MessageResponse>() {
            @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                actionInFlight = false;
                if (r.isSuccessful()) {
                    Toast.makeText(ManageGroupsActivity.this, m.name + " is now leader of " + grp.group_name, Toast.LENGTH_SHORT).show();
                } else {
                    String msg = "Couldn't set group leader";
                    try {
                        MessageResponse body = r.errorBody() != null
                                ? new Gson().fromJson(r.errorBody().charStream(), MessageResponse.class) : null;
                        if (body != null && body.error != null) msg = body.error;
                    } catch (Exception ignored) {}
                    Toast.makeText(ManageGroupsActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<MessageResponse> c, Throwable t) {
                actionInFlight = false;
                Toast.makeText(ManageGroupsActivity.this, "Couldn't reach the server. Check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---- Adapter ----

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

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
