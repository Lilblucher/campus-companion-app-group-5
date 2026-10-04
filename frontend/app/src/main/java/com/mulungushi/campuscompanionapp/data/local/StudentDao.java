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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Student student);

    @Update
    int update(Student student);

    @Query("SELECT * FROM students WHERE deleted_at IS NULL ORDER BY student_name ASC")
    LiveData<List<Student>> observeAllActive();

    @Query("SELECT * FROM students WHERE student_id = :id LIMIT 1")
    Student findById(String id);

    @Query("SELECT * FROM students WHERE student_number = :number LIMIT 1")
    Student findByNumber(String number);

    @Query("SELECT * FROM students WHERE lab_group = :group AND deleted_at IS NULL")
    LiveData<List<Student>> observeByGroup(String group);

    @Query("SELECT COUNT(*) FROM students WHERE lab_group = :group AND deleted_at IS NULL")
    int countActiveInGroup(String group);

    @Query("UPDATE students SET deleted_at = :now, deletion_marker = :marker, " +
            "number_reserved = 1, lab_group = NULL, base_version = base_version + 1 " +
            "WHERE student_id = :id")
    int softDelete(String id, long now, String marker);

    @Query("UPDATE students SET sync_status = :status WHERE student_id = :id")
    int updateSyncStatus(String id, String status);

    @Query("SELECT * FROM students WHERE sync_status = :status AND deleted_at IS NULL")
    LiveData<List<Student>> observeByStatus(String status);
}