package com.mulungushi.campuscompanionapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "students",
        indices = {
                @Index(value = "student_number", unique = true),
                @Index(value = "lab_group")
        }
)
public class Student {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "student_id")
    public String studentId;                 // immutable UUID

    @NonNull
    @ColumnInfo(name = "student_number")
    public String studentNumber;             // 9 digits, String preserves leading zeros

    @NonNull
    @ColumnInfo(name = "student_name")
    public String studentName;

    @NonNull
    @ColumnInfo(name = "programme")
    public String programme;                 // "CS" | "IT" | "DS"

    @ColumnInfo(name = "lab_group")
    public String labGroup;                  // "G01".."G04" or null = Unassigned

    @ColumnInfo(name = "base_version")
    public int baseVersion;                  // for Challenge 3 conflict detection

    @ColumnInfo(name = "deleted_at")
    public Long deletedAt;                   // null = active; timestamp = soft-deleted

    @ColumnInfo(name = "deletion_marker")
    public String deletionMarker;            // sync marker for pull/push

    @ColumnInfo(name = "number_reserved")
    public boolean numberReserved;           // true after soft delete — blocks re-registration

    @ColumnInfo(name = "updated_at")
    public long updatedAt;                   // for sync ordering
}