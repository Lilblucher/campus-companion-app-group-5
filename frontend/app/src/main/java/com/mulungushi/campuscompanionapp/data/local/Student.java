package com.mulungushi.campuscompanionapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Locally cached student record — mirrors the server row but also tracks
 * sync state and soft-deletion so the UI can distinguish confirmed vs
 * pending values (per the lab's "confirmed values separate from pending
 * changes" rule).
 *
 * student_id is immutable and independent of student_number, so a
 * lecturer correcting a student number never breaks ownership or FKs.
 */
@Entity(
        tableName = "students",
        indices = {
                @Index(value = "studentNumber", unique = true),
                @Index(value = "labGroup")
        }
)
public class Student {

    @PrimaryKey
    @NonNull
    public String studentId; // server-issued UUID; generated client-side only for an unsynced CREATE

    @ColumnInfo(name = "studentNumber")
    @NonNull
    public String studentNumber; // exactly 9 digits, leading zeroes preserved, no embedded spaces

    @NonNull
    public String studentName; // 2-100 trimmed chars

    @NonNull
    public Programme programOfStudy;

    @NonNull
    public LabGroup labGroup; // confirmed group; UNASSIGNED if none

    public String accountId; // nullable until the student verifies/links their account

    // --- soft delete / integrity ---
    public boolean active = true;     // false once soft-deleted; number stays reserved
    public long deletedAt = 0L;       // 0 = not deleted; epoch millis otherwise

    // --- optimistic concurrency ---
    public int baseVersion = 0;       // server row version this local copy is based on

    // --- local-only sync bookkeeping (not sent to server) ---
    @NonNull
    public SyncStatus syncStatus = SyncStatus.SYNCED;
    public long lastSyncedAt = 0L;

    public Student(@NonNull String studentId,
                   @NonNull String studentNumber,
                   @NonNull String studentName,
                   @NonNull Programme programOfStudy,
                   @NonNull LabGroup labGroup) {
        this.studentId = studentId;
        this.studentNumber = studentNumber;
        this.studentName = studentName;
        this.programOfStudy = programOfStudy;
        this.labGroup = labGroup;
    }
}