package com.quittle.rds;

import android.content.Context;
import android.content.SharedPreferences;

public class DownloadMetadata {
    private static final String PREF_CACHED_URL = "cached_url";
    private static final String PREF_GROUP_DOWNLOADS = "downloads";

    final private Context context;

    public DownloadMetadata(final Context context) {
        this.context = context.getApplicationContext();
    }

    // May return null
    public String getDownloadUrl() {
        return getSharedPreferences().getString(PREF_CACHED_URL, null);
    }

    public void setDownloadUrl(final String url) {
        getSharedPreferences().edit().putString(PREF_CACHED_URL, url).apply();
    }

    private SharedPreferences getSharedPreferences() {
        return context.getSharedPreferences(PREF_GROUP_DOWNLOADS, Context.MODE_PRIVATE);
    }
}