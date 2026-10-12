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

import com.mulungushi.campuscompanionapp.data.local.CachedTimetable;
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

public class TimetableActivity extends AppCompatActivity {

    static final String[] DAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private LinearLayout root, list;
    private ApiService api;
    private boolean lecturer;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        api = ApiClient.getClient(this).create(ApiService.class);
        lecturer = SessionManager.getInstance(this).isLecturer();

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.PAGE_BG);
        root = Ui.vbox(this);
        sv.addView(root);
        setContentView(sv);

        root.addView(Ui.header(this, "Timetable"));
        if (lecturer) {
            Button add = Ui.button(this, "+ Add class");
            int m = Ui.dp(this, 16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(m, 0, m, m);
            add.setLayoutParams(lp);
            add.setOnClickListener(v -> showAddDialog());
            root.addView(add);
        }
        list = Ui.vbox(this);
        int p = Ui.dp(this, 16);
        list.setPadding(p, 0, p, p);
        root.addView(list);

        ConnectivityMonitor.getInstance(this).probe(st -> NetBanner.show(this, root, st));
        reload();
    }

    private void reload() {
        CacheRepo.loadTimetable(this, api, new CacheRepo.Cb<CachedTimetable>() {
            @Override public void onData(List<CachedTimetable> items, boolean fromCache) { render(items); }
            @Override public void onError(String m) {}
        });
    }

    private String fmt(String t) { return t == null ? "" : (t.length() >= 5 ? t.substring(0, 5) : t); }

    private void render(List<CachedTimetable> items) {
        list.removeAllViews();
        if (items == null || items.isEmpty()) { list.addView(Ui.placeholder(this, "No classes scheduled yet.")); return; }
        for (CachedTimetable t : items) {
            LinearLayout card = Ui.card(this);
            card.addView(Ui.text(this, t.course_code + " · " + t.title, Ui.NAVY, 14, true));
            String day = (t.day_of_week >= 0 && t.day_of_week < 7) ? DAYS[t.day_of_week] : "";
            card.addView(Ui.text(this, day + "  " + fmt(t.start_time) + "-" + fmt(t.end_time), Ui.MUTED, 12, false));
            String where = (t.venue == null ? "" : t.venue) + (t.room != null ? " · " + t.room : "") + (t.group_name != null ? " · " + t.group_name : " · All groups");
            card.addView(Ui.text(this, where, Ui.MUTED, 11, false));
            if (lecturer) {
                Button del = Ui.button(this, "Delete");
                final long id = t.session_id;
                del.setOnClickListener(v -> api.deleteSession(id).enqueue(new Callback<MessageResponse>() {
                    @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) { reload(); }
                    @Override public void onFailure(Call<MessageResponse> c, Throwable th) { Toast.makeText(TimetableActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
                }));
                card.addView(del);
            }
            list.addView(card);
        }
    }

    private void showAddDialog() {
        LinearLayout box = Ui.vbox(this);
        int p = Ui.dp(this, 16); box.setPadding(p, p, p, 0);
        final EditText code = new EditText(this); code.setHint("Course code (e.g. ICT361)");
        final EditText title = new EditText(this); title.setHint("Title");
        final Spinner day = new Spinner(this);
        day.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, DAYS));
        final EditText start = new EditText(this); start.setHint("Start HH:MM (e.g. 10:00)");
        final EditText end = new EditText(this); end.setHint("End HH:MM");
        final EditText group = new EditText(this); group.setHint("group_id (blank = all groups)");
        final EditText venue = new EditText(this); venue.setHint("Venue");
        final EditText room = new EditText(this); room.setHint("Room");
        for (View v : new View[]{code, title, Ui.text(this, "Day", Ui.MUTED, 12, false), day, start, end, group, venue, room}) box.addView(v);

        new AlertDialog.Builder(this)
                .setTitle("Add class")
                .setView(box)
                .setPositiveButton("Add", (d, w) -> {
                    ApiBodies.Session body = new ApiBodies.Session();
                    body.course_code = code.getText().toString().trim();
                    body.title = title.getText().toString().trim();
                    body.day_of_week = day.getSelectedItemPosition();
                    body.start_time = start.getText().toString().trim();
                    body.end_time = end.getText().toString().trim();
                    String g = group.getText().toString().trim();
                    if (g.matches("\\d+")) body.group_id = Integer.parseInt(g);
                    body.venue = venue.getText().toString().trim();
                    body.room = room.getText().toString().trim();
                    api.createSession(body).enqueue(new Callback<MessageResponse>() {
                        @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                            if (r.isSuccessful()) { Toast.makeText(TimetableActivity.this, "Class added", Toast.LENGTH_SHORT).show(); reload(); }
                            else Toast.makeText(TimetableActivity.this, r.body() != null && r.body().error != null ? r.body().error : "Failed (" + r.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(Call<MessageResponse> c, Throwable t) { Toast.makeText(TimetableActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
