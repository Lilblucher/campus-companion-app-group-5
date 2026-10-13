package com.mulungushi.campuscompanionapp.util;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.mulungushi.campuscompanionapp.R;

/**
 * Drives the skeleton-loading states described in the redesign handoff.
 *
 * Usage:
 *   Skeleton.start(skeletonView);                 // show + pulse while LOADING
 *   Skeleton.finish(skeletonView, contentView);   // crossfade (150ms) to real content
 */
public final class Skeleton {
    private Skeleton() {}

    /** Make the skeleton visible and start the shared pulse animation. */
    public static void start(View skeleton) {
        if (skeleton == null) return;
        skeleton.setVisibility(View.VISIBLE);
        skeleton.setAlpha(1f);
        Animation pulse = AnimationUtils.loadAnimation(skeleton.getContext(), R.anim.skeleton_pulse);
        skeleton.startAnimation(pulse);
    }

    /** Stop the pulse, fade the skeleton out and the content in over 150ms. */
    public static void finish(final View skeleton, final View content) {
        if (content != null) {
            content.setVisibility(View.VISIBLE);
            content.setAlpha(0f);
            content.animate().alpha(1f).setDuration(150).start();
        }
        if (skeleton != null) {
            skeleton.clearAnimation();
            skeleton.animate().alpha(0f).setDuration(150).withEndAction(new Runnable() {
                @Override public void run() { skeleton.setVisibility(View.GONE); }
            }).start();
        }
    }

    /** Immediately hide the skeleton with no animation (e.g. on error). */
    public static void hide(View skeleton) {
        if (skeleton == null) return;
        skeleton.clearAnimation();
        skeleton.setVisibility(View.GONE);
    }
}
