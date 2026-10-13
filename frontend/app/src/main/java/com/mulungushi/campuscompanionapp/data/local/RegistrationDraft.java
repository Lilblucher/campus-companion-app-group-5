package com.mulungushi.campuscompanionapp.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * A registration draft saved while offline. Kept separate from Student
 * because it has no student_id yet and no server confirmation — it only
 * becomes a Student (or a PendingOperation of type CREATE) once the user
 * completes account verification online.
 */
@Entity(tableName = "registration_drafts")
public class RegistrationDraft {

    @PrimaryKey(autoGenerate = true)
    public long draftId;

    public String studentName;
    public String studentNumber;
    public Programme programOfStudy;
    public LabGroup requestedLabGroup; // request only — never reserves a place

    public String claimCode;
    public boolean verified = false;
    public long updatedAt;

    public RegistrationDraft() {
        this.updatedAt = System.currentTimeMillis();
    }
}
