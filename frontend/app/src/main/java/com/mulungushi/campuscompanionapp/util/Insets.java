package com.mulungushi.campuscompanionapp.util;

import android.app.Activity;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Edge-to-edge helper (redesign handoff §Global notes).
 *
 * Draws under the system bars, then pushes the status-bar inset onto a header
 * view as top padding and the navigation-bar inset onto the bottom nav (and any
 * scroll containers) as bottom padding, so content never sits under the
 * 3-button nav bar or the gesture handle.
 */
public final class Insets {
    private Insets() {}

    /**
     * @param activity   the host activity (its window goes edge-to-edge)
     * @param header     view to receive the top (status bar) inset as padding; may be null
     * @param bottomNav  view to receive the bottom (nav bar) inset as padding; may be null
     */
    public static void apply(final Activity activity, final View header, final View bottomNav) {
        if (activity == null) return;
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        final View root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        final int headerTop = header != null ? header.getPaddingTop() : 0;
        final int navBottom = bottomNav != null ? bottomNav.getPaddingBottom() : 0;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            androidx.core.graphics.Insets bars =
                    windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (header != null) {
                header.setPadding(header.getPaddingLeft(), headerTop + bars.top,
                        header.getPaddingRight(), header.getPaddingBottom());
            }
            if (bottomNav != null) {
                bottomNav.setPadding(bottomNav.getPaddingLeft(), bottomNav.getPaddingTop(),
                        bottomNav.getPaddingRight(), navBottom + bars.bottom);
            }
            return windowInsets;
        });
    }
}
