package com.mulungushi.campuscompanionapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A single queued mutation (create/edit/transfer/delete). WorkManager reads
 * these in order and replays them against the server.
 *
 * operationId is the idempotency key: retries reuse the SAME id, and the
 * server must reject reuse of that id with different content. Storing
 * serverResultJson durably means a lost response can't cause a duplicate
 * effect — on retry we just re-read the stored result instead of re-applying.
 */
@Entity(
        tableName = "pending_operations",
        indices = {
                @Index(value = "studentId"),
                @Index(value = "status")
        }
)
public class PendingOperation {

    @PrimaryKey
    @NonNull
    public String operationId; // client-generated UUID, stable across retries

    @NonNull
    public String studentId;

    public String accountId; // whose queue this belongs to — used to scope/clear on logout

    @NonNull
    public OperationType type;

    @NonNull
    @ColumnInfo(name = "payloadJson")
    public String payloadJson; // serialized field changes / creation payload

    public int baseVersion; // server version this op was built against, for conflict checks

    public int sequence; // monotonically increasing per-student, preserves operation order

    @NonNull
    public SyncStatus status = SyncStatus.PENDING;

    public int retryCount = 0;
    public long createdAt;
    public long lastAttemptAt = 0L;

    // Durable outcome storage — set once the server responds, even if the
    // response is later lost on the client side before it can be applied.
    public String serverResultJson;   // e.g. {"status":"OK","newVersion":4}
    public String failureReason;      // e.g. "GROUP_FULL", "CONFLICT", "STALE_NUMBER"

    public PendingOperation(@NonNull String operationId,
                            @NonNull String studentId,
                            @NonNull OperationType type,
                            @NonNull String payloadJson,
                            int baseVersion,
                            int sequence) {
        this.operationId = operationId;
        this.studentId = studentId;
        this.type = type;
        this.payloadJson = payloadJson;
        this.baseVersion = baseVersion;
        this.sequence = sequence;
        this.createdAt = System.currentTimeMillis();
    }
}