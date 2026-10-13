package com.mulungushi.campuscompanionapp.network;

import java.util.List;

/** GET /api/lecturer/groups */
public class GroupsResponse {
    public List<Group> groups;

    public static class Group {
        public int group_id;
        public String group_name;
        public int max_members;
        public int active_members;
        public int places_left;
    }
}
