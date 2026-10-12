package com.mulungushi.campuscompanionapp.util;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mulungushi.campuscompanionapp.R;

/** Small helpers for building the data screens programmatically. */
public final class Ui {
    private Ui() {}
    public static final int NAVY = 0xFF022F77;
    public static final int MUTED = 0xFF64748B;
    public static final int RED = 0xFFE51937;
    public static final int TEXT = 0xFF0F172A;
    public static final int PAGE_BG = 0xFFF4F7FC;

    public static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    public static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return l;
    }

    public static TextView text(Context c, String s, int color, float size, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextColor(color); t.setTextSize(size);
        if (bold) t.setTypeface(t.getTypeface(), Typeface.BOLD);
        return t;
    }

    public static Button button(Context c, String s) {
        Button b = new Button(c);
        b.setText(s); b.setAllCaps(false);
        return b;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 12);
        l.setLayoutParams(lp);
        try { l.setBackgroundResource(R.drawable.cc_card_bordered); } catch (Exception ignored) {}
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        return l;
    }

    public static TextView header(Context c, String s) {
        TextView t = text(c, s, TEXT, 22, true);
        int p = dp(c, 16);
        t.setPadding(p, p, p, dp(c, 8));
        return t;
    }

    public static TextView placeholder(Context c, String s) {
        TextView t = text(c, s, MUTED, 13, false);
        int p = dp(c, 8);
        t.setPadding(dp(c, 4), p, dp(c, 4), p);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        return t;
    }
}
