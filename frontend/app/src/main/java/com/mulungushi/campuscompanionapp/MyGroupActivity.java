package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanionapp.network.ApiBodies;
import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.GroupInfo;
import com.mulungushi.campuscompanionapp.network.MessageResponse;
import com.mulungushi.campuscompanionapp.network.SessionManager;
import com.mulungushi.campuscompanionapp.util.FileShare;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyGroupActivity extends AppCompatActivity {

    private View navDashboard, navGroup, navProfile, navAlerts, navSettings;
    private ApiService api;
    private boolean leader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_group);

        api = ApiClient.getClient(this).create(ApiService.class);
        leader = SessionManager.getInstance(this).isGroupLeader();

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        com.mulungushi.campuscompanionapp.util.Skeleton.start(findViewById(R.id.skeletonDetail));

        View btnRequestChange = findViewById(R.id.btnRequestChange);
        if (btnRequestChange != null) btnRequestChange.setOnClickListener(v -> showGroupChangeDialog());

        View btnLecturerMail = findViewById(R.id.btnLecturerMail);
        if (btnLecturerMail != null) btnLecturerMail.setOnClickListener(v -> {
            try {
                Intent email = new Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:"));
                email.putExtra(Intent.EXTRA_SUBJECT, "Lab group enquiry");
                startActivity(Intent.createChooser(email, "Email lecturer"));
            } catch (Exception e) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        });

        setupBottomNav();
        loadGroup();
    }

    private void reveal() {
        com.mulungushi.campuscompanionapp.util.Skeleton.finish(
                findViewById(R.id.skeletonDetail), findViewById(R.id.groupContent));
    }

    /** Dialog: pick a target group (G01–G04) + optional reason, then POST the request. */
    private void showGroupChangeDialog() {
        final String[] groups = {"G01", "G02", "G03", "G04"};
        final int[] chosen = {-1};
        final EditText reason = new EditText(this);
        reason.setHint("Reason (optional)");

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        wrap.setPadding(p, dp(8), p, 0);
        wrap.addView(reason);

        new AlertDialog.Builder(this)
                .setTitle("Request group change")
                .setSingleChoiceItems(groups, -1, (d, which) -> chosen[0] = which)
                .setView(wrap)
                .setPositiveButton("Send request", (d, w) -> {
                    if (chosen[0] < 0) { Toast.makeText(this, "Pick a group first", Toast.LENGTH_SHORT).show(); return; }
                    // Groups G01–G04 map to ids 1–4 (seeded lab_groups).
                    Integer gid = chosen[0] + 1;
                    String r = reason.getText().toString().trim();
                    api.requestGroupChange(new ApiBodies.GroupChangeRequest(gid, r.isEmpty() ? null : r))
                            .enqueue(new Callback<MessageResponse>() {
                                @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> resp) {
                                    if (resp.isSuccessful()) Toast.makeText(MyGroupActivity.this, "Group change request sent to lecturer", Toast.LENGTH_SHORT).show();
                                    else Toast.makeText(MyGroupActivity.this, resp.body() != null && resp.body().error != null ? resp.body().error : "Request failed", Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onFailure(Call<MessageResponse> c, Throwable t) { Toast.makeText(MyGroupActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
                            });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** A leader loads via /api/group/members (carries student_id for remove); a
     *  plain student via /api/students/me/group. */
    private void loadGroup() {
        final ViewGroup container = findViewById(R.id.containerMembers);
        if (container != null) { container.removeAllViews(); }
        Callback<GroupInfo> cb = new Callback<GroupInfo>() {
            @Override public void onResponse(Call<GroupInfo> call, Response<GroupInfo> response) {
                reveal();
                if (!response.isSuccessful() || response.body() == null) {
                    if (container != null) { container.removeAllViews(); container.addView(placeholder("Couldn't load your group right now.")); }
                    return;
                }
                render(response.body());
            }
            @Override public void onFailure(Call<GroupInfo> call, Throwable t) {
                reveal();
                if (container != null) { container.removeAllViews(); container.addView(placeholder("Couldn't load your group. Check your connection.")); }
            }
        };
        if (leader) api.getGroupMembers().enqueue(cb); else api.getMyGroup().enqueue(cb);
    }

    private void render(GroupInfo g) {
        final TextView tvGroupName = findViewById(R.id.tvGroupName);
        final TextView tvMembersHeader = findViewById(R.id.tvMembersHeader);
        final TextView tvMembersPill = findViewById(R.id.tvMembersPill);
        final com.google.android.material.progressindicator.LinearProgressIndicator occ = findViewById(R.id.progressGroupOccupancy);
        final ViewGroup container = findViewById(R.id.containerMembers);
        if (container != null) container.removeAllViews();

        if (!g.isAssigned()) {
            if (tvGroupName != null) tvGroupName.setText("Not assigned yet");
            if (tvMembersPill != null) tvMembersPill.setText("—");
            if (tvMembersHeader != null) tvMembersHeader.setText("Group Members");
            if (container != null) container.addView(placeholder("You'll see your lab group here once a lecturer assigns you."));
            return;
        }
        if (tvGroupName != null) tvGroupName.setText("Lab Group " + g.getGroupName());
        int active = g.getActiveMembers() == null ? 0 : g.getActiveMembers();
        int max = g.getMaxMembers() == null ? 15 : g.getMaxMembers();
        if (tvMembersPill != null) tvMembersPill.setText(active + "/" + max + " members");
        if (occ != null) { occ.setMax(max); occ.setProgressCompat(active, true); }
        if (tvMembersHeader != null) tvMembersHeader.setText("Group Members");

        if (leader && container != null) container.addView(leaderActions());

        final String myName = SessionManager.getInstance(this).getUserName();
        if (container != null && g.getMembers() != null) {
            for (GroupInfo.Member m : g.getMembers()) {
                boolean isMe = myName != null && myName.equalsIgnoreCase(m.getName());
                container.addView(memberRow(m, isMe));
            }
        }
    }

    private View leaderActions() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12); box.setLayoutParams(lp);

        Button add = new Button(this); add.setAllCaps(false); add.setText("+ Add member");
        add.setOnClickListener(v -> showAddMemberDialog());
        Button exp = new Button(this); exp.setAllCaps(false); exp.setText("Export members (.xlsx)");
        exp.setOnClickListener(v -> api.exportMyGroupXlsx().enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) {
                if (r.isSuccessful() && r.body() != null) FileShare.saveAndShare(MyGroupActivity.this, r.body(), "group-members.xlsx");
                else Toast.makeText(MyGroupActivity.this, "Export failed (" + r.code() + ")", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(Call<ResponseBody> c, Throwable t) { Toast.makeText(MyGroupActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
        }));
        Button send = new Button(this); send.setAllCaps(false); send.setText("Send roster to lecturer");
        send.setOnClickListener(v -> api.sendRosterToLecturer().enqueue(new Callback<MessageResponse>() {
            @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                Toast.makeText(MyGroupActivity.this, r.isSuccessful() ? "Sent to lecturer" : "Failed", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(Call<MessageResponse> c, Throwable t) { Toast.makeText(MyGroupActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
        }));
        box.addView(add); box.addView(exp); box.addView(send);
        return box;
    }

    private void showAddMemberDialog() {
        final EditText input = new EditText(this);
        input.setHint("Student number (9 digits)");
        new AlertDialog.Builder(this)
                .setTitle("Add member to your group")
                .setView(input)
                .setPositiveButton("Add", (d, w) -> {
                    String sn = input.getText().toString().trim();
                    api.addMember(new ApiBodies.AddMember(sn)).enqueue(new Callback<MessageResponse>() {
                        @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                            if (r.isSuccessful()) { Toast.makeText(MyGroupActivity.this, "Member added", Toast.LENGTH_SHORT).show(); loadGroup(); }
                            else Toast.makeText(MyGroupActivity.this, r.body() != null && r.body().error != null ? r.body().error : "Failed (" + r.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(Call<MessageResponse> c, Throwable t) { Toast.makeText(MyGroupActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void removeMember(long studentId) {
        api.removeMember(studentId).enqueue(new Callback<MessageResponse>() {
            @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                if (r.isSuccessful()) { Toast.makeText(MyGroupActivity.this, "Removed", Toast.LENGTH_SHORT).show(); loadGroup(); }
                else Toast.makeText(MyGroupActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(Call<MessageResponse> c, Throwable t) { Toast.makeText(MyGroupActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
        });
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView placeholder(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF64748B);
        tv.setTextSize(13);
        tv.setPadding(dp(4), dp(8), dp(4), dp(8));
        return tv;
    }

    private View memberRow(GroupInfo.Member m, boolean isMe) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        row.setLayoutParams(lp);
        row.setBackgroundResource(R.drawable.cc_card_bordered);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));

        // Avatar with initials
        TextView avatar = new TextView(this);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(dp(40), dp(40));
        avatar.setLayoutParams(alp);
        avatar.setGravity(android.view.Gravity.CENTER);
        avatar.setText(initialsOf(m.getName()));
        avatar.setTextColor(0xFFFFFFFF);
        avatar.setTextSize(13);
        avatar.setTypeface(avatar.getTypeface(), Typeface.BOLD);
        avatar.setBackgroundResource(isMe ? R.drawable.cc_avatar_crimson : R.drawable.cc_avatar_navy);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(12);
        texts.setLayoutParams(tlp);

        TextView name = new TextView(this);
        name.setText(isMe ? m.getName() + " (You)" : m.getName());
        name.setTextColor(0xFF0F172A);
        name.setTextSize(14);
        name.setTypeface(name.getTypeface(), Typeface.BOLD);

        TextView sub = new TextView(this);
        String prog = m.getProgrammeCode() == null ? "" : " · " + m.getProgrammeCode();
        sub.setText("Student ID: " + m.getStudentNumber() + prog);
        sub.setTextColor(0xFF64748B);
        sub.setTextSize(12);

        texts.addView(name);
        texts.addView(sub);

        row.addView(avatar);
        row.addView(texts);

        // A leader can remove any member except themselves.
        if (leader && !isMe && m.getStudentId() > 0) {
            android.widget.ImageButton remove = new android.widget.ImageButton(this);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(dp(44), dp(44));
            remove.setLayoutParams(rlp);
            remove.setImageResource(R.drawable.ic_ms_delete);
            remove.setColorFilter(0xFFB30D25);
            remove.setBackgroundResource(android.R.color.transparent);
            remove.setContentDescription("Remove member");
            final long sid = m.getStudentId();
            remove.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Remove " + m.getName() + "?")
                    .setPositiveButton("Remove", (d, w) -> removeMember(sid))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show());
            row.addView(remove);
        }
        return row;
    }

    private static String initialsOf(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        String s = parts[0].substring(0, 1);
        if (parts.length > 1) s += parts[parts.length - 1].substring(0, 1);
        return s.toUpperCase();
    }

    private void setupBottomNav() {
        navDashboard = findViewById(R.id.navDashboard);
        navGroup = findViewById(R.id.navGroup);
        navProfile = findViewById(R.id.navProfile);
        navAlerts = findViewById(R.id.navAlerts);
        navSettings = findViewById(R.id.navSettings);

        if (navDashboard != null) navDashboard.setOnClickListener(v -> navigateTo(MainActivity.class));
        if (navGroup != null) selectNav(navGroup);
        if (navProfile != null) navProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));
        if (navAlerts != null) navAlerts.setOnClickListener(v -> navigateTo(NotificationsActivity.class));
        if (navSettings != null) navSettings.setOnClickListener(v -> navigateTo(SettingsActivity.class));

        selectNav(navGroup);
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.mulungushi.campuscompanionapp.util.NavBadge.refresh(this);
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
