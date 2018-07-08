package com.quittle.rds

import android.content.Context
import android.content.SharedPreferences

private const val PREF_CACHED_URL : String = "cached_url"
private const val PREF_GROUP_DOWNLOADS : String = "downloads"

class DownloadMetadata(context:Context) {
    private val sharedPreferences : SharedPreferences =
            context.getSharedPreferences(PREF_GROUP_DOWNLOADS, Context.MODE_PRIVATE)

    fun getDownloadUrl() : String? {
        return sharedPreferences.getString(PREF_CACHED_URL, null)
    }

    fun setDownloadUrl(url:String) {
        sharedPreferences.edit().putString(PREF_CACHED_URL, url).apply()
    }
}
