package com.example.project_pandora.ui;

import android.content.Context;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatActivity;
import com.example.project_pandora.core.config.FontSizePrefs;

/** Keeps native sp text and calendar measurements on the same user-selected scale. */
public abstract class PandoraActivity extends AppCompatActivity {
    private float appliedFontSize;
    @Override protected void attachBaseContext(Context base) {
        appliedFontSize = FontSizePrefs.get(base);
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.fontScale *= appliedFontSize;
        super.attachBaseContext(base.createConfigurationContext(configuration));
    }
    @Override protected void onResume() {
        super.onResume();
        if (Float.compare(appliedFontSize, FontSizePrefs.get(this)) != 0) recreate();
    }
}
