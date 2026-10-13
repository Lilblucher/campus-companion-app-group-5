package com.mulungushi.campuscompanionapp.network;

import java.util.List;

/** GET /api/lecturer/requests */
public class RequestsResponse {
    public List<Request> requests;

    public static class Request {
        public long request_id;
        public long student_id;
        public String student_name;
        public String student_number;
        public Integer current_group;
        public String current_group_name;
        public Integer requested_group;
        public String requested_group_name;
        public String reason;
        public String status;
        public String created_at;
    }
}
