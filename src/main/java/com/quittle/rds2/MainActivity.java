package com.quittle.rds2;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
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

    private DownloadManager downloadManager;
    private EditText urlEditText;
    private long ongoingDownload;

    public MainActivity() {
        DownloadBroadcastReceiver.setCallback(new Runnable() {
           @Override
           public void run() {
               DownloadManager.Query query = new DownloadManager.Query().setFilterById(new long[]{ongoingDownload});
               final Cursor cursor = downloadManager.query(query);
               cursor.moveToFirst();
               if (cursor.isBeforeFirst() || cursor.isAfterLast()) {
                   Log.e(TAG, "Unable to find download");
                   return;
               }
               final int localUriColumn = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
               if (localUriColumn < 0) {
                   Log.e(TAG, "Unable to find the local uri column");
                   return;
               }
               final String localUri = cursor.getString(localUriColumn);
               if (localUri == null) {
                   Log.e(TAG, "No local uri");
                   return;
               }
               final int remoteUriColumn = cursor.getColumnIndex(DownloadManager.COLUMN_URI);
               if (remoteUriColumn < 0) {
                   Log.e(TAG, "Unable to get remote uri column");
                   return;
               }
               final String remoteUri = cursor.getString(remoteUriColumn);
               if (remoteUri == null) {
                   Log.e(TAG, "No remote uri");
               }
               final Uri uri = Uri.parse(localUri);
               if (getApkPackageName(uri) != null) {
                   cacheUrl(remoteUri);
                   if (Objects.equals(hashFile(uri.getPath()), getInstalledPackageSignature(getApkPackageName(uri)))) {
                       Log.i(TAG, "APK installed with matching signature already");
                       return;
                   }

                   Intent promptInstall = new Intent(Intent.ACTION_VIEW)
                       .setDataAndType(uri, "application/vnd.android.package-archive")
                       .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

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
                    final String text = editable.toString();
                    final Uri uri = Uri.parse(text);

                    if (ongoingDownload != 0) {
                        if (downloadManager.remove(new long[]{ ongoingDownload }) != 1) {
                            Log.e(TAG, "Unable to cancel ongoing download");
                        }
                    }

                    try {
                        ongoingDownload = downloadManager.enqueue(new DownloadManager.Request(uri).setDestinationInExternalFilesDir(getApplicationContext(), null, "download.apk"));
                    } catch (IllegalArgumentException e) {}
                }
        });
        urlEditText.setText(getCachedUrl());
    }
}
