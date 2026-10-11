package com.mulungushi.campuscompanionapp.data.local;

import androidx.room.TypeConverter;

public class Converters {

    @TypeConverter
    public static Programme toProgramme(String value) {
        return value == null ? null : Programme.valueOf(value);
    }

    @TypeConverter
    public static String fromProgramme(Programme value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static LabGroup toLabGroup(String value) {
        return value == null ? null : LabGroup.valueOf(value);
    }

    @TypeConverter
    public static String fromLabGroup(LabGroup value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static SyncStatus toSyncStatus(String value) {
        return value == null ? null : SyncStatus.valueOf(value);
    }

    @TypeConverter
    public static String fromSyncStatus(SyncStatus value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static OperationType toOperationType(String value) {
        return value == null ? null : OperationType.valueOf(value);
    }

    @TypeConverter
    public static String fromOperationType(OperationType value) {
        return value == null ? null : value.name();
    }
}