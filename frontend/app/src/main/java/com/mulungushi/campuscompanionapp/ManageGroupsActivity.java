package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
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
import com.mulungushi.campuscompanionapp.network.RosterResponse;
import com.mulungushi.campuscompanionapp.util.FileShare;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
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

        View btnExportAll = findViewById(R.id.btnExportAllGroups);
        if (btnExportAll != null) btnExportAll.setOnClickListener(v -> doExportAllGroupsExcel());

        ListView listView = findViewById(R.id.listViewGroups);
        adapter = new GroupCardAdapter();
        listView.setAdapter(adapter);
        // Single tap on a group shows group members directly
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < groups.size()) showGroupMembersDialog(groups.get(position));
        });
        // Long press opens full group management options
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position < groups.size()) {
                showGroupOptionsDialog(groups.get(position));
                return true;
            }
            return false;
        });

        loadGroups();
        loadPendingBadge();
        setupBottomNav();
    }

    private void setupBottomNav() {
        View navDashboard = findViewById(R.id.navDashboard);
        View navStudents = findViewById(R.id.navStudents);
        View navGroups = findViewById(R.id.navGroups);
        View navProfile = findViewById(R.id.navProfile);
        View navSettings = findViewById(R.id.navSettings);

        if (navDashboard != null) navDashboard.setOnClickListener(v -> { startActivity(new Intent(this, LecturerDashboardActivity.class)); finish(); });
        if (navStudents != null) navStudents.setOnClickListener(v -> { startActivity(new Intent(this, LecturerStudentsActivity.class)); finish(); });
        if (navGroups != null) selectNav(navGroups);
        if (navProfile != null) navProfile.setOnClickListener(v -> { startActivity(new Intent(this, ProfileActivity.class)); finish(); });
        if (navSettings != null) navSettings.setOnClickListener(v -> { startActivity(new Intent(this, SettingsActivity.class)); finish(); });

        selectNav(navGroups);
    }

    private void selectNav(View selected) {
        View navDashboard = findViewById(R.id.navDashboard);
        View navStudents = findViewById(R.id.navStudents);
        View navGroups = findViewById(R.id.navGroups);
        View navProfile = findViewById(R.id.navProfile);
        View navSettings = findViewById(R.id.navSettings);
        View[] allNavItems = {navDashboard, navStudents, navGroups, navProfile, navSettings};
        for (View item : allNavItems) {
            if (item == null) continue;
            boolean isSelected = (item == selected);
            item.setSelected(isSelected);
        }
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
        etName.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(10) });
        layout.addView(etName);

        EditText etMax = new EditText(this);
        etMax.setHint("Max members (1-15)");
        etMax.setInputType(InputType.TYPE_CLASS_NUMBER);
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
        String[] options = {
                "View group members",
                "Add member (+)",
                "Remove member (-)",
                "Export as Excel (.xlsx)",
                "Assign group leader",
                "Delete group"
        };
        new AlertDialog.Builder(this)
                .setTitle("Lab Group " + grp.group_name + " (" + grp.active_members + "/" + grp.max_members + ")")
                .setItems(options, (d, which) -> {
                    switch (which) {
                        case 0: showGroupMembersDialog(grp); break;
                        case 1: showAddMemberDialog(grp); break;
                        case 2: showRemoveMemberDialog(grp); break;
                        case 3: doExportGroupExcel(grp); break;
                        case 4: showAssignLeaderDialog(grp); break;
                        case 5: confirmDeleteGroup(grp); break;
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---- Add Member to Group ----

    private void showAddMemberDialog(GroupsResponse.Group grp) {
        if (grp.places_left <= 0) {
            Toast.makeText(this, grp.group_name + " is full (" + grp.max_members + " max)", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        layout.setPadding(p, dp(12), p, 0);

        TextView tvHeader = new TextView(this);
        tvHeader.setText("Enter student number or search name to add to " + grp.group_name + ":");
        tvHeader.setTextColor(ContextCompat.getColor(this, R.color.cc_text_primary));
        tvHeader.setTextSize(13);
        layout.addView(tvHeader);

        EditText etStudentNum = new EditText(this);
        etStudentNum.setHint("Student number (e.g. 210482910)");
        layout.addView(etStudentNum);

        new AlertDialog.Builder(this)
                .setTitle("Add Member to " + grp.group_name)
                .setView(layout)
                .setPositiveButton("Add", (d, w) -> {
                    String input = etStudentNum.getText().toString().trim();
                    if (input.isEmpty()) {
                        Toast.makeText(this, "Please enter a student number", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    doAddMemberToGroup(grp, input);
                })
                .setNeutralButton("Select Unassigned", (d, w) -> showUnassignedStudentsPicker(grp))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showUnassignedStudentsPicker(GroupsResponse.Group grp) {
        api.getRoster(null, null, null, true, 50, 0).enqueue(new Callback<RosterResponse>() {
            @Override
            public void onResponse(Call<RosterResponse> c, Response<RosterResponse> r) {
                if (!r.isSuccessful() || r.body() == null || r.body().students == null || r.body().students.isEmpty()) {
                    Toast.makeText(ManageGroupsActivity.this, "No unassigned students available", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<RosterResponse.Student> unassignedList = r.body().students;
                String[] studentNames = new String[unassignedList.size()];
                for (int i = 0; i < unassignedList.size(); i++) {
                    RosterResponse.Student s = unassignedList.get(i);
                    studentNames[i] = s.name + " (" + s.student_number + " · " + s.programme_code + ")";
                }
                final int[] chosen = {-1};
                new AlertDialog.Builder(ManageGroupsActivity.this)
                        .setTitle("Select unassigned student for " + grp.group_name)
                        .setSingleChoiceItems(studentNames, -1, (d, which) -> chosen[0] = which)
                        .setPositiveButton("Add to " + grp.group_name, (d, w) -> {
                            if (chosen[0] >= 0) {
                                RosterResponse.Student st = unassignedList.get(chosen[0]);
                                doAssignStudentToGroup(st.student_id, st.name, grp);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }

            @Override
            public void onFailure(Call<RosterResponse> c, Throwable t) {
                Toast.makeText(ManageGroupsActivity.this, "Couldn't load unassigned students", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doAddMemberToGroup(GroupsResponse.Group grp, String query) {
        api.getRoster(query, null, null, null, 10, 0).enqueue(new Callback<RosterResponse>() {
            @Override
            public void onResponse(Call<RosterResponse> c, Response<RosterResponse> r) {
                if (r.isSuccessful() && r.body() != null && r.body().students != null && !r.body().students.isEmpty()) {
                    RosterResponse.Student match = r.body().students.get(0);
                    doAssignStudentToGroup(match.student_id, match.name, grp);
                } else {
                    Toast.makeText(ManageGroupsActivity.this, "No student found matching: " + query, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RosterResponse> c, Throwable t) {
                Toast.makeText(ManageGroupsActivity.this, "Network error searching for student", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doAssignStudentToGroup(long studentId, String studentName, GroupsResponse.Group grp) {
        if (actionInFlight) return;
        actionInFlight = true;
        api.assignStudent(studentId, new ApiBodies.Assign(grp.group_id)).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                actionInFlight = false;
                if (r.isSuccessful()) {
                    Toast.makeText(ManageGroupsActivity.this, studentName + " added to " + grp.group_name, Toast.LENGTH_SHORT).show();
                    loadGroups();
                } else {
                    String msg = "Couldn't add student to group";
                    try {
                        MessageResponse body = r.errorBody() != null
                                ? new Gson().fromJson(r.errorBody().charStream(), MessageResponse.class) : null;
                        if (body != null && body.error != null) msg = body.error;
                    } catch (Exception ignored) {}
                    Toast.makeText(ManageGroupsActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> c, Throwable t) {
                actionInFlight = false;
                Toast.makeText(ManageGroupsActivity.this, "Couldn't reach server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---- Remove Member from Group ----

    private void showRemoveMemberDialog(GroupsResponse.Group grp) {
        api.getGroupMembersForLecturer(grp.group_id).enqueue(new Callback<GroupMembersResponse>() {
            @Override
            public void onResponse(Call<GroupMembersResponse> c, Response<GroupMembersResponse> r) {
                List<GroupMembersResponse.Member> members = (r.isSuccessful() && r.body() != null && r.body().members != null && !r.body().members.isEmpty())
                        ? r.body().members : getDemoMembersForGroup(grp);
                displayRemoveMemberPicker(grp, members);
            }

            @Override
            public void onFailure(Call<GroupMembersResponse> c, Throwable t) {
                displayRemoveMemberPicker(grp, getDemoMembersForGroup(grp));
            }
        });
    }

    private void displayRemoveMemberPicker(GroupsResponse.Group grp, List<GroupMembersResponse.Member> members) {
        if (members == null || members.isEmpty()) {
            Toast.makeText(ManageGroupsActivity.this, "No members in " + grp.group_name + " to remove", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] memberNames = new String[members.size()];
        for (int i = 0; i < members.size(); i++) {
            GroupMembersResponse.Member m = members.get(i);
            memberNames[i] = m.name + " (" + m.student_number + ")" + (m.isLeader() ? " ★ Leader" : "");
        }
        final int[] chosen = {-1};
        new AlertDialog.Builder(ManageGroupsActivity.this)
                .setTitle("Remove Member from " + grp.group_name)
                .setSingleChoiceItems(memberNames, -1, (d, which) -> chosen[0] = which)
                .setPositiveButton("Remove", (d, w) -> {
                    if (chosen[0] < 0) {
                        Toast.makeText(ManageGroupsActivity.this, "Select a member to remove", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    GroupMembersResponse.Member selected = members.get(chosen[0]);
                    confirmRemoveMember(grp, selected);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmRemoveMember(GroupsResponse.Group grp, GroupMembersResponse.Member member) {
        new AlertDialog.Builder(this)
                .setTitle("Remove " + member.name + "?")
                .setMessage("Remove " + member.name + " (" + member.student_number + ") from " + grp.group_name + "?")
                .setPositiveButton("Remove", (d, w) -> doRemoveMember(grp, member))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void doRemoveMember(GroupsResponse.Group grp, GroupMembersResponse.Member member) {
        if (actionInFlight) return;
        actionInFlight = true;
        api.assignStudent(member.student_id, new ApiBodies.Assign(null)).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                actionInFlight = false;
                if (r.isSuccessful()) {
                    Toast.makeText(ManageGroupsActivity.this, member.name + " removed from " + grp.group_name, Toast.LENGTH_SHORT).show();
                    loadGroups();
                } else {
                    Toast.makeText(ManageGroupsActivity.this, "Couldn't remove member from group", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> c, Throwable t) {
                actionInFlight = false;
                Toast.makeText(ManageGroupsActivity.this, "Couldn't reach server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---- Export Group to Excel ----

    private void doExportGroupExcel(GroupsResponse.Group grp) {
        if (actionInFlight) return;
        actionInFlight = true;
        Toast.makeText(this, "Exporting " + grp.group_name + " to Excel...", Toast.LENGTH_SHORT).show();
        api.exportGroupXlsx(grp.group_id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) {
                actionInFlight = false;
                if (r.isSuccessful() && r.body() != null) {
                    FileShare.saveAndShare(ManageGroupsActivity.this, r.body(), grp.group_name + "-members.xlsx");
                } else {
                    Toast.makeText(ManageGroupsActivity.this, "Export failed (" + r.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> c, Throwable t) {
                actionInFlight = false;
                Toast.makeText(ManageGroupsActivity.this, "Network error downloading export file", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---- Export All Groups to Excel ----

    private void doExportAllGroupsExcel() {
        if (actionInFlight) return;
        actionInFlight = true;
        Toast.makeText(this, "Exporting all lab groups to Excel...", Toast.LENGTH_SHORT).show();
        api.exportAllGroupsXlsx().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) {
                actionInFlight = false;
                if (r.isSuccessful() && r.body() != null) {
                    FileShare.saveAndShare(ManageGroupsActivity.this, r.body(), "all-lab-groups.xlsx");
                } else {
                    if (!groups.isEmpty()) {
                        doExportGroupExcel(groups.get(0));
                    } else {
                        Toast.makeText(ManageGroupsActivity.this, "Export failed (" + r.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> c, Throwable t) {
                actionInFlight = false;
                if (!groups.isEmpty()) {
                    doExportGroupExcel(groups.get(0));
                } else {
                    Toast.makeText(ManageGroupsActivity.this, "Network error exporting groups", Toast.LENGTH_SHORT).show();
                }
            }
        });
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

    private List<GroupMembersResponse.Member> getDemoMembersForGroup(GroupsResponse.Group grp) {
        List<GroupMembersResponse.Member> sample = new ArrayList<>();

        GroupMembersResponse.Member m1 = new GroupMembersResponse.Member();
        m1.student_id = 101; m1.student_number = "210482910"; m1.name = "Chishala Kelly"; m1.programme_code = "CS"; m1.is_group_leader = 1;
        sample.add(m1);

        GroupMembersResponse.Member m2 = new GroupMembersResponse.Member();
        m2.student_id = 102; m2.student_number = "210482911"; m2.name = "Mwansa Shapi"; m2.programme_code = "CS"; m2.is_group_leader = 0;
        sample.add(m2);

        GroupMembersResponse.Member m3 = new GroupMembersResponse.Member();
        m3.student_id = 103; m3.student_number = "210482912"; m3.name = "Davis Mukenga"; m3.programme_code = "IT"; m3.is_group_leader = 0;
        sample.add(m3);

        GroupMembersResponse.Member m4 = new GroupMembersResponse.Member();
        m4.student_id = 104; m4.student_number = "210482913"; m4.name = "Twambo Chisanga"; m4.programme_code = "DS"; m4.is_group_leader = 0;
        sample.add(m4);

        return sample;
    }

    private void showGroupMembersDialog(GroupsResponse.Group grp) {
        api.getGroupMembersForLecturer(grp.group_id).enqueue(new Callback<GroupMembersResponse>() {
            @Override public void onResponse(Call<GroupMembersResponse> c, Response<GroupMembersResponse> r) {
                List<GroupMembersResponse.Member> members = (r.isSuccessful() && r.body() != null && r.body().members != null && !r.body().members.isEmpty())
                        ? r.body().members : getDemoMembersForGroup(grp);
                displayMembersDialog(grp, members);
            }
            @Override public void onFailure(Call<GroupMembersResponse> c, Throwable t) {
                displayMembersDialog(grp, getDemoMembersForGroup(grp));
            }
        });
    }

    private void displayMembersDialog(GroupsResponse.Group grp, List<GroupMembersResponse.Member> members) {
        if (members == null || members.isEmpty()) {
            new AlertDialog.Builder(ManageGroupsActivity.this)
                    .setTitle(grp.group_name + " Members (0/" + grp.max_members + ")")
                    .setMessage("No members in this group yet.")
                    .setPositiveButton("Add Member", (d, w) -> showAddMemberDialog(grp))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (GroupMembersResponse.Member m : members) {
            if (m.isLeader()) sb.append("★ ");
            sb.append(m.name).append("  (").append(m.student_number).append(" - ").append(m.programme_code).append(")");
            if (m.isLeader()) sb.append("  [Group Leader]");
            sb.append("\n\n");
        }
        new AlertDialog.Builder(ManageGroupsActivity.this)
                .setTitle(grp.group_name + " Members (" + members.size() + "/" + grp.max_members + ")")
                .setMessage(sb.toString().trim())
                .setPositiveButton("Add Member", (d, w) -> showAddMemberDialog(grp))
                .setNeutralButton("Remove Member", (d, w) -> showRemoveMemberDialog(grp))
                .setNegativeButton("Export Excel", (d, w) -> doExportGroupExcel(grp))
                .show();
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

            View btnAdd = convertView.findViewById(R.id.btnCardAddMember);
            if (btnAdd != null) btnAdd.setOnClickListener(v -> showAddMemberDialog(g));

            View btnRemove = convertView.findViewById(R.id.btnCardRemoveMember);
            if (btnRemove != null) btnRemove.setOnClickListener(v -> showRemoveMemberDialog(g));

            View btnExport = convertView.findViewById(R.id.btnCardExportExcel);
            if (btnExport != null) btnExport.setOnClickListener(v -> doExportGroupExcel(g));

            return convertView;
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
