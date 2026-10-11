package com.mulungushi.campuscompanionapp.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;

@Dao
public interface RegistrationDraftDao {

    @Insert
    long insert(RegistrationDraft draft);

    @Update
    void update(RegistrationDraft draft);

    @Delete
    void delete(RegistrationDraft draft);

    @Query("SELECT * FROM registration_drafts WHERE verified = 0 ORDER BY updatedAt DESC LIMIT 1")
    LiveData<RegistrationDraft> observeLatestUnverified();

    @Query("SELECT * FROM registration_drafts WHERE draftId = :draftId LIMIT 1")
    RegistrationDraft getById(long draftId);
}