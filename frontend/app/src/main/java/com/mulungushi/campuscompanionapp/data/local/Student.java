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
    public String studentId;

    @NonNull
    @ColumnInfo(name = "student_number")
    public String studentNumber;

    @NonNull
    @ColumnInfo(name = "student_name")
    public String studentName;

    @NonNull
    @ColumnInfo(name = "programme")
    public String programme;

    @ColumnInfo(name = "lab_group")
    public String labGroup;

    @ColumnInfo(name = "base_version")
    public int baseVersion;

    @ColumnInfo(name = "deleted_at")
    public Long deletedAt;

    @ColumnInfo(name = "deletion_marker")
    public String deletionMarker;

    @ColumnInfo(name = "number_reserved")
    public boolean numberReserved;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @NonNull
    @ColumnInfo(name = "sync_status", defaultValue = "SYNCED")
    public String syncStatus = "SYNCED";
}