package com.quittle.rds;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DownloadBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = DownloadBroadcastReceiver.class.getSimpleName();

    private static final Object lock = new Object();

    private static Runnable runnable;

    public static void setCallback(final Runnable callback) {
        synchronized (lock) {
            runnable = callback;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            final Runnable r;
            synchronized (lock) {
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

