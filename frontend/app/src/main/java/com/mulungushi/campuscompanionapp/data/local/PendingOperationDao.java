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

    // Fails on a genuine PK collision with different content instead of
    // silently overwriting it — enforces "reject reuse of that id with
    // different content" at the local layer too.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(PendingOperation operation);

    @Update
    void update(PendingOperation operation);

    @Query("SELECT * FROM pending_operations WHERE operationId = :operationId LIMIT 1")
    PendingOperation findByOperationId(String operationId);

    @Query("DELETE FROM pending_operations WHERE operationId = :operationId")
    void deleteById(String operationId);

    @Query("SELECT * FROM pending_operations WHERE accountId = :accountId AND status = 'PENDING' ORDER BY sequence ASC")
    List<PendingOperation> pendingForAccount(String accountId);

    @Query("SELECT COUNT(*) FROM pending_operations WHERE accountId = :accountId AND status = 'PENDING'")
    LiveData<Integer> observePendingCount(String accountId);
}
