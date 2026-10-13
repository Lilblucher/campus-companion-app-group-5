package com.mulungushi.campuscompanionapp.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tiny background/main thread helper for Room I/O and UI callbacks. */
public final class Bg {
    private Bg() {}
    private static final ExecutorService IO = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void io(Runnable r) { IO.execute(r); }
    public static void main(Runnable r) { MAIN.post(r); }
}
