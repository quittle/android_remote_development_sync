package com.quittle.rds;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DownloadBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = DownloadBroadcastReceiver.class.getSimpleName();

    private static final Object LOCK = new Object();

    private static Runnable runnable;

    public static void setCallback(final Runnable callback) {
        synchronized (LOCK) {
            runnable = callback;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            @SuppressWarnings("PMD.AvoidFinalLocalVariable")
            final Runnable r;
            synchronized (LOCK) {
                r = runnable;
            }

            if (r != null) {
                r.run();
            }
        } else {
            Log.e(TAG, "Unexected action received");
        }
    }
}

