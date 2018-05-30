package com.quittle.rds;

import android.app.Activity;
import android.app.DownloadManager;
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

    private DownloadMetadata downloadMetadata;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        downloadMetadata = new DownloadMetadata(this);
        DownloadJobService.startRunning(this);
        Log.i(TAG, "Scheduling download service");

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
        urlEditText.setText(downloadMetadata.getDownloadUrl());
    }
}
