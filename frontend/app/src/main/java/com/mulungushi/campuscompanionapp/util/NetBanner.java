package com.mulungushi.campuscompanionapp.util;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mulungushi.campuscompanionapp.network.ConnectivityMonitor;

/** A thin status strip at the top of a screen: offline / slow / (hidden when good). */
public final class NetBanner {
    private NetBanner() {}
    private static final String TAG = "cr_net_banner";

    public static void show(Context c, LinearLayout root, ConnectivityMonitor.State s) {
        if (root == null) return;
        View old = root.findViewWithTag(TAG);
        if (old != null) root.removeView(old);
        if (s == ConnectivityMonitor.State.GOOD) return;

        TextView t = new TextView(c);
        t.setTag(TAG);
        boolean offline = s == ConnectivityMonitor.State.OFFLINE;
        t.setText(offline ? "Offline · showing saved data" : "Slow connection");
        t.setTextColor(0xFFFFFFFF);
        t.setBackgroundColor(offline ? 0xFF6B7280 : 0xFFB45309);
        int p = Math.round(8 * c.getResources().getDisplayMetrics().density);
        t.setPadding(p, p, p, p);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(12);
        root.addView(t, 0);
    }
}
