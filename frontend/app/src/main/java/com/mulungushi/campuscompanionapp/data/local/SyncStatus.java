package com.mulungushi.campuscompanionapp.data.local;

/**
 * Sync lifecycle for a locally stored student record.
 * Shown to the user per the lab spec: Saved locally, Pending, Syncing, Synced, Action required.
 */
public enum SyncStatus {
    SAVED_LOCALLY,   // written to Room only, not yet queued
    PENDING,         // queued as a PendingOperation, waiting for connectivity/WorkManager
    SYNCING,         // request currently in flight
    SYNCED,          // server confirmed, local record matches server version
    ACTION_REQUIRED  // conflict, GROUP_FULL, or rejected — needs user review
}