package com.quittle.rds;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class MainActivity extends Activity {
    private DownloadMetadata downloadMetadata;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        downloadMetadata = new DownloadMetadata(this);
        DownloadJobService.startRunning(this);

        final EditText urlEditText = findViewById(R.id.url_edit_text);
        urlEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {
                // Not needed
            }

            @Override
            @SuppressWarnings("PMD.AccessorMethodGeneration")
            public void afterTextChanged(Editable editable) {
                final String text = editable.toString().trim();
                downloadMetadata.setDownloadUrl(text);
            }
        });
        urlEditText.setText(downloadMetadata.getDownloadUrl());
    }
}
