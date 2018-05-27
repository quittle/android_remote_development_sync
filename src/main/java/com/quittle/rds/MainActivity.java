package com.quittle.rds;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String PREF_GROUP_DOWNLOADS = "downloads";
    private static final String PREF_CACHED_URL = "cached_url";
    private static final int JOB_ID_DOWNLOAD_CHECK = 1;

    private DownloadManager downloadManager;
    private DownloadMetadata downloadMetadata;
    private OngoingDownload ongoingDownload;

    private static class OngoingDownload {
        long id;
        String url;
    }

    public MainActivity() {
        DownloadBroadcastReceiver.setCallback(new Runnable() {
            @Override
            public void run() {
                final Uri uri = downloadManager.getUriForDownloadedFile(ongoingDownload.id);
                if (uri == null) {
                    Log.i(TAG, "Unable to download file");
                    return;
                }
                if (getApkPackageName(uri) != null) {
                    cacheUrl(ongoingDownload.url);
                    if (Objects.equals(hashFile(uri.getPath()), getInstalledPackageSignature(getApkPackageName(uri)))) {
                        Log.i(TAG, "APK installed with matching signature already");
                        return;
                    }

                    Intent promptInstall = new Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, "application/vnd.android.package-archive")
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                   startActivity(promptInstall);
                }
            }
        });
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

    private SharedPreferences getSharedPreferences() {
        return getSharedPreferences(PREF_GROUP_DOWNLOADS, MODE_PRIVATE);
    }

    private String getCachedUrl() {
        return getSharedPreferences().getString(PREF_CACHED_URL, "");
    }

    private void cacheUrl(final String url) {
        getSharedPreferences().edit().putString(PREF_CACHED_URL, url).apply();
    }

    private String getApkPackageName(Uri uri) {
        PackageInfo info = getPackageManager().getPackageArchiveInfo(uri.getPath(), 0);
        if (info == null) return null;

        return info.applicationInfo.packageName;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        final JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            Log.e(TAG, "Unable to get job scheduler");
            return;
        }
        downloadMetadata = new DownloadMetadata(this);
        jobScheduler.schedule(new JobInfo.Builder(JOB_ID_DOWNLOAD_CHECK, new ComponentName(this, DownloadJobService.class))
                .setPeriodic(1000)
                .build());

        final EditText urlEditText = findViewById(R.id.url_edit_text);
        urlEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {
                    // TODO: Implement this method
                }

                @Override
                public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {
                    // TODO: Implement this method
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    final String text = editable.toString().trim();
                    downloadMetadata.setDownloadUrl(text);
                }
        });
        urlEditText.setText(getCachedUrl());
    }
}
