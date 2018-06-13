package com.quittle.rds;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

@SuppressWarnings("PMD.AccessorMethodGeneration")
public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final boolean DOWNLOAD_ON_START_UP = true;

    private DownloadMetadata downloadMetadata;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        downloadMetadata = new DownloadMetadata(this);
        final String previousUrl = downloadMetadata.getDownloadUrl();
        if (previousUrl != null && DOWNLOAD_ON_START_UP) {
            DownloadJobService.startRunning(this, downloadMetadata.getDownloadUrl());
        }

        final EditText urlEditText = findViewById(R.id.url_edit_text);
        urlEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {
                final String text = sequence.toString().trim();
                Log.d(TAG, "Cancelling: " + text);
                DownloadJobService.stopRunning(MainActivity.this, text);
            }

            @Override
            public void onTextChanged(CharSequence sequence, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable editable) {
                final String text = editable.toString().trim();
                downloadMetadata.setDownloadUrl(text);
                DownloadJobService.startRunning(MainActivity.this, text);
            }
        });
        urlEditText.setText(downloadMetadata.getDownloadUrl());

        final ToggleButton toggleServiceButton = findViewById(R.id.toggle_service_button);
        toggleServiceButton.setChecked(DOWNLOAD_ON_START_UP);
        toggleServiceButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final Context context = MainActivity.this;
                final String url = downloadMetadata.getDownloadUrl();

                if (isChecked) {
                    DownloadJobService.startRunning(context, url);
                } else {
                    DownloadJobService.stopRunning(context, url);
                }
            }
        });
    }
}
