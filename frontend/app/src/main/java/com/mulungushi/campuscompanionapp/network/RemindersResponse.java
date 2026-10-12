package com.mulungushi.campuscompanionapp.network;

import java.util.List;

/** GET /api/reminders */
public class RemindersResponse {
    public List<Reminder> reminders;

    public static class Reminder {
        public long reminder_id;
        public long created_by;
        public String creator_role;
        public String scope;            // personal | group | programme | all
        public Integer target_group_id;
        public String target_programme;
        public String title;
        public String body;
        public String remind_at;        // may be null
        public String created_at;
    }
}
