package com.mulungushi.campuscompanionapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "pending_operations",
        indices = {
                @Index(value = "account_id"),
                @Index(value = "student_id"),
                @Index(value = "status")
        }
)
public class PendingOperation {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "operation_id")
    public String operationId;

    @NonNull
    @ColumnInfo(name = "account_id")
    public String accountId;

    @NonNull
    @ColumnInfo(name = "student_id")
    public String studentId;

    @NonNull
    @ColumnInfo(name = "type")
    public String type;

    @NonNull
    @ColumnInfo(name = "payload_json")
    public String payloadJson;

    @ColumnInfo(name = "base_version")
    public int baseVersion;

    @NonNull
    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "last_attempt_at")
    public long lastAttemptAt;
}