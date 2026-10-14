package com.mulungushi.campuscompanionapp.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface ApiService {
    // ---- auth ----
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @POST("api/auth/forgot-password")
    Call<MessageResponse> forgotPassword(@Body ApiBodies.ForgotPassword body);

    @POST("api/auth/reset-password")
    Call<MessageResponse> resetPassword(@Body ApiBodies.ResetPassword body);

    // ---- student self ----
    @GET("api/students/me")
    Call<StudentProfile> getMe();

    @PUT("api/students/me")
    Call<StudentProfile> updateMe(@Body ApiBodies.UpdateSelf body);

    @POST("api/students/me/student-number-correction")
    Call<MessageResponse> requestNumberCorrection(@Body ApiBodies.NumberCorrection body);

    @GET("api/students/me/group")
    Call<GroupInfo> getMyGroup();

    @POST("api/students/me/group-change-request")
    Call<MessageResponse> requestGroupChange(@Body ApiBodies.GroupChangeRequest body);

    @GET("api/students/groups")
    Call<GroupsResponse> getOpenGroups();

    @POST("api/students/me/group/join")
    Call<MessageResponse> joinGroup(@Body ApiBodies.JoinGroup body);

    @GET("api/notifications")
    Call<NotificationsResponse> getNotifications();

    // ---- lecturer panel ----
    @GET("api/lecturer/students")
    Call<RosterResponse> getRoster(@Query("q") String q, @Query("programme") String programme,
                                   @Query("group") String group, @Query("unassigned") Boolean unassigned,
                                   @Query("limit") Integer limit, @Query("offset") Integer offset);

    @POST("api/lecturer/students")
    Call<MessageResponse> createStudent(@Body ApiBodies.CreateStudent body);

    @PATCH("api/lecturer/students/{id}")
    Call<MessageResponse> editStudent(@Path("id") long id, @Body ApiBodies.EditStudent body);

    @DELETE("api/lecturer/students/{id}")
    Call<MessageResponse> deleteStudent(@Path("id") long id);

    @POST("api/lecturer/students/{id}/assign")
    Call<MessageResponse> assignStudent(@Path("id") long id, @Body ApiBodies.Assign body);

    @GET("api/lecturer/groups")
    Call<GroupsResponse> getGroups();

    @POST("api/lecturer/groups")
    Call<MessageResponse> createLecturerGroup(@Body ApiBodies.CreateGroup body);

    @DELETE("api/lecturer/groups/{id}")
    Call<MessageResponse> deleteLecturerGroup(@Path("id") int groupId);

    @GET("api/lecturer/groups/{id}/members")
    Call<GroupMembersResponse> getGroupMembersForLecturer(@Path("id") int groupId);

    @POST("api/lecturer/groups/{id}/leader")
    Call<MessageResponse> setGroupLeader(@Path("id") int groupId, @Body ApiBodies.SetLeader body);

    @Streaming
    @GET("api/lecturer/groups/{id}/export.xlsx")
    Call<ResponseBody> exportGroupXlsx(@Path("id") int groupId);

    @GET("api/lecturer/requests")
    Call<RequestsResponse> getRequests(@Query("status") String status);

    @POST("api/lecturer/requests/{id}/approve")
    Call<MessageResponse> approveRequest(@Path("id") long id);

    @POST("api/lecturer/requests/{id}/reject")
    Call<MessageResponse> rejectRequest(@Path("id") long id);

    @POST("api/lecturer/announcements")
    Call<MessageResponse> postAnnouncement(@Body ApiBodies.PostAnnouncement body);

    // ---- group leader ----
    @GET("api/group/members")
    Call<GroupInfo> getGroupMembers();

    @POST("api/group/members")
    Call<MessageResponse> addMember(@Body ApiBodies.AddMember body);

    @DELETE("api/group/members/{sid}")
    Call<MessageResponse> removeMember(@Path("sid") long studentId);

    @Streaming
    @GET("api/group/export.xlsx")
    Call<ResponseBody> exportMyGroupXlsx();

    @POST("api/group/export/send-to-lecturer")
    Call<MessageResponse> sendRosterToLecturer();

    // ---- reminders ----
    @GET("api/reminders")
    Call<RemindersResponse> getReminders();

    @POST("api/reminders")
    Call<MessageResponse> createReminder(@Body ApiBodies.CreateReminder body);

    @DELETE("api/reminders/{id}")
    Call<MessageResponse> deleteReminder(@Path("id") long id);

    // ---- timetable ----
    @GET("api/timetable")
    Call<TimetableResponse> getTimetable();

    @GET("api/timetable/next")
    Call<NextSessionResponse> getNextSession();

    @POST("api/timetable")
    Call<MessageResponse> createSession(@Body ApiBodies.Session body);

    @PATCH("api/timetable/{id}")
    Call<MessageResponse> updateSession(@Path("id") long id, @Body ApiBodies.Session body);

    @DELETE("api/timetable/{id}")
    Call<MessageResponse> deleteSession(@Path("id") long id);
}
