package com.example.project_pandora.core.autosave;

import android.app.AlertDialog;
import android.content.Context;

import java.util.Map;

public class DraftRestoreDialog {

    public interface OnRestoreListener {
        void onRestore(Map<String, Object> content);
    }

    public interface OnDiscardListener {
        void onDiscard();
    }

    public static void show(Context context, Map<String, Object> draftData,
                            OnRestoreListener onRestore, OnDiscardListener onDiscard) {
        new AlertDialog.Builder(context)
                .setTitle("发现未完成的编辑内容")
                .setMessage("是否恢复上次未完成的编辑？")
                .setPositiveButton("恢复", (dialog, which) -> {
                    if (onRestore != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> content = (Map<String, Object>) draftData.get("content");
                        onRestore.onRestore(content);
                    }
                })
                .setNegativeButton("放弃", (dialog, which) -> {
                    if (onDiscard != null) {
                        onDiscard.onDiscard();
                    }
                })
                .setCancelable(false)
                .show();
    }
}