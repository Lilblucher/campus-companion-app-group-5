package com.mulungushi.campuscompanionapp.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.mulungushi.campuscompanionapp.NotificationsActivity;
import com.mulungushi.campuscompanionapp.R;
import com.mulungushi.campuscompanionapp.SettingsActivity;

/**
 * Real, system-level heads-up notifications for new Alerts — the Settings >
 * Push Notifications switch previously only saved a SharedPreferences boolean
 * with nothing reading it, so toggling it changed nothing observable. Now:
 *   - ensureChannel() creates a real high-importance NotificationChannel.
 *   - maybeNotifyNewAlerts() is called from NavBadge (the one place that
 *     already polls GET /api/notifications on every screen's onResume) and
 *     posts a heads-up notification the first time it sees the unread count
 *     go UP since the last check, but only while the switch is on and the
 *     app actually has permission to post notifications.
 */
public final class AppNotifications {
    private AppNotifications() {}

    private static final String CHANNEL_ID = "campus_companion_alerts";
    private static final int NOTIFICATION_ID = 1001;
    private static final String KEY_LAST_SEEN_UNREAD = "pref_last_seen_unread";

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Alerts", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("New announcements, timetable changes and group updates");
        nm.createNotificationChannel(channel);
    }

    /** True only when the in-app switch is on AND the app is actually allowed
     *  to post notifications (runtime permission on API 33+, plus whatever the
     *  user set in system Settings for this app/channel). */
    public static boolean notificationsAllowed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(SettingsActivity.KEY_NOTIFICATIONS, true)) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                   != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /** Call after every GET /api/notifications poll with the freshly-read
     *  unread_count. Fires a heads-up notification only on an increase since
     *  the last call (so it never re-fires for the same unread backlog on
     *  every screen's onResume), and treats the very first call on a device
     *  (no stored baseline yet) as "nothing new" so a user with existing
     *  alerts isn't greeted with a notification burst on first login. */
    public static void maybeNotifyNewAlerts(Context context, int unreadCount) {
        SharedPreferences prefs = context.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE);
        int lastSeen = prefs.getInt(KEY_LAST_SEEN_UNREAD, -1);
        prefs.edit().putInt(KEY_LAST_SEEN_UNREAD, unreadCount).apply();

        if (lastSeen < 0 || unreadCount <= lastSeen) return; // first run, or no new alerts
        int delta = unreadCount - lastSeen;
        notifyNow(context, delta);
    }

    private static void notifyNow(Context context, int newCount) {
        if (!notificationsAllowed(context)) return;
        ensureChannel(context);

        Intent openAlerts = new Intent(context, NotificationsActivity.class);
        openAlerts.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openAlerts, flags);

        String title = newCount == 1 ? "New alert" : newCount + " new alerts";
        String body = newCount == 1
                ? "You have a new alert in Campus Companion."
                : "You have " + newCount + " new alerts in Campus Companion.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException ignored) {
            // Permission was revoked between the allowed-check above and here; drop silently.
        }
    }
}
