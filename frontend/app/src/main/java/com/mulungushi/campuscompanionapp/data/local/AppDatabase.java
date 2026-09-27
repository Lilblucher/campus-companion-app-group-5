package com.mulungushi.campuscompanionapp.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = { Student.class, PendingOperation.class },
        version = 1,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract StudentDao studentDao();
    public abstract PendingOperationDao pendingOperationDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "campus_companion.db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}
