package com.example.project_pandora.ui.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.example.project_pandora.R;
import com.example.project_pandora.core.exception.BusinessException;
import com.example.project_pandora.core.util.ErrorMessageMapper;
import com.example.project_pandora.core.util.NetworkErrorClassifier;
import com.example.project_pandora.core.util.NumberCompat;
import com.example.project_pandora.databinding.DialogDailyItemBinding;
import com.example.project_pandora.repository.DailyItemRepository;
import com.example.project_pandora.ui.widget.QuickDatePickerDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import java.util.*;
import java.util.function.Consumer;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.SerialDisposable;

/** Keeps unsubmitted edits visible on errors and prevents duplicate writes. */
public final class DailyItemEditor {
    private final Context context;
    private final DailyItemRepository repository;
    private final Consumer<Map<String, Object>> onSaved;
    private final SerialDisposable writingRequest = new SerialDisposable();
    private AlertDialog dialog, deleteDialog;
    private AlertDialog datePicker;
    private DialogDailyItemBinding form;
    private String selectedDate;
    private Long id;
    private boolean writing;

    public DailyItemEditor(Context context, DailyItemRepository repository, Consumer<Map<String, Object>> onSaved) { this.context = context; this.repository = repository; this.onSaved = onSaved; }
    public void show(Map<String, Object> item, String date) {
        if (dialog != null && dialog.isShowing()) return;
        id = item == null ? null : NumberCompat.toLong(DailyItems.value(item, "id"));
        selectedDate = item == null ? date : DailyItems.date(item);
        form = DialogDailyItemBinding.inflate(LayoutInflater.from(context));
        form.textEditorTitle.setText(id == null ? "新增事项" : "编辑事项");
        form.editDailyContent.setText(item == null ? "" : DailyItems.content(item));
        form.editDailyContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                form.dailyContentLayout.setError(null);
                form.textDailyEditorError.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        renderDate();
        form.btnDailyDate.setOnClickListener(v -> chooseDate());
        form.btnSaveDailyItem.setOnClickListener(v -> save());
        form.btnCancelDailyItem.setOnClickListener(v -> dialog.dismiss());
        form.btnDeleteDailyItem.setVisibility(id == null ? View.GONE : View.VISIBLE);
        form.btnDeleteDailyItem.setOnClickListener(v -> confirmDelete());
        MaterialShapeDrawable background = new MaterialShapeDrawable(ShapeAppearanceModel.builder()
                .setAllCornerSizes(dp(24)).build());
        background.setFillColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.color_bg_page)));
        dialog = new MaterialAlertDialogBuilder(context).setView(form.getRoot()).setBackground(background)
                .setBackgroundInsetStart(0).setBackgroundInsetEnd(0).create();
        dialog.setOnShowListener(ignored -> {
            Window window = dialog.getWindow();
            if (window == null) return;
            window.setLayout(Math.min(dp(480), context.getResources().getDisplayMetrics().widthPixels - dp(32)),
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        });
        dialog.setOnDismissListener(ignored -> {
            if (datePicker != null) { datePicker.dismiss(); datePicker = null; }
        });
        dialog.show();
    }
    private void renderDate() {
        String[] parts = selectedDate.split("-");
        form.btnDailyDate.setText(String.format(Locale.CHINA, "%s年%d月%d日", parts[0],
                Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
        form.btnDailyDate.setContentDescription("所属日期：" + selectedDate + "，点击选择日期");
    }
    private void chooseDate() {
        if (datePicker != null && datePicker.isShowing()) return;
        Calendar calendar = Calendar.getInstance(); String[] parts = selectedDate.split("-");
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
        datePicker = QuickDatePickerDialog.create(context, calendar, 1900, date -> {
            if (dialog == null || !dialog.isShowing()) return;
            selectedDate = String.format(Locale.ROOT, "%04d-%02d-%02d", date.get(Calendar.YEAR),
                    date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
            renderDate();
        });
        datePicker.show();
    }
    private void save() {
        if (writing) return;
        String text = form.editDailyContent.getText().toString().trim();
        if (text.isEmpty()) { form.dailyContentLayout.setError("请输入事项内容"); form.editDailyContent.requestFocus(); return; }
        if (!DailyItems.validDate(selectedDate)) { showError("请选择有效日期"); return; }
        setWriting(true);
        form.btnSaveDailyItem.setText("正在保存…");
        writingRequest.set(repository.save(id, text, selectedDate).observeOn(AndroidSchedulers.mainThread()).subscribe(saved -> {
            setWriting(false); dialog.dismiss(); onSaved.accept(saved);
        }, error -> { setWriting(false); showError("保存失败：" + errorMessage(error)); }));
    }
    private void confirmDelete() {
        if (writing) return;
        deleteDialog = new MaterialAlertDialogBuilder(context).setTitle("删除此事项？").setMessage("删除后将从个人日历中移除。").setNegativeButton("取消", null).setPositiveButton("删除", null).create();
        deleteDialog.setOnShowListener(ignored -> deleteDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (writing) return; setWriting(true); deleteDialog.setCancelable(false); deleteDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); deleteDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
            form.btnDeleteDailyItem.setText("正在删除…");
            writingRequest.set(repository.delete(id).observeOn(AndroidSchedulers.mainThread()).subscribe(done -> {
                setWriting(false); deleteDialog.dismiss(); dialog.dismiss(); onSaved.accept(null);
            }, error -> { setWriting(false); deleteDialog.dismiss(); Toast.makeText(context, "删除失败：" + errorMessage(error), Toast.LENGTH_LONG).show(); }));
        })); deleteDialog.show();
    }
    private void setWriting(boolean value) {
        writing = value;
        form.dailyContentLayout.setEnabled(!value); form.btnDailyDate.setEnabled(!value); dialog.setCancelable(!value);
        form.btnSaveDailyItem.setEnabled(!value); form.btnCancelDailyItem.setEnabled(!value); form.btnDeleteDailyItem.setEnabled(!value);
        if (!value) { form.btnSaveDailyItem.setText("保存事项"); form.btnDeleteDailyItem.setText("删除事项"); }
        else form.textDailyEditorError.setVisibility(View.GONE);
    }
    private void showError(String message) { form.textDailyEditorError.setText(message); form.textDailyEditorError.setVisibility(View.VISIBLE); }
    private static String errorMessage(Throwable error) {
        if (error instanceof BusinessException) {
            BusinessException business = (BusinessException) error;
            return ErrorMessageMapper.map(business.getCode(), business.getMessage());
        }
        return NetworkErrorClassifier.classify(error);
    }
    private int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    public void close() { writingRequest.dispose(); if (datePicker != null) datePicker.dismiss(); if (deleteDialog != null) deleteDialog.dismiss(); if (dialog != null) dialog.dismiss(); }
}
