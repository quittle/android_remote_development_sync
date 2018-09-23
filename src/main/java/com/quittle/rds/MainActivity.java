package com.quittle.rds;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.List;

@SuppressWarnings("PMD.AccessorMethodGeneration")
public class MainActivity extends Activity {
    private static final int MAX_LOGCAT_OUTPUT_LENGTH = 10000;
    private static final boolean DOWNLOAD_ON_START_UP = true;

    private DownloadMetadata downloadMetadata;
    private Logcat logcat;
    private final Queue<String> logcatOutput = new ArrayDeque<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        downloadMetadata = new DownloadMetadata(this);
        final String previousUrl = downloadMetadata.getDownloadUrl();
        if (previousUrl != null && DOWNLOAD_ON_START_UP) {
            DownloadJobService.startRunning(this, downloadMetadata.getDownloadUrl());
        }

        logcat = new Logcat();
        logcat.addWatcher("com.quittle.rds", this::logcatLog);

        final EditText urlEditText = findViewById(R.id.url_edit_text);
        urlEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {
                final String text = sequence.toString().trim();
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

    @Override
    public void onDestroy() {
        logcat.clearAndStop();
        logcat = null;

        super.onDestroy();
    }

    private void logcatLog(List<String> messages) {
        logcatOutput.addAll(messages);
        while (logcatOutput.size() > MAX_LOGCAT_OUTPUT_LENGTH) {
            logcatOutput.remove();
        }

        StringBuilder sb = new StringBuilder();
        for (final String line : logcatOutput) {
            sb.append(line);
            sb.append(System.lineSeparator());
        }
        String logcatBody = sb.toString();
        runOnUiThread(() -> {
            final TextView tv = findViewById(R.id.logcat_output);
            tv.setText(logcatBody);
            final ScrollView sv = findViewById(R.id.logcat_container);
            sv.fullScroll(View.FOCUS_DOWN);
        });
    }
}
