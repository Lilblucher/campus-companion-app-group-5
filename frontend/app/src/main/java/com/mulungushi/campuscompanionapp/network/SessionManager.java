package com.mulungushi.campuscompanionapp.network;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS_NAME = "campus_companion_secure_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_STUDENT_ID = "student_id";

    private static SessionManager instance;
    private final SharedPreferences sharedPreferences;

    private SessionManager(Context context) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void saveUserSession(String role, String userName, String studentId, String token) {
        if (sharedPreferences == null) return;
        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_USER_NAME, userName)
                .putString(KEY_STUDENT_ID, studentId)
                .putString(KEY_AUTH_TOKEN, token)
                .apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences != null && sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getAuthToken() {
        return sharedPreferences != null ? sharedPreferences.getString(KEY_AUTH_TOKEN, "") : "";
    }

    public String getUserRole() {
        return sharedPreferences != null ? sharedPreferences.getString(KEY_USER_ROLE, "student") : "student";
    }

    public String getUserName() {
        return sharedPreferences != null ? sharedPreferences.getString(KEY_USER_NAME, "User") : "User";
    }

    public String getStudentId() {
        return sharedPreferences != null ? sharedPreferences.getString(KEY_STUDENT_ID, "") : "";
    }

    public void clearSession() {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().clear().apply();
    }
}
