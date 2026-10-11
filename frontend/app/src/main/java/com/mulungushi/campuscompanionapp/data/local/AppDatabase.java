package com.mulungushi.campuscompanionapp.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {Student.class, PendingOperation.class, RegistrationDraft.class},
        version = 2,
        exportSchema = true
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    public abstract StudentDao studentDao();
    public abstract PendingOperationDao pendingOperationDao();
    public abstract RegistrationDraftDao registrationDraftDao();

    private static volatile AppDatabase INSTANCE;

    // Example migration: version 1 -> 2 adds failureReason without losing
    // existing rows or queued pending operations, per the "one Room
    // migration without deleting existing or pending data" requirement.
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE pending_operations ADD COLUMN failureReason TEXT");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "labgroup.db")
                            .addMigrations(MIGRATION_1_2)
                            // Never use fallbackToDestructiveMigration() here: it would
                            // wipe saved offline work and pending operations, which the
                            // lab explicitly requires to survive a migration.
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public void saveOfflineEdit(Student student, PendingOperation op) {
        runInTransaction(() -> {
            studentDao().update(student);
            pendingOperationDao().insert(op);
        });
    }
}
