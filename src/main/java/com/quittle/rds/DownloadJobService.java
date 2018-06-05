package com.quittle.rds;

import android.app.DownloadManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import lombok.Data;

@SuppressWarnings("PMD.AccessorMethodGeneration")
public class DownloadJobService extends JobService {
    private static final String TAG = DownloadJobService.class.getSimpleName();
    private static final HashFunction SHA_256_HASH_FUNCTION = Hashing.sha256();
    private static final int JOB_ID_DOWNLOAD_CHECK = 1;

    private DownloadMetadata downloadMetadata;
    private OngoingDownload ongoingDownload;
    private DownloadManager downloadManager;
    private PackageManagementUtils packageManagementUtils;

    private String previousHash;

    @Data
    private static class OngoingDownload {
        private long id;
        private JobParameters params;
        private String url;
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
        packageManagementUtils = new PackageManagementUtils(this);
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        DownloadBroadcastReceiver.setCallback(new Runnable() {
            @Override
            public void run() {
                final Uri uri = getDownloadUri(ongoingDownload.getId());
                if (uri == null) {
                    Log.i(TAG, "Unable to download file");
                    reschedule();
                    return;
                }

                final String packageName = packageManagementUtils.getApkPackageName(uri);
                if (packageName == null) {
                    Log.i(TAG, "Downloaded file was an invalid APK");
                    delete(uri);
                    reschedule();
                    return;
                }

                downloadMetadata.setDownloadUrl(ongoingDownload.getUrl());

                final String downloadedHash = hashFile(uri.getPath());
                if (downloadedHash == null) {
                    Log.i(TAG, "Unable to hash downloaded file. Skipping");
                    delete(uri);
                    reschedule();
                    return;
                }

                Log.i(TAG, "previousHash: " + previousHash + " downloadedHash: " + downloadedHash);

                if (Objects.equals(downloadedHash, previousHash)) {
                    Log.i(TAG, "APK downloaded previously. Skipping install prompt");
                    delete(uri);
                    reschedule();
                    return;
                }

                previousHash = downloadedHash;

                if (Objects.equals(downloadedHash, hashFile(packageManagementUtils.getInstalledPackageApk(packageName)))) {
                    Log.i(TAG, "APK installed with matching signature already.");
                    delete(uri);
                    reschedule();
                    return;
                }

                final Uri downloadedContentUri = downloadManager.getUriForDownloadedFile(ongoingDownload.getId());

                Log.i(TAG, "Installing downloaded APK");
                Intent promptInstall = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(downloadedContentUri, "application/vnd.android.package-archive")
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(promptInstall);
                reschedule();
            }
        });
    }

    private static void delete(final Uri uri) {
        final String path = uri.getPath();
        final File file = new File(path);
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Unable to delete file: " + path);
        }
    }

    private void reschedule() {
        Log.i(TAG, "Rescheduling download");
        jobFinished(ongoingDownload.getParams(), false);
        startRunning(this);
    }

    @Override
    public boolean onStartJob(JobParameters parameters) {
        final String url = downloadMetadata.getDownloadUrl();
        if (url == null) {
            reschedule();
            return false;
        }

        if (ongoingDownload != null) {
            if (url.equals(ongoingDownload.getUrl())) {
                reschedule();
                return false;
            } else if (downloadManager.remove(ongoingDownload.getId()) != 1) {
                Log.e(TAG, "Unable to cancel ongoing download.");
            }
        }

        ongoingDownload = new OngoingDownload();
        ongoingDownload.setUrl(url);
        ongoingDownload.setParams(parameters);
        try {
            final long downloadId = downloadManager.enqueue(
                    new DownloadManager.Request(Uri.parse(url))
                            .setDestinationInExternalFilesDir(this, null, "3-download.apk"));
            ongoingDownload.setId(downloadId);
            Log.i(TAG, "Enqued download of " +  url + " with id " + downloadId);
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
            return Files.hash(file, SHA_256_HASH_FUNCTION).toString();
        } catch (IOException e) {
            return null;
        }
    }

    private Uri getDownloadUri(long downloadId) {
        final Cursor cursor = downloadManager.query(
                new DownloadManager.Query().setFilterById(downloadId)
                        .setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL));
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }

            final int col = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
            if (col == -1) {
                return null;
            }

            final String uri = cursor.getString(col);
            if (uri == null) {
                return null;
            }
            return Uri.parse(uri);
        } finally {
            cursor.close();
        }
    }
}
