package com.quittle.rds;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.job.JobInfo;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.app.job.JobWorkItem;
import android.content.ComponentName;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class DownloadJobService extends JobService {
    private static final String TAG = DownloadJobService.class.getSimpleName();
    private static final int JOB_ID_DOWNLOAD_CHECK = 1;

    private DownloadMetadata downloadMetadata;
    private OngoingDownload ongoingDownload;
    private DownloadManager downloadManager;

    private static class OngoingDownload {
        long id;
        JobParameters params;
        String url;
    }

    public static void startRunning(final Context context) {
        final JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            Log.e(TAG, "Unable to get job scheduler.");
            return;
        }

        jobScheduler.schedule(new JobInfo.Builder(JOB_ID_DOWNLOAD_CHECK, new ComponentName(context, DownloadJobService.class))
                .setMinimumLatency(1000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build());
    }

    @Override
    public void onCreate() {
        downloadMetadata = new DownloadMetadata(this);
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        Log.i(TAG, "onCreate");

        final Context self = this;
        DownloadBroadcastReceiver.setCallback(new Runnable() {
            @Override
            public void run() {
                final Uri uri = getDownloadUri(ongoingDownload.id);
                if (uri == null) {
                    Log.i(TAG, "Unable to download file");
                    reschedule();
                    return;
                }
                if (getApkPackageName(uri) == null) {
                    Log.i(TAG, "Downloaded file was an invalid APK");
                    reschedule();
                    return;
                }

                downloadMetadata.setDownloadUrl(ongoingDownload.url);
                if (Objects.equals(hashFile(uri.getPath()), getInstalledPackageSignature(getApkPackageName(uri)))) {
                    Log.i(TAG, "APK installed with matching signature already.");
                    reschedule();
                    return;
                }

                final Uri downloadedContentUri = downloadManager.getUriForDownloadedFile(ongoingDownload.id);

                Log.i(TAG, "Installing downloaded APK");
                Intent promptInstall = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(downloadedContentUri, "application/vnd.android.package-archive")
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(promptInstall);
                reschedule();
            }
        });
    }

    private void reschedule() {
        Log.i(TAG, "Rescheduling download");
        jobFinished(ongoingDownload.params, false);
        startRunning(this);
    }

    @Override
    public boolean onStartJob(JobParameters parameters) {
        Log.i(TAG, "onStartJob");

        final String url = downloadMetadata.getDownloadUrl();
        if (url == null) {
            reschedule();
            return false;
        }

        if (ongoingDownload != null) {
            if (url.equals(ongoingDownload.url)) {
                reschedule();
                return false;
            } else if (downloadManager.remove(ongoingDownload.id) != 1) {
                Log.e(TAG, "Unable to cancel ongoing download.");
            }
        }

        ongoingDownload = new OngoingDownload();
        ongoingDownload.url = url;
        ongoingDownload.params = parameters;
        try {
            ongoingDownload.id = downloadManager.enqueue(new DownloadManager.Request(Uri.parse(url)).setDestinationInExternalFilesDir(this, null, "download.apk"));
            Log.i(TAG, "Enqued download of " +  url + " with id " + ongoingDownload.id);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Unable to enqueue download", e);
            reschedule();
            return false;
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
        startRunning(this);
        return false;
    }


    private String hashFile(final String path) {
        final File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        try {
            return Files.hash(new File(path), Hashing.sha256()).toString();
        } catch (IOException e) {
            return null;
        }
    }

    private String getInstalledPackageSignature(final String packageName) {
        final String path;
        try {
            path = getPackageManager().getApplicationInfo(packageName, 0).sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }

        return hashFile(path);
    }

    private String getApkPackageName(Uri uri) {
        PackageInfo info = getPackageManager().getPackageArchiveInfo(uri.getPath(), 0);
        Log.d(TAG, "uri: " + new File(uri.toString()).getAbsolutePath());
        Log.d(TAG, "packageInfo: " + info);
        if (info == null) return null;

        return info.applicationInfo.packageName;
    }

    private Uri getDownloadUri(long downloadId) {
        final Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId).setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL));
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }

            final int col = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
            if (col == -1) {
                return null;
            }

            final String uri = cursor.getString(col);
            return Uri.parse(uri);
        } finally {
            cursor.close();
        }
    }

}