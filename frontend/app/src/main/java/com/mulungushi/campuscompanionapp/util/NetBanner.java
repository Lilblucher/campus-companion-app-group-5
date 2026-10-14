package com.mulungushi.campuscompanionapp.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mulungushi.campuscompanionapp.network.ConnectivityMonitor;

/**
 * A thin status strip at the top of a screen: offline / slow / back online / (hidden when good).
 * Works with any ViewGroup root (FrameLayout, LinearLayout, ConstraintLayout).
 */
public final class NetBanner {
    private NetBanner() {}
    private static final String TAG = "cr_net_banner";
    private static final Handler H = new Handler(Looper.getMainLooper());

    public static void show(Context c, ViewGroup root, ConnectivityMonitor.State s) {
        if (root == null) return;
        View old = root.findViewWithTag(TAG);
        if (old != null) root.removeView(old);
        if (s == ConnectivityMonitor.State.GOOD) return;

        TextView t = makeBanner(c);
        boolean offline = s == ConnectivityMonitor.State.OFFLINE;
        t.setText(offline ? "You're offline. Some features may not work." : "Slow connection");
        t.setBackgroundColor(offline ? 0xFFDC2626 : 0xFFD97706);
        addToRoot(root, t);
        animateIn(t);
    }

    public static void showBackOnline(Context c, ViewGroup root) {
        if (root == null) return;
        View old = root.findViewWithTag(TAG);
        if (old != null) root.removeView(old);

        TextView t = makeBanner(c);
        t.setText("Back online");
        t.setBackgroundColor(0xFF16A34A);
        addToRoot(root, t);
        animateIn(t);
        H.postDelayed(() -> animateOut(t), 2500);
    }

    private static TextView makeBanner(Context c) {
        TextView t = new TextView(c);
        t.setTag(TAG);
        t.setTextColor(0xFFFFFFFF);
        int p = Math.round(10 * c.getResources().getDisplayMetrics().density);
        int ph = Math.round(16 * c.getResources().getDisplayMetrics().density);
        t.setPadding(ph, p, ph, p);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(13);
        t.setAlpha(0f);
        return t;
    }

    private static void addToRoot(ViewGroup root, TextView t) {
        if (root instanceof LinearLayout) {
            root.addView(t, 0);
        } else {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.TOP;
            root.addView(t, lp);
        }
    }

    private static void animateIn(View v) {
        v.setTranslationY(-v.getLayoutParams().height);
        v.animate().alpha(1f).translationY(0f).setDuration(250).start();
    }

    private static void animateOut(View v) {
        v.animate().alpha(0f).setDuration(300).setListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                ViewGroup p = (ViewGroup) v.getParent();
                if (p != null) p.removeView(v);
            }
        }).start();
    }
}
