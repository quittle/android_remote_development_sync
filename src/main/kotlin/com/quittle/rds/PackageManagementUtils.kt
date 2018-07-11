package com.quittle.rds

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri

import java.io.File

class PackageManagementUtils(context: Context) {
    private val context : Context = context.getApplicationContext()

    fun getInstalledPackageApk(packageName: String) : File? {
        val dir : String?; // May be null if the package was previously installed
        try {
            dir = context.getPackageManager().getApplicationInfo(packageName, 0).sourceDir;
        } catch (e: PackageManager.NameNotFoundException) {
            return null;
        }

        if (dir == null) {
            return null;
        }

        return File(dir);
    }

    fun getApkPackageName(uri: Uri) : String? {
        return context.getPackageManager()
                .getPackageArchiveInfo(uri.getPath(), 0)
                ?.applicationInfo
                ?.packageName;
    }
}
