package com.quittle.rds;

import android.app.DownloadManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class DownloadJobService extends JobService {
    private static final String TAG = DownloadJobService.class.getSimpleName();

    private DownloadMetadata downloadMetadata;
    private OngoingDownload ongoingDownload;
    private DownloadManager downloadManager;

    private static class OngoingDownload {
        long id;
        JobParameters params;
        String url;
    }

    @Override
    public void onCreate() {
        downloadMetadata = new DownloadMetadata(this);
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        DownloadBroadcastReceiver.setCallback(new Runnable() {
            @Override
            public void run() {
                final Uri uri = downloadManager.getUriForDownloadedFile(ongoingDownload.id);
                if (uri == null) {
                    Log.i(TAG, "Unable to download file");
                    return;
                }
                if (getApkPackageName(uri) != null) {
                    downloadMetadata.setDownloadUrl(ongoingDownload.url);
                    if (Objects.equals(hashFile(uri.getPath()), getInstalledPackageSignature(getApkPackageName(uri)))) {
                        Log.i(TAG, "APK installed with matching signature already");
                        return;
                    }

                    Intent promptInstall = new Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, "application/vnd.android.package-archive")
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                   startActivity(promptInstall);
                   jobFinished(ongoingDownload.params, false);
                }
            }
        });
    }

    @Override
    public boolean onStartJob(JobParameters parameters) {
        final String url = downloadMetadata.getDownloadUrl();
        if (url == null) {
            return false;
        }

        if (ongoingDownload != null) {
            if (url.equals(ongoingDownload.url)) {
                return false;
            } else if (downloadManager.remove(ongoingDownload.id) != 1) {
                Log.e(TAG, "Unable to cancel ongoing download");
            }
        }

        ongoingDownload = new OngoingDownload();
        ongoingDownload.url = url;
        ongoingDownload.params = parameters;
        try {
            ongoingDownload.id = downloadManager.enqueue(new DownloadManager.Request(Uri.parse(url)).setDestinationInExternalFilesDir(getApplicationContext(), null, "download.apk"));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Unable to enqueue download", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
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
        if (info == null) return null;

        return info.applicationInfo.packageName;
    }

}