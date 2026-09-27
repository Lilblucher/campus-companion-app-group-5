package com.mulungushi.campuscompanionapp.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.mulungushi.campuscompanionapp.data.local.AppDatabase;
import com.mulungushi.campuscompanionapp.data.local.Student;
import com.mulungushi.campuscompanionapp.data.local.StudentDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudentRepository {

    private final StudentDao studentDao;
    private final ExecutorService executor;

    public StudentRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.studentDao = db.studentDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    // ---------- background writes ----------

    public void insert(Student student, Runnable onComplete) {
        executor.execute(() -> {
            studentDao.insert(student);
            if (onComplete != null) onComplete.run();
        });
    }

    public void update(Student student, Runnable onComplete) {
        executor.execute(() -> {
            studentDao.update(student);
            if (onComplete != null) onComplete.run();
        });
    }

    public void softDelete(String studentId, Runnable onComplete) {
        executor.execute(() -> {
            long now = System.currentTimeMillis();
            String marker = "del_" + studentId + "_" + now;
            studentDao.softDelete(studentId, now, marker);
            if (onComplete != null) onComplete.run();
        });
    }

    // ---------- live reads ----------

    public LiveData<List<Student>> observeAllActive() {
        return studentDao.observeAllActive();
    }

    public LiveData<List<Student>> observeByGroup(String group) {
        return studentDao.observeByGroup(group);
    }

    // ---------- one-shot reads (background) ----------

    public void findByNumber(String number, OnResult<Student> callback) {
        executor.execute(() -> callback.onResult(studentDao.findByNumber(number)));
    }

    public void countActiveInGroup(String group, OnResult<Integer> callback) {
        executor.execute(() -> callback.onResult(studentDao.countActiveInGroup(group)));
    }

    // ---------- simple callback interface ----------

    public interface OnResult<T> {
        void onResult(T value);
    }
}