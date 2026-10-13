package com.mulungushi.campuscompanionapp.data.repo;

import android.content.Context;

import com.mulungushi.campuscompanionapp.data.local.AppDatabase;
import com.mulungushi.campuscompanionapp.data.local.CacheDao;
import com.mulungushi.campuscompanionapp.data.local.CachedNotification;
import com.mulungushi.campuscompanionapp.data.local.CachedReminder;
import com.mulungushi.campuscompanionapp.data.local.CachedTimetable;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.ConnectivityMonitor;
import com.mulungushi.campuscompanionapp.network.NotificationsResponse;
import com.mulungushi.campuscompanionapp.network.RemindersResponse;
import com.mulungushi.campuscompanionapp.network.TimetableResponse;
import com.mulungushi.campuscompanionapp.util.Bg;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Offline-first reads: emit the Room cache immediately, then (when online)
 * refresh from the API, upsert the cache, and re-emit. On a network failure the
 * cached copy stays on screen. Room runs off the main thread via Bg.io.
 */
public final class CacheRepo {
    private CacheRepo() {}

    public interface Cb<T> {
        void onData(List<T> items, boolean fromCache);
        void onError(String message);
    }

    // ---------------- reminders ----------------
    public static void loadReminders(Context ctx, ApiService api, Cb<CachedReminder> cb) {
        final CacheDao dao = AppDatabase.getInstance(ctx).cacheDao();
        Bg.io(() -> {
            final List<CachedReminder> cached = dao.getReminders();
            Bg.main(() -> cb.onData(cached, true));
            if (!ConnectivityMonitor.getInstance(ctx).isOnline()) return;
            api.getReminders().enqueue(new Callback<RemindersResponse>() {
                @Override public void onResponse(Call<RemindersResponse> c, Response<RemindersResponse> r) {
                    if (!r.isSuccessful() || r.body() == null || r.body().reminders == null) { cb.onError("update failed"); return; }
                    final List<CachedReminder> fresh = new ArrayList<>();
                    long now = System.currentTimeMillis();
                    for (RemindersResponse.Reminder x : r.body().reminders) {
                        CachedReminder cr = new CachedReminder();
                        cr.reminder_id = x.reminder_id; cr.title = x.title; cr.body = x.body; cr.scope = x.scope;
                        cr.creator_role = x.creator_role; cr.remind_at = x.remind_at; cr.created_at = x.created_at; cr.cached_at = now;
                        fresh.add(cr);
                    }
                    Bg.io(() -> { dao.clearReminders(); dao.putReminders(fresh); Bg.main(() -> cb.onData(fresh, false)); });
                }
                @Override public void onFailure(Call<RemindersResponse> c, Throwable t) { cb.onError(t.getMessage()); }
            });
        });
    }

    // ---------------- timetable ----------------
    public static void loadTimetable(Context ctx, ApiService api, Cb<CachedTimetable> cb) {
        final CacheDao dao = AppDatabase.getInstance(ctx).cacheDao();
        Bg.io(() -> {
            final List<CachedTimetable> cached = dao.getTimetable();
            Bg.main(() -> cb.onData(cached, true));
            if (!ConnectivityMonitor.getInstance(ctx).isOnline()) return;
            api.getTimetable().enqueue(new Callback<TimetableResponse>() {
                @Override public void onResponse(Call<TimetableResponse> c, Response<TimetableResponse> r) {
                    if (!r.isSuccessful() || r.body() == null || r.body().sessions == null) { cb.onError("update failed"); return; }
                    final List<CachedTimetable> fresh = new ArrayList<>();
                    long now = System.currentTimeMillis();
                    for (TimetableResponse.Session x : r.body().sessions) {
                        CachedTimetable t = new CachedTimetable();
                        t.session_id = x.session_id; t.course_code = x.course_code; t.title = x.title; t.group_name = x.group_name;
                        t.day_of_week = x.day_of_week; t.start_time = x.start_time; t.end_time = x.end_time; t.venue = x.venue; t.room = x.room; t.cached_at = now;
                        fresh.add(t);
                    }
                    Bg.io(() -> { dao.clearTimetable(); dao.putTimetable(fresh); Bg.main(() -> cb.onData(fresh, false)); });
                }
                @Override public void onFailure(Call<TimetableResponse> c, Throwable t) { cb.onError(t.getMessage()); }
            });
        });
    }

    // ---------------- notifications ----------------
    public static void loadNotifications(Context ctx, ApiService api, Cb<CachedNotification> cb) {
        final CacheDao dao = AppDatabase.getInstance(ctx).cacheDao();
        Bg.io(() -> {
            final List<CachedNotification> cached = dao.getNotifications();
            Bg.main(() -> cb.onData(cached, true));
            if (!ConnectivityMonitor.getInstance(ctx).isOnline()) return;
            api.getNotifications().enqueue(new Callback<NotificationsResponse>() {
                @Override public void onResponse(Call<NotificationsResponse> c, Response<NotificationsResponse> r) {
                    if (!r.isSuccessful() || r.body() == null || r.body().getNotifications() == null) { cb.onError("update failed"); return; }
                    final List<CachedNotification> fresh = new ArrayList<>();
                    long now = System.currentTimeMillis();
                    for (NotificationsResponse.Item x : r.body().getNotifications()) {
                        CachedNotification n = new CachedNotification();
                        n.notification_id = x.getNotificationId(); n.type = x.getType(); n.title = x.getTitle();
                        n.body = x.getBody(); n.is_read = x.isRead(); n.created_at = x.getCreatedAt(); n.cached_at = now;
                        fresh.add(n);
                    }
                    Bg.io(() -> { dao.clearNotifications(); dao.putNotifications(fresh); Bg.main(() -> cb.onData(fresh, false)); });
                }
                @Override public void onFailure(Call<NotificationsResponse> c, Throwable t) { cb.onError(t.getMessage()); }
            });
        });
    }
}
