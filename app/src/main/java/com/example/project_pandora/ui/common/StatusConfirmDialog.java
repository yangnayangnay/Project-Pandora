package com.example.project_pandora.ui.common;

import android.content.Context;
import android.content.DialogInterface;

import com.example.project_pandora.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * REQ-31-01: 任务状态变更统一二次确认弹窗。
 * 视图页快速切换与任务详情页完成按钮两入口共用；文案走 strings.xml。
 * 取消/框外 dismiss 均无网络副作用（onCancel 回调由调用方重置弹窗标志）。
 */
public final class StatusConfirmDialog {

    private StatusConfirmDialog() {}

    /**
     * @param context                 容器上下文
     * @param taskName                任务名称（正文占位）
     * @param targetStatusDisplayName 目标状态显示名（如"已完成"/"进行中"）
     * @param onConfirm               点击确认后执行（由调用方发起既有状态变更链）
     * @param onCancel                取消/框外 dismiss 后执行（可为 null，用于重置弹窗可见标志）
     */
    public static void show(Context context, String taskName, String targetStatusDisplayName,
                            Runnable onConfirm, Runnable onCancel) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.confirm_status_title)
                .setMessage(context.getString(R.string.confirm_status_message, taskName, targetStatusDisplayName))
                .setPositiveButton(R.string.confirm_status_confirm, (DialogInterface d, int w) -> {
                    d.dismiss();
                    if (onConfirm != null) onConfirm.run();
                })
                .setNegativeButton(R.string.confirm_status_cancel, (DialogInterface d, int w) -> {
                    d.dismiss();
                    if (onCancel != null) onCancel.run();
                })
                .setOnCancelListener(d -> {
                    if (onCancel != null) onCancel.run();
                });
        builder.show();
    }
}