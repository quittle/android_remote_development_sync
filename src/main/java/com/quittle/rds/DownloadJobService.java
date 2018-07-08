package com.quittle.rds;

import android.app.DownloadManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.PersistableBundle;
import android.util.Log;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("PMD.AccessorMethodGeneration")
public class DownloadJobService extends JobService {
    private static final String TAG = DownloadJobService.class.getSimpleName();
    private static final HashFunction SHA_256_HASH_FUNCTION = Hashing.sha256();
    private static final String BUNDLE_KEY_URL = "url";

    private static final IntentFilter DOWNLOAD_COMPLETE_INTENT_FILTER = new IntentFilter();
    static {
        DOWNLOAD_COMPLETE_INTENT_FILTER.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
    }

    private DownloadManager downloadManager;
    private PackageManagementUtils packageManagementUtils;

    private static String previousHash;

    private static Set<Integer> cancelledJobIds = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());

    /**
     * @param context Android context used to schedule the download
     * @param url The url to download
     */
    public static void startRunning(final Context context, final String url) {
        cancelledJobIds.remove(urlToJobId(url));
        startRunning(context, url, false);
    }

    private static void startRunning(final Context context, final String url, final boolean force) {
        final JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            Log.e(TAG, "Unable to get job scheduler.");
            return;
        }

        final int jobId = urlToJobId(url);

        if (!force && isJobPending(jobScheduler, jobId)) {
            Log.d(TAG, "Job already scheduled: " + url);
            return;
        }

        if (cancelledJobIds.contains(jobId)) {
            Log.d(TAG, "Job cancelled. Skipping scheduling");
            return;
        }

        final PersistableBundle extras = new PersistableBundle();
        extras.putString(BUNDLE_KEY_URL, url);

        jobScheduler.schedule(new JobInfo.Builder(jobId, new ComponentName(context, DownloadJobService.class))
                .setMinimumLatency(1000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setExtras(extras)
                .build());
    }

    private static boolean isJobPending(final JobScheduler jobScheduler, final int jobId) {
        for (final JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == jobId) {
                return true;
            }
        }
        return false;
    }

    private static int urlToJobId(final String url) {
        return url.hashCode();
    }

    public static void stopRunning(final Context context, final String url) {
        stopRunning(context, urlToJobId(url));
    }

    private static void stopRunning(final Context context, final int jobId) {
        cancelledJobIds.add(jobId);

        final JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            Log.e(TAG, "Unable to get job scheduler.");
            return;
        }

        jobScheduler.cancel(jobId);
    }

    @Override
    public void onCreate() {
        packageManagementUtils = new PackageManagementUtils(this);
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
    }

    private static void delete(final Uri uri) {
        final String path = uri.getPath();
        final File file = new File(path);
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Unable to delete file: " + path);
        }
    }

    private void reschedule(final JobParameters params) {
        Log.i(TAG, "Rescheduling download");
        jobFinished(params, false);
        startRunning(this, params.getExtras().getString(BUNDLE_KEY_URL), true);
    }

    @Override
    @SuppressWarnings("PMD.AvoidFinalLocalVariable")
    public boolean onStartJob(final JobParameters parameters) {
        final String url = parameters.getExtras().getString(BUNDLE_KEY_URL);
        if (url == null) {
            reschedule(parameters);
            return false;
        }

        final long downloadId;
        try {
            downloadId = downloadManager.enqueue(
                    new DownloadManager.Request(Uri.parse(url))
                            .setDestinationInExternalFilesDir(this, null, "download.apk"));
            Log.i(TAG, "Enqued download of " +  url + " with id " + downloadId);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Unable to enqueue download", e);
            reschedule(parameters);
            return false;
        }
        getApplicationContext().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "Download received");
                    final Long receivedDownloadId = intent.getExtras().getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
                    if (receivedDownloadId != downloadId) {
                        return;
                    }

                    getApplicationContext().unregisterReceiver(this);
                    onDownloadComplete(parameters, receivedDownloadId);
                }
            }, DOWNLOAD_COMPLETE_INTENT_FILTER);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
        Log.i(TAG, "Stopping job");
        return false;
    }

    private void onDownloadComplete(final JobParameters jobParameters, final long downloadId) {
        final Uri uri = getDownloadedFileUriByDownloadId(downloadId);
        if (uri == null) {
            Log.i(TAG, "Unable to download file");
            reschedule(jobParameters);
            return;
        }

        final String packageName = packageManagementUtils.getApkPackageName(uri);
        if (packageName == null) {
            Log.i(TAG, "Downloaded file was an invalid APK");
            delete(uri);
            reschedule(jobParameters);
            return;
        }

        final String downloadedHash = hashFile(new File(uri.getPath()));
        if (downloadedHash == null) {
            Log.i(TAG, "Unable to hash downloaded file. Skipping");
            delete(uri);
            reschedule(jobParameters);
            return;
        }

        Log.d(TAG, "previousHash: " + previousHash + " downloadedHash: " + downloadedHash);

        if (Objects.equals(downloadedHash, previousHash)) {
            Log.i(TAG, "APK downloaded previously. Skipping install prompt");
            delete(uri);
            reschedule(jobParameters);
            return;
        }

        previousHash = downloadedHash;

        final File installedApk = packageManagementUtils.getInstalledPackageApk(packageName);
        if (installedApk != null && Objects.equals(downloadedHash, hashFile(installedApk))) {
            Log.i(TAG, "APK installed with matching signature already.");
            delete(uri);
            reschedule(jobParameters);
            return;
        }

        final Uri downloadedContentUri = downloadManager.getUriForDownloadedFile(downloadId);

        Log.i(TAG, "Installing downloaded APK");
        Intent promptInstall = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(downloadedContentUri, "application/vnd.android.package-archive")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(promptInstall);
        reschedule(jobParameters);
    }

    private String hashFile(final File file) {
        if (!file.exists()) {
            return null;
        }
        try {
            return Files.hash(file, SHA_256_HASH_FUNCTION).toString();
        } catch (IOException e) {
            return null;
        }
    }

    private Uri getDownloadedFileUriByDownloadId(long downloadId) {
        final Cursor cursor = downloadManager.query(
                new DownloadManager.Query()
                        .setFilterById(downloadId)
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
