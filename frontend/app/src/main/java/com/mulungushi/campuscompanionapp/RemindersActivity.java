package com.mulungushi.campuscompanionapp;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanionapp.data.local.CachedReminder;
import com.mulungushi.campuscompanionapp.data.repo.CacheRepo;
import com.mulungushi.campuscompanionapp.network.ApiBodies;
import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.ConnectivityMonitor;
import com.mulungushi.campuscompanionapp.network.MessageResponse;
import com.mulungushi.campuscompanionapp.network.SessionManager;
import com.mulungushi.campuscompanionapp.util.NetBanner;
import com.mulungushi.campuscompanionapp.util.Ui;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RemindersActivity extends BaseActivity {

    private LinearLayout root, list;
    private ApiService api;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        api = ApiClient.getClient(this).create(ApiService.class);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.PAGE_BG);
        root = Ui.vbox(this);
        sv.addView(root);
        setContentView(sv);

        root.addView(Ui.header(this, "Reminders"));

        SessionManager sm = SessionManager.getInstance(this);
        if (sm.isLecturer() || sm.isGroupLeader()) {
            android.widget.Button add = Ui.button(this, "+ New reminder");
            int m = Ui.dp(this, 16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(m, 0, m, m);
            add.setLayoutParams(lp);
            add.setOnClickListener(v -> showCreateDialog());
            root.addView(add);
        }

        list = Ui.vbox(this);
        int p = Ui.dp(this, 16);
        list.setPadding(p, 0, p, p);
        root.addView(list);

        reload();
    }

    private void reload() {
        CacheRepo.loadReminders(this, api, new CacheRepo.Cb<CachedReminder>() {
            @Override public void onData(List<CachedReminder> items, boolean fromCache) { render(items); }
            @Override public void onError(String m) { /* keep cache shown */ }
        });
    }

    private void render(List<CachedReminder> items) {
        list.removeAllViews();
        if (items == null || items.isEmpty()) { list.addView(Ui.placeholder(this, "No reminders yet.")); return; }
        for (CachedReminder r : items) {
            LinearLayout card = Ui.card(this);
            card.addView(Ui.text(this, r.title == null ? "" : r.title, Ui.NAVY, 14, true));
            if (r.body != null && !r.body.isEmpty()) card.addView(Ui.text(this, r.body, Ui.MUTED, 12, false));
            String meta = (r.scope == null ? "" : r.scope) + (r.remind_at != null ? " · " + r.remind_at.replace('T', ' ') : "");
            card.addView(Ui.text(this, meta, Ui.RED, 10, false));
            list.addView(card);
        }
    }

    private void showCreateDialog() {
        boolean lecturer = SessionManager.getInstance(this).isLecturer();
        LinearLayout box = Ui.vbox(this);
        int p = Ui.dp(this, 16);
        box.setPadding(p, p, p, 0);
        final EditText title = new EditText(this); title.setHint("Title");
        final EditText body = new EditText(this); body.setHint("Details (optional)");
        final Spinner scope = new Spinner(this);
        String[] scopes = lecturer ? new String[]{"group", "programme", "all", "personal"} : new String[]{"group", "personal"};
        scope.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, scopes));
        final EditText extra = new EditText(this);
        extra.setHint(lecturer ? "group_id (for group) / CS|IT|DS (for programme)" : "(group uses your own group)");
        box.addView(Ui.text(this, "Scope", Ui.MUTED, 12, false));
        box.addView(scope);
        box.addView(title); box.addView(body);
        if (lecturer) box.addView(extra);

        new AlertDialog.Builder(this)
                .setTitle("New reminder")
                .setView(box)
                .setPositiveButton("Create", (d, w) -> {
                    ApiBodies.CreateReminder cr = new ApiBodies.CreateReminder();
                    cr.title = title.getText().toString().trim();
                    cr.body = body.getText().toString().trim();
                    cr.scope = (String) scope.getSelectedItem();
                    if (lecturer) {
                        String ex = extra.getText().toString().trim();
                        if ("group".equals(cr.scope) && ex.matches("\\d+")) cr.target_group_id = Integer.parseInt(ex);
                        if ("programme".equals(cr.scope)) cr.target_programme = ex.toUpperCase();
                    }
                    if (cr.title.length() < 2) { Toast.makeText(this, "Title too short", Toast.LENGTH_SHORT).show(); return; }
                    api.createReminder(cr).enqueue(new Callback<MessageResponse>() {
                        @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                            if (r.isSuccessful()) { Toast.makeText(RemindersActivity.this, "Reminder created", Toast.LENGTH_SHORT).show(); reload(); }
                            else Toast.makeText(RemindersActivity.this, r.body() != null && r.body().error != null ? r.body().error : "Failed (" + r.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(Call<MessageResponse> c, Throwable t) { Toast.makeText(RemindersActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
