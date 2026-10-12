package com.mulungushi.campuscompanionapp.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;

/** Saves a downloaded .xlsx to app storage and opens the Android share sheet. */
public final class FileShare {
    private FileShare() {}
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static void saveAndShare(Activity act, ResponseBody body, String filename) {
        Bg.io(() -> {
            try {
                File dir = new File(act.getFilesDir(), "exports");
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, filename);
                try (InputStream in = body.byteStream(); OutputStream out = new FileOutputStream(f)) {
                    byte[] buf = new byte[8192]; int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                Uri uri = FileProvider.getUriForFile(act, act.getPackageName() + ".fileprovider", f);
                Bg.main(() -> {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType(XLSX);
                    i.putExtra(Intent.EXTRA_STREAM, uri);
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    act.startActivity(Intent.createChooser(i, "Share group roster"));
                });
            } catch (Exception e) {
                Bg.main(() -> Toast.makeText(act, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
}
