package com.example.project_pandora.core.template;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.project_pandora.repository.TemplateRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class TemplateMigrationHelper {

    private static final String PREF_NAME = "log_templates";
    private static final String KEY_TEMPLATES = "templates";
    private static final String KEY_MIGRATED = "template_migrated";

    private final Context context;
    private final TemplateRepository templateRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public TemplateMigrationHelper(Context context) {
        this.context = context;
        this.templateRepository = new TemplateRepository(context);
    }

    public void migrateIfNeeded() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_MIGRATED, false)) {
            return;
        }

        String templatesJson = prefs.getString(KEY_TEMPLATES, null);
        if (templatesJson == null || templatesJson.isEmpty()) {
            prefs.edit().putBoolean(KEY_MIGRATED, true).apply();
            return;
        }

        try {
            JSONArray array = new JSONArray(templatesJson);
            List<Map<String, Object>> templates = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Map<String, Object> content = new HashMap<>();
                content.put("workItem", obj.optString("workItem", ""));
                content.put("timeCost", obj.optString("timeCost", ""));

                Map<String, Object> template = new HashMap<>();
                String savedAt = obj.optString("savedAt", "");
                String name = "模板" + (i + 1);
                if (!savedAt.isEmpty()) {
                    name = "本地模板-" + savedAt;
                }
                template.put("name", name);
                template.put("content", content);
                templates.add(template);
            }

            if (templates.isEmpty()) {
                prefs.edit().putBoolean(KEY_MIGRATED, true).apply();
                return;
            }

            disposables.add(templateRepository.migrateTemplates("LOG", templates)
                    .subscribe(
                            result -> {
                                prefs.edit().putBoolean(KEY_MIGRATED, true).apply();
                            },
                            error -> {
                            }
                    ));
        } catch (Exception e) {
            prefs.edit().putBoolean(KEY_MIGRATED, true).apply();
        }
    }

    public void dispose() {
        disposables.clear();
    }
}