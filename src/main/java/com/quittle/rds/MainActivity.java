package com.quittle.rds;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.job.JobScheduler;
import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
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
    private JobScheduler jobScheduler;
    private EditText urlEditText;
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
                }
                // DownloadManager.Query query = new DownloadManager.Query().setFilterById(new long[]{ongoingDownload});
                // final Cursor cursor = downloadManager.query(query);
                // cursor.moveToFirst();
                // if (cursor.isBeforeFirst() || cursor.isAfterLast()) {
                //     Log.e(TAG, "Unable to find download");
                //     return;
                // }
                // final int localUriColumn = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                // if (localUriColumn < 0) {
                //     Log.e(TAG, "Unable to find the local uri column");
                //     return;
                // }
                // final String localUri = cursor.getString(localUriColumn);
                // if (localUri == null) {
                //     Log.e(TAG, "No local uri");
                //     return;
                // }
                // final int remoteUriColumn = cursor.getColumnIndex(DownloadManager.COLUMN_URI);
                // if (remoteUriColumn < 0) {
                //     Log.e(TAG, "Unable to get remote uri column");
                //     return;
                // }
                // final String remoteUri = cursor.getString(remoteUriColumn);
                // if (remoteUri == null) {
                //     Log.e(TAG, "No remote uri");
                // }
                // final Uri uri = Uri.parse(localUri);
                if (getApkPackageName(uri) != null) {
                    cacheUrl(ongoingDownload.url);
                    if (Objects.equals(hashFile(uri.getPath()), getInstalledPackageSignature(getApkPackageName(uri)))) {
                        Log.i(TAG, "APK installed with matching signature already");
                        return;
                    }

                    Intent promptInstall = new Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, "application/vnd.android.package-archive")
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    //   Uri apkURI = FileProvider.getUriForFile(
                    //          context,
                    //          context.getApplicationContext()
                    //          .getPackageName() + ".provider", file);
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
        getSharedPreferences().edit().putString(PREF_CACHED_URL, url).commit();
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

        downloadManager = getSystemService(DownloadManager.class);//(DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        jobScheduler = getSystemService(JobScheduler.class);
        downloadMetadata = new DownloadMetadata(this);
        // jobScheduler.schedule(new JobInfo.Builder(JOB_ID_DOWNLOAD_CHECK, new ComponentName(DownloadJobService.class.getPackage().getName(), DownloadJobService.class.getSimpleName()))
        jobScheduler.schedule(new JobInfo.Builder(JOB_ID_DOWNLOAD_CHECK, new ComponentName(this, DownloadJobService.class))
                .setPeriodic(1000)
                .build());

        urlEditText = findViewById(R.id.url_edit_text);
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
                    // final Uri uri = Uri.parse(text);

                    // if (ongoingDownload != null) {
                    //     if (downloadManager.remove(new long[]{ ongoingDownload.id }) != 1) {
                    //         Log.e(TAG, "Unable to cancel ongoing download");
                    //     }
                    // }

                    downloadMetadata.setDownloadUrl(text);

                    // try {
                    //     ongoingDownload = new OngoingDownload();
                    //     ongoingDownload.url = text;
                    //     ongoingDownload.id = downloadManager.enqueue(new DownloadManager.Request(uri).setDestinationInExternalFilesDir(getApplicationContext(), null, "download.apk"));
                    // } catch (IllegalArgumentException e) {}
                }
        });
        urlEditText.setText(getCachedUrl());
    }
}
