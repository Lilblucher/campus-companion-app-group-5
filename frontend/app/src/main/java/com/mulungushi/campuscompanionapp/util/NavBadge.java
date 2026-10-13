package com.mulungushi.campuscompanionapp.util;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.mulungushi.campuscompanionapp.R;
import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.NotificationsResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Keeps the Alerts bottom-nav badge (tvAlertBadge) showing the REAL number of
 * unread notifications from GET /api/notifications (its unread_count field),
 * instead of a hardcoded value. Hidden when zero; shows "9+" above 9.
 */
public final class NavBadge {
    private NavBadge() {}

    public static void refresh(final Activity activity) {
        if (activity == null) return;
        final TextView badge = activity.findViewById(R.id.tvAlertBadge);
        if (badge == null) return;

        ApiService api = ApiClient.getClient(activity).create(ApiService.class);
        api.getNotifications().enqueue(new Callback<NotificationsResponse>() {
            @Override
            public void onResponse(Call<NotificationsResponse> c, Response<NotificationsResponse> r) {
                int unread = 0;
                if (r.isSuccessful() && r.body() != null) {
                    unread = r.body().getUnreadCount();
                    // Fall back to counting unread items if the server omits the count.
                    if (unread == 0 && r.body().getNotifications() != null) {
                        for (NotificationsResponse.Item it : r.body().getNotifications()) {
                            if (!it.isRead()) unread++;
                        }
                    }
                }
                apply(badge, unread);
            }

            @Override
            public void onFailure(Call<NotificationsResponse> c, Throwable t) {
                apply(badge, 0);
            }
        });
    }

    private static void apply(TextView badge, int unread) {
        if (unread <= 0) {
            badge.setVisibility(View.GONE);
            return;
        }
        badge.setText(unread > 9 ? "9+" : String.valueOf(unread));
        badge.setVisibility(View.VISIBLE);
    }
}
