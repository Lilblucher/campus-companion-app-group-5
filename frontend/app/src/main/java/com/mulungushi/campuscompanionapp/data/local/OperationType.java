package com.mulungushi.campuscompanionapp.data.local;

/** Type of mutation queued for sync. Used for both student and lecturer actions. */
public enum OperationType {
    CREATE,
    EDIT,
    TRANSFER,   // group change / assignment
    DELETE      // soft delete
}