package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanionapp.network.ConnectivityMonitor;
import com.mulungushi.campuscompanionapp.util.NetBanner;

/**
 * Base activity that shows an offline/slow/back-online banner automatically.
 * All activities should extend this instead of AppCompatActivity directly.
 */
public abstract class BaseActivity extends AppCompatActivity
        implements ConnectivityMonitor.ConnectivityListener {

    private ConnectivityMonitor connectivityMonitor;
    private boolean wasOffline;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        connectivityMonitor = ConnectivityMonitor.getInstance(this);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectivityMonitor.addListener(this);
        checkConnectivity();
    }

    @Override
    protected void onStop() {
        super.onStop();
        connectivityMonitor.removeListener(this);
    }

    private void checkConnectivity() {
        connectivityMonitor.probe(state -> {
            wasOffline = state != ConnectivityMonitor.State.GOOD;
            ViewGroup root = getNetBannerRoot();
            if (root != null) {
                NetBanner.show(this, root, state);
            }
        });
    }

    @Override
    public void onConnectivityChanged(boolean online) {
        ViewGroup root = getNetBannerRoot();
        if (root == null) return;
        if (online) {
            if (wasOffline) {
                NetBanner.showBackOnline(this, root);
                wasOffline = false;
            }
        } else {
            wasOffline = true;
            NetBanner.show(this, root, ConnectivityMonitor.State.OFFLINE);
        }
    }

    /**
     * Returns the root ViewGroup where the network banner should be inserted.
     * Default: the content view root. Override in subclasses for custom placement.
     */
    protected ViewGroup getNetBannerRoot() {
        return (ViewGroup) findViewById(android.R.id.content);
    }
}
