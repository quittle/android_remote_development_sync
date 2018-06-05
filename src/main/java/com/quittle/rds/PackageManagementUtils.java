package com.quittle.rds;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

public class PackageManagementUtils {
    private final Context context;

    public PackageManagementUtils(final Context context) {
        this.context = context.getApplicationContext();
    }

    public String getInstalledPackageApk(final String packageName) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0).sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public String getApkPackageName(Uri uri) {
        final PackageInfo info = context.getPackageManager().getPackageArchiveInfo(uri.getPath(), 0);
        if (info == null) {
            return null;
        }

        return info.applicationInfo.packageName;
    }
}
