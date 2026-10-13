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
        entities = {Student.class, PendingOperation.class, RegistrationDraft.class,
                CachedReminder.class, CachedTimetable.class, CachedNotification.class},
        version = 3,
        exportSchema = true
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    public abstract StudentDao studentDao();
    public abstract PendingOperationDao pendingOperationDao();
    public abstract RegistrationDraftDao registrationDraftDao();
    public abstract CacheDao cacheDao();

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

    // v2 -> v3: add the offline read-caches. Non-destructive: only CREATEs new
    // tables, so saved students / drafts / pending operations are untouched.
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS cached_reminders (reminder_id INTEGER NOT NULL, title TEXT, body TEXT, scope TEXT, creator_role TEXT, remind_at TEXT, created_at TEXT, cached_at INTEGER NOT NULL, PRIMARY KEY(reminder_id))");
            database.execSQL("CREATE TABLE IF NOT EXISTS cached_timetable (session_id INTEGER NOT NULL, course_code TEXT, title TEXT, group_name TEXT, day_of_week INTEGER NOT NULL, start_time TEXT, end_time TEXT, venue TEXT, room TEXT, cached_at INTEGER NOT NULL, PRIMARY KEY(session_id))");
            database.execSQL("CREATE TABLE IF NOT EXISTS cached_notifications (notification_id INTEGER NOT NULL, type TEXT, title TEXT, body TEXT, is_read INTEGER NOT NULL, created_at TEXT, cached_at INTEGER NOT NULL, PRIMARY KEY(notification_id))");
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
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
