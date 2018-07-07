package com.quittle.rds;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.io.File;

public class PackageManagementUtils {
    private final Context context;

    public PackageManagementUtils(final Context context) {
        this.context = context.getApplicationContext();
    }

    public File getInstalledPackageApk(final String packageName) {
        try {
            final String dir = context.getPackageManager().getApplicationInfo(packageName, 0).sourceDir;
            if (dir == null) { // May be null if the package was previously installed
                return null;
            }
            return new File(dir);
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
