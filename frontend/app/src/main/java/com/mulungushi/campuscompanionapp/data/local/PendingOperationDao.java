package com.mulungushi.campuscompanionapp.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PendingOperationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(PendingOperation op);

    @Update
    int update(PendingOperation op);

    @Query("SELECT * FROM pending_operations " +
            "WHERE account_id = :accountId AND status = 'PENDING' " +
            "ORDER BY created_at ASC")
    List<PendingOperation> pendingForAccount(String accountId);

    @Query("SELECT COUNT(*) FROM pending_operations " +
            "WHERE account_id = :accountId AND status = 'PENDING'")
    LiveData<Integer> observePendingCount(String accountId);

    @Query("SELECT * FROM pending_operations WHERE operation_id = :opId LIMIT 1")
    PendingOperation findByOperationId(String opId);

    @Query("DELETE FROM pending_operations WHERE operation_id = :opId")
    int deleteById(String opId);
}