package com.mulungushi.campuscompanionapp.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.mulungushi.campuscompanionapp.data.local.AppDatabase;
import com.mulungushi.campuscompanionapp.data.local.PendingOperation;
import com.mulungushi.campuscompanionapp.data.local.PendingOperationDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PendingOperationRepository {

    private final PendingOperationDao dao;
    private final ExecutorService executor;

    public PendingOperationRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.dao = db.pendingOperationDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void enqueue(PendingOperation op) {
        executor.execute(() -> dao.insert(op));
    }

    public void updateStatus(PendingOperation op) {
        executor.execute(() -> dao.update(op));
    }

    public void deleteById(String opId) {
        executor.execute(() -> dao.deleteById(opId));
    }

    public void pendingForAccount(String accountId, OnResult<List<PendingOperation>> callback) {
        executor.execute(() -> callback.onResult(dao.pendingForAccount(accountId)));
    }

    public LiveData<Integer> observePendingCount(String accountId) {
        return dao.observePendingCount(accountId);
    }

    public void findByOperationId(String opId, OnResult<PendingOperation> callback) {
        executor.execute(() -> callback.onResult(dao.findByOperationId(opId)));
    }

    public interface OnResult<T> {
        void onResult(T value);
    }
}