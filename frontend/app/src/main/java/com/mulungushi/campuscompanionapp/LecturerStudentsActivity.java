package com.mulungushi.campuscompanionapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanionapp.network.ApiBodies;
import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.GroupsResponse;
import com.mulungushi.campuscompanionapp.network.MessageResponse;
import com.mulungushi.campuscompanionapp.network.RosterResponse;
import com.mulungushi.campuscompanionapp.util.FileShare;
import com.mulungushi.campuscompanionapp.util.Ui;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LecturerStudentsActivity extends AppCompatActivity {

    private ApiService api;
    private LinearLayout groupsBox, list;
    private EditText search;
    private Spinner groupFilter, progFilter;
    private final List<GroupsResponse.Group> groups = new ArrayList<>();
    private static final String[] GROUP_OPTS = {"All groups", "Unassigned", "G01", "G02", "G03", "G04"};
    private static final String[] PROG_OPTS = {"All", "CS", "IT", "DS"};

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        api = ApiClient.getClient(this).create(ApiService.class);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.PAGE_BG);
        LinearLayout root = Ui.vbox(this);
        sv.addView(root);
        setContentView(sv);
        int p = Ui.dp(this, 16);

        root.addView(Ui.header(this, "Students"));
        groupsBox = Ui.vbox(this); groupsBox.setPadding(p, 0, p, 0); root.addView(groupsBox);

        // filters
        LinearLayout bar = Ui.vbox(this); bar.setPadding(p, p, p, 0);
        search = new EditText(this); search.setHint("Search name or student number");
        groupFilter = new Spinner(this); groupFilter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, GROUP_OPTS));
        progFilter = new Spinner(this); progFilter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, PROG_OPTS));
        Button go = Ui.button(this, "Apply filters");
        go.setOnClickListener(v -> loadRoster());
        Button add = Ui.button(this, "+ Add student");
        add.setOnClickListener(v -> showAddDialog());
        bar.addView(search); bar.addView(Ui.text(this, "Group", Ui.MUTED, 12, false)); bar.addView(groupFilter);
        bar.addView(Ui.text(this, "Programme", Ui.MUTED, 12, false)); bar.addView(progFilter);
        bar.addView(go); bar.addView(add);
        root.addView(bar);

        list = Ui.vbox(this); list.setPadding(p, p, p, p); root.addView(list);

        loadGroups();
        loadRoster();
    }

    private void loadGroups() {
        api.getGroups().enqueue(new Callback<GroupsResponse>() {
            @Override public void onResponse(Call<GroupsResponse> c, Response<GroupsResponse> r) {
                groupsBox.removeAllViews();
                groups.clear();
                if (!r.isSuccessful() || r.body() == null || r.body().groups == null) return;
                groups.addAll(r.body().groups);
                for (GroupsResponse.Group g : groups) {
                    LinearLayout card = Ui.card(LecturerStudentsActivity.this);
                    card.addView(Ui.text(LecturerStudentsActivity.this, g.group_name + "  (" + g.active_members + "/" + g.max_members + ")", Ui.NAVY, 14, true));
                    Button exp = Ui.button(LecturerStudentsActivity.this, "Export " + g.group_name + " .xlsx");
                    final int gid = g.group_id;
                    final String gname = g.group_name;
                    exp.setOnClickListener(v -> api.exportGroupXlsx(gid).enqueue(new Callback<ResponseBody>() {
                        @Override public void onResponse(Call<ResponseBody> cc, Response<ResponseBody> rr) {
                            if (rr.isSuccessful() && rr.body() != null) FileShare.saveAndShare(LecturerStudentsActivity.this, rr.body(), gname + "-members.xlsx");
                            else Toast.makeText(LecturerStudentsActivity.this, "Export failed (" + rr.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(Call<ResponseBody> cc, Throwable t) { Toast.makeText(LecturerStudentsActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
                    }));
                    card.addView(exp);
                    groupsBox.addView(card);
                }
            }
            @Override public void onFailure(Call<GroupsResponse> c, Throwable t) {}
        });
    }

    private void loadRoster() {
        String q = search.getText().toString().trim();
        int gi = groupFilter.getSelectedItemPosition();
        Boolean unassigned = gi == 1 ? Boolean.TRUE : null;
        String group = (gi >= 2) ? GROUP_OPTS[gi] : null;
        int pi = progFilter.getSelectedItemPosition();
        String prog = pi >= 1 ? PROG_OPTS[pi] : null;

        list.removeAllViews();
        list.addView(Ui.placeholder(this, "Loading…"));
        api.getRoster(q.isEmpty() ? null : q, prog, group, unassigned, 100, 0).enqueue(new Callback<RosterResponse>() {
            @Override public void onResponse(Call<RosterResponse> c, Response<RosterResponse> r) {
                list.removeAllViews();
                if (!r.isSuccessful() || r.body() == null || r.body().students == null) { list.addView(Ui.placeholder(LecturerStudentsActivity.this, "Couldn't load students.")); return; }
                if (r.body().students.isEmpty()) { list.addView(Ui.placeholder(LecturerStudentsActivity.this, "No students match.")); return; }
                for (RosterResponse.Student st : r.body().students) list.addView(studentCard(st));
            }
            @Override public void onFailure(Call<RosterResponse> c, Throwable t) { list.removeAllViews(); list.addView(Ui.placeholder(LecturerStudentsActivity.this, "Network error.")); }
        });
    }

    private View studentCard(RosterResponse.Student st) {
        LinearLayout card = Ui.card(this);
        String nm = st.name + (st.is_group_leader == 1 ? "  ★ leader" : "");
        card.addView(Ui.text(this, nm, Ui.NAVY, 14, true));
        card.addView(Ui.text(this, st.student_number + " · " + st.programme_code + " · " + (st.lab_group == null ? "Unassigned" : st.lab_group) + " · " + st.status, Ui.MUTED, 12, false));
        Button actions = Ui.button(this, "Manage");
        actions.setOnClickListener(v -> showActions(st));
        card.addView(actions);
        return card;
    }

    private void showActions(RosterResponse.Student st) {
        new AlertDialog.Builder(this)
                .setTitle(st.name)
                .setItems(new String[]{"Assign / transfer group", "Edit details", "Delete (soft)"}, (d, which) -> {
                    if (which == 0) showAssignDialog(st);
                    else if (which == 1) showEditDialog(st);
                    else confirmDelete(st);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAssignDialog(RosterResponse.Student st) {
        final List<String> labels = new ArrayList<>();
        final List<Integer> ids = new ArrayList<>();
        labels.add("Unassign"); ids.add(null);
        for (GroupsResponse.Group g : groups) { labels.add(g.group_name + " (" + g.places_left + " free)"); ids.add(g.group_id); }
        new AlertDialog.Builder(this)
                .setTitle("Assign " + st.name)
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    api.assignStudent(st.student_id, new ApiBodies.Assign(ids.get(which))).enqueue(new Callback<MessageResponse>() {
                        @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                            if (r.isSuccessful()) { Toast.makeText(LecturerStudentsActivity.this, "Updated", Toast.LENGTH_SHORT).show(); loadGroups(); loadRoster(); }
                            else Toast.makeText(LecturerStudentsActivity.this, r.body() != null && r.body().error != null ? r.body().error : "Failed", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(Call<MessageResponse> c, Throwable t) { Toast.makeText(LecturerStudentsActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showEditDialog(RosterResponse.Student st) {
        LinearLayout box = Ui.vbox(this); int p = Ui.dp(this, 16); box.setPadding(p, p, p, 0);
        final EditText name = new EditText(this); name.setHint("Name"); name.setText(st.name);
        final EditText prog = new EditText(this); prog.setHint("Programme CS/IT/DS"); prog.setText(st.programme_code);
        final EditText num = new EditText(this); num.setHint("Student number"); num.setText(st.student_number);
        box.addView(name); box.addView(prog); box.addView(num);
        new AlertDialog.Builder(this).setTitle("Edit student").setView(box)
                .setPositiveButton("Save", (d, w) -> {
                    ApiBodies.EditStudent b = new ApiBodies.EditStudent();
                    b.name = name.getText().toString().trim();
                    b.programme_code = prog.getText().toString().trim().toUpperCase();
                    b.student_number = num.getText().toString().trim();
                    api.editStudent(st.student_id, b).enqueue(new Callback<MessageResponse>() {
                        @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                            if (r.isSuccessful()) { Toast.makeText(LecturerStudentsActivity.this, "Saved", Toast.LENGTH_SHORT).show(); loadRoster(); }
                            else Toast.makeText(LecturerStudentsActivity.this, r.body() != null && r.body().error != null ? r.body().error : "Failed", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(Call<MessageResponse> c, Throwable t) { Toast.makeText(LecturerStudentsActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
                    });
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void confirmDelete(RosterResponse.Student st) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + st.name + "?")
                .setMessage("Soft-deletes the record, releases their group place and locks the account. The student number stays reserved.")
                .setPositiveButton("Delete", (d, w) -> api.deleteStudent(st.student_id).enqueue(new Callback<MessageResponse>() {
                    @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                        if (r.isSuccessful()) { Toast.makeText(LecturerStudentsActivity.this, "Deleted", Toast.LENGTH_SHORT).show(); loadGroups(); loadRoster(); }
                        else Toast.makeText(LecturerStudentsActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(Call<MessageResponse> c, Throwable t) { Toast.makeText(LecturerStudentsActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
                }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAddDialog() {
        LinearLayout box = Ui.vbox(this); int p = Ui.dp(this, 16); box.setPadding(p, p, p, 0);
        final EditText name = new EditText(this); name.setHint("Name");
        final EditText num = new EditText(this); num.setHint("Student number (9 digits)");
        final EditText prog = new EditText(this); prog.setHint("Programme CS/IT/DS");
        final EditText email = new EditText(this); email.setHint("Email (optional)");
        final EditText phone = new EditText(this); phone.setHint("Phone (optional)");
        box.addView(name); box.addView(num); box.addView(prog); box.addView(email); box.addView(phone);
        new AlertDialog.Builder(this).setTitle("Add student").setView(box)
                .setPositiveButton("Create", (d, w) -> {
                    ApiBodies.CreateStudent b = new ApiBodies.CreateStudent(
                            name.getText().toString().trim(), num.getText().toString().trim(),
                            prog.getText().toString().trim().toUpperCase(),
                            email.getText().toString().trim(), phone.getText().toString().trim());
                    api.createStudent(b).enqueue(new Callback<MessageResponse>() {
                        @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                            if (r.isSuccessful() && r.body() != null) {
                                new AlertDialog.Builder(LecturerStudentsActivity.this)
                                        .setTitle("Student created")
                                        .setMessage("Claim code for the student to register with:\n\n" + r.body().claim_code)
                                        .setPositiveButton(android.R.string.ok, null).show();
                                loadRoster();
                            } else Toast.makeText(LecturerStudentsActivity.this, r.body() != null && r.body().error != null ? r.body().error : "Failed", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(Call<MessageResponse> c, Throwable t) { Toast.makeText(LecturerStudentsActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
                    });
                }).setNegativeButton(android.R.string.cancel, null).show();
    }
}
