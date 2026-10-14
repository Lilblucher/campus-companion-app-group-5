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
    private static final String KEY_IS_GROUP_LEADER = "is_group_leader";
    private static final String KEY_TOKEN_CREATED_AT = "token_created_at";

    private static final long SESSION_LIFETIME_MS = 8 * 60 * 60 * 1000L; // 8 hours

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
        saveUserSession(role, userName, studentId, token, false);
    }

    public void saveUserSession(String role, String userName, String studentId, String token, boolean isGroupLeader) {
        if (sharedPreferences == null) return;
        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_USER_NAME, userName)
                .putString(KEY_STUDENT_ID, studentId)
                .putString(KEY_AUTH_TOKEN, token)
                .putBoolean(KEY_IS_GROUP_LEADER, isGroupLeader)
                .putLong(KEY_TOKEN_CREATED_AT, System.currentTimeMillis())
                .apply();
    }

    public boolean isSessionExpired() {
        if (sharedPreferences == null) return true;
        long createdAt = sharedPreferences.getLong(KEY_TOKEN_CREATED_AT, 0);
        if (createdAt == 0) return true;
        return (System.currentTimeMillis() - createdAt) >= SESSION_LIFETIME_MS;
    }

    public boolean isGroupLeader() {
        return sharedPreferences != null && sharedPreferences.getBoolean(KEY_IS_GROUP_LEADER, false);
    }

    public boolean isLecturer() {
        return "lecturer".equals(getUserRole());
    }

    public boolean isLoggedIn() {
        if (sharedPreferences == null) return false;
        if (!sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)) return false;
        if (isSessionExpired()) {
            clearSession();
            return false;
        }
        return true;
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
