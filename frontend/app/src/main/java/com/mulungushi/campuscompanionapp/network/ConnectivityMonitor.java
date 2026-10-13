package com.mulungushi.campuscompanionapp.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.mulungushi.campuscompanionapp.util.Bg;

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

    private static volatile ConnectivityMonitor INSTANCE;
    private final ConnectivityManager cm;
    private volatile boolean online;

    // Quality thresholds
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
                    @Override public void onAvailable(Network n) { online = true; }
                    @Override public void onLost(Network n) { online = computeOnline(); }
                });
            }
        } catch (Exception ignored) { /* some devices restrict this */ }
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

    private int downstreamKbps() {
        try {
            Network n = cm.getActiveNetwork();
            NetworkCapabilities caps = n == null ? null : cm.getNetworkCapabilities(n);
            return caps == null ? -1 : caps.getLinkDownstreamBandwidthKbps();
        } catch (Exception e) { return -1; }
    }

    /** Measure and deliver the current state on the main thread. */
    public void probe(StateCb cb) {
        Bg.io(() -> {
            if (!isOnline()) { Bg.main(() -> cb.onState(State.OFFLINE)); return; }
            State s;
            int kbps = downstreamKbps();
            long start = System.currentTimeMillis();
            boolean ok = false;
            try (Response r = probeClient.newCall(new Request.Builder().url(ApiClient.baseUrl() + "health").build()).execute()) {
                ok = r.isSuccessful();
            } catch (Exception ignored) { ok = false; }
            long rtt = System.currentTimeMillis() - start;
            if (!ok || rtt > SLOW_RTT_MS || (kbps > 0 && kbps < SLOW_KBPS)) s = State.SLOW;
            else s = State.GOOD;
            final State out = s;
            Bg.main(() -> cb.onState(out));
        });
    }
}
