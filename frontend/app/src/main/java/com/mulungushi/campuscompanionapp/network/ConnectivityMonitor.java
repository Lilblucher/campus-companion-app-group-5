package com.mulungushi.campuscompanionapp.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.mulungushi.campuscompanionapp.util.Bg;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Online/offline + connection-quality detection.
 *   OFFLINE — no usable network
 *   SLOW    — reachable but low bandwidth / high latency / probe timed out
 *   GOOD    — healthy
 * Live online/offline comes from a default-network callback; quality is measured
 * on demand with a short latency probe against the backend's /health endpoint.
 */
public final class ConnectivityMonitor {

    public enum State { OFFLINE, SLOW, GOOD }
    public interface StateCb { void onState(State s); }
    public interface ConnectivityListener { void onConnectivityChanged(boolean online); }

    private static volatile ConnectivityMonitor INSTANCE;
    private final ConnectivityManager cm;
    private volatile boolean online;
    private final List<ConnectivityListener> listeners = new CopyOnWriteArrayList<>();

    private static final long SLOW_RTT_MS = 2000;
    private static final int SLOW_KBPS = 200;

    private final OkHttpClient probeClient = new OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build();

    private ConnectivityMonitor(Context ctx) {
        cm = (ConnectivityManager) ctx.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        online = computeOnline();
        try {
            if (cm != null) {
                cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                    @Override public void onAvailable(Network n) {
                        boolean was = online;
                        online = true;
                        if (!was) notifyListeners(true);
                    }
                    @Override public void onLost(Network n) {
                        boolean was = online;
                        online = computeOnline();
                        if (was && !online) notifyListeners(false);
                    }
                });
            }
        } catch (Exception ignored) {}
    }

    public static ConnectivityMonitor getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (ConnectivityMonitor.class) {
                if (INSTANCE == null) INSTANCE = new ConnectivityMonitor(ctx);
            }
        }
        return INSTANCE;
    }

    private boolean computeOnline() {
        try {
            if (cm == null) return false;
            Network n = cm.getActiveNetwork();
            if (n == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(n);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) { return false; }
    }

    public boolean isOnline() { return online || computeOnline(); }

    public void addListener(ConnectivityListener l) { listeners.add(l); }
    public void removeListener(ConnectivityListener l) { listeners.remove(l); }

    private void notifyListeners(boolean isOnline) {
        Bg.main(() -> {
            for (ConnectivityListener l : listeners) {
                try { l.onConnectivityChanged(isOnline); } catch (Exception ignored) {}
            }
        });
    }

    private int downstreamKbps() {
        try {
            Network n = cm.getActiveNetwork();
            NetworkCapabilities caps = n == null ? null : cm.getNetworkCapabilities(n);
            return caps == null ? -1 : caps.getLinkDownstreamBandwidthKbps();
        } catch (Exception e) { return -1; }
    }

    public void probe(StateCb cb) {
        Bg.io(() -> {
            boolean systemOnline = isOnline();
            State s;
            int kbps = downstreamKbps();
            long start = System.currentTimeMillis();
            boolean ok = false;
            try (Response r = probeClient.newCall(new Request.Builder().url(ApiClient.baseUrl() + "health").build()).execute()) {
                ok = r.isSuccessful();
            } catch (Exception ignored) { ok = false; }
            long rtt = System.currentTimeMillis() - start;
            if (ok) {
                if (rtt > SLOW_RTT_MS || (kbps > 0 && kbps < SLOW_KBPS)) s = State.SLOW;
                else s = State.GOOD;
                online = true;
            } else if (!systemOnline) {
                s = State.OFFLINE;
            } else {
                s = State.SLOW;
            }
            final State out = s;
            Bg.main(() -> cb.onState(out));
        });
    }
}
