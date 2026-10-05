package com.mulungushi.campuscompanionapp.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Transaction;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = { Student.class, PendingOperation.class },
        version = 2,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract StudentDao studentDao();
    public abstract PendingOperationDao pendingOperationDao();

    @Transaction
    public void saveOfflineEdit(Student student, PendingOperation op) {
        studentDao().insert(student);
        pendingOperationDao().insert(op);
    }

    private static volatile AppDatabase INSTANCE;

    // ---------- MIGRATION 1 → 2 ----------
    // Adds the `sync_status` column to the students table.
    // Runs automatically when a v1 database is upgraded to v2.
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "ALTER TABLE students ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'SYNCED'"
            );
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                     INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "campus_companion.db"
                            )
                            .addMigrations(MIGRATION_1_2)   // ← no more destructive fallback
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}