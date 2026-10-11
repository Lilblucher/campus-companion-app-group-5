package com.mulungushi.campuscompanionapp.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Student student);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(Student student);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<Student> students);

    @Update
    void update(Student student);

    @Query("SELECT * FROM students WHERE studentId = :studentId LIMIT 1")
    LiveData<Student> observeById(String studentId);

    @Query("SELECT * FROM students WHERE studentId = :studentId LIMIT 1")
    Student getByIdSync(String studentId);

    @Query("UPDATE students SET active = 0, deletedAt = :now, studentNumber = :marker WHERE studentId = :studentId")
    void softDelete(String studentId, long now, String marker);

    @Query("UPDATE students SET syncStatus = :status WHERE studentId = :studentId")
    void updateSyncStatus(String studentId, SyncStatus status);

    @Query("SELECT * FROM students WHERE active = 1 ORDER BY studentName ASC")
    LiveData<List<Student>> observeAllActive();

    @Query("SELECT * FROM students WHERE active = 1 AND labGroup = :group ORDER BY studentName ASC")
    LiveData<List<Student>> observeByGroup(String group);

    @Query("SELECT * FROM students WHERE active = 1 AND syncStatus = :status ORDER BY studentName ASC")
    LiveData<List<Student>> observeByStatus(SyncStatus status);

    @Query("SELECT * FROM students WHERE studentNumber = :number LIMIT 1")
    Student findByNumber(String number);

    @Query("SELECT COUNT(*) FROM students WHERE active = 1 AND labGroup = :group")
    int countActiveInGroup(String group);
}
