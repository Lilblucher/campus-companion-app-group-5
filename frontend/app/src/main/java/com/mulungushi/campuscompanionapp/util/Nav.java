package com.mulungushi.campuscompanionapp.util;

import android.content.Context;

import com.mulungushi.campuscompanionapp.LecturerDashboardActivity;
import com.mulungushi.campuscompanionapp.LecturerStudentsActivity;
import com.mulungushi.campuscompanionapp.MainActivity;
import com.mulungushi.campuscompanionapp.ManageGroupsActivity;
import com.mulungushi.campuscompanionapp.MyCoursesActivity;
import com.mulungushi.campuscompanionapp.MyGroupActivity;
import com.mulungushi.campuscompanionapp.network.SessionManager;

public final class Nav {
    private Nav() {}

    public static Class<?> homeFor(Context c) {
        return SessionManager.getInstance(c).isStaff()
                ? LecturerDashboardActivity.class
                : MainActivity.class;
    }

    public static Class<?> groupFor(Context c) {
        return SessionManager.getInstance(c).isStaff()
                ? LecturerStudentsActivity.class
                : MyGroupActivity.class;
    }

    // tab 3: "Courses" for students, "Groups" for staff
    public static Class<?> coursesFor(Context c) {
        return SessionManager.getInstance(c).isStaff()
                ? ManageGroupsActivity.class
                : MyCoursesActivity.class;
    }

    public static void adaptBottomNav(android.app.Activity a) {
        if (!SessionManager.getInstance(a).isStaff()) return;
        setTab(a.findViewById(com.mulungushi.campuscompanionapp.R.id.navGroup),
                "Students", com.mulungushi.campuscompanionapp.R.drawable.ic_ms_person_search);
        setTab(a.findViewById(com.mulungushi.campuscompanionapp.R.id.navCourses),
                "Groups", com.mulungushi.campuscompanionapp.R.drawable.ic_ms_groups);
    }

    private static void setTab(android.view.View tab, String label, int icon) {
        if (!(tab instanceof android.view.ViewGroup)) return;
        android.view.ViewGroup g = (android.view.ViewGroup) tab;
        if (g.getChildCount() < 2) return;
        android.view.View iconView = g.getChildAt(0);
        android.view.View textView = g.getChildAt(1);
        if (iconView instanceof android.widget.ImageView)
            ((android.widget.ImageView) iconView).setImageResource(icon);
        if (textView instanceof android.widget.TextView)
            ((android.widget.TextView) textView).setText(label);
    }
}