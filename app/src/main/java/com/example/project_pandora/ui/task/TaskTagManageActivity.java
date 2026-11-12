package com.example.project_pandora.ui.task;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.*;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.*;
import com.example.project_pandora.R;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.util.*;
import com.example.project_pandora.repository.TaskTagRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.*;
import java.util.*;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.*;

public final class TaskTagManageActivity extends com.example.project_pandora.ui.PandoraActivity {
    private final List<Map<String, Object>> rows = new ArrayList<>();
    private final CompositeDisposable actions = new CompositeDisposable();
    private final SerialDisposable loading = new SerialDisposable();
    private TaskTagRepository repository;
    private TextView message;
    private RecyclerView recycler;
    private boolean writing;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!TaskTags.canManage(TokenManager.getInstance(this).getRole())) { finish(); return; }
        repository = new TaskTagRepository(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setFitsSystemWindows(true); root.setBackgroundColor(color(R.color.color_bg_page));
        MaterialToolbar toolbar = new MaterialToolbar(this); toolbar.setTitle("标签管理"); toolbar.setTitleTextColor(color(R.color.color_text_primary));
        toolbar.setBackgroundColor(color(R.color.color_surface)); toolbar.setNavigationIcon(R.drawable.ic_back); toolbar.setNavigationIconTint(color(R.color.color_text_primary)); toolbar.setNavigationOnClickListener(v -> finish()); root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(56)));
        MaterialButton add = new MaterialButton(this); add.setText("新增标签"); add.setOnClickListener(v -> edit(null)); root.addView(add);
        message = text("", 14); message.setPadding(dp(16), dp(8), dp(16), dp(8)); message.setMinHeight(dp(48)); message.setOnClickListener(v -> load()); root.addView(message);
        recycler = new RecyclerView(this); recycler.setLayoutManager(new LinearLayoutManager(this)); recycler.setAdapter(new Adapter()); root.addView(recycler, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }
    @Override protected void onResume() { super.onResume(); if (repository != null) load(); }
    private void load() {
        message.setText("标签加载中…");
        loading.set(repository.list().observeOn(AndroidSchedulers.mainThread()).subscribe(data -> {
            rows.clear(); rows.addAll(data); recycler.getAdapter().notifyDataSetChanged(); message.setText(rows.isEmpty() ? "暂无标签，点击新增标签开始创建" : "共 " + rows.size() + " 个标签 · 改名和改色同步影响引用任务");
        }, error -> message.setText("加载失败，点击重试：" + error.getMessage())));
    }
    private void edit(Map<String, Object> tag) {
        if (writing) return;
        Long id = tag == null ? null : NumberCompat.toLong(TaskTags.value(tag, "id"));
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20), dp(8), dp(20), dp(8));
        EditText name = new EditText(this); name.setSingleLine(true); name.setHint("标签名称（最多32字）"); name.setText(tag == null ? "" : Objects.toString(TaskTags.value(tag, "name"), "")); name.setTextColor(color(R.color.color_text_primary)); form.addView(name);
        form.addView(text("选择标签和任务使用的颜色", 14));
        ChipGroup colors = new ChipGroup(this); colors.setSingleSelection(true); colors.setSelectionRequired(true);
        Long initial = tag == null ? null : NumberCompat.toLong(TaskTags.value(tag, "color_index"));
        int[] selected = {initial == null ? 0 : initial.intValue()};
        for (int i = 0; i < TaskTagColors.NAMES.length; i++) {
            final int index = i;
            Chip chip = new Chip(this); chip.setId(View.generateViewId()); chip.setText(TaskTagColors.NAMES[i]); chip.setCheckable(true); chip.setEnsureMinTouchTargetSize(true);
            int fill = TaskTagColors.color(this, i); int ink = TaskTagColors.text(this, fill);
            chip.setChipBackgroundColor(ColorStateList.valueOf(fill)); chip.setTextColor(ink);
            chip.setCheckedIconTint(ColorStateList.valueOf(ink)); colors.addView(chip); chip.setChecked(i == selected[0]);
            chip.setOnCheckedChangeListener((button, checked) -> { if (checked) selected[0] = index; });
        }
        form.addView(colors);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(id == null ? "新增标签" : "修改标签").setView(form).setNegativeButton("取消", null).setPositiveButton("保存", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (writing) return;
            if (name.getText().toString().trim().isEmpty()) { name.setError("请输入标签名称"); return; }
            writing = true; dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false); dialog.setCancelable(false);
            actions.add(repository.save(id, name.getText().toString(), selected[0]).observeOn(AndroidSchedulers.mainThread()).subscribe(saved -> {
                writing = false; dialog.dismiss(); load();
            }, error -> { writing = false; dialog.setCancelable(true); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true); name.setError(error.getMessage()); }));
        })); dialog.show();
    }
    private void delete(Map<String, Object> tag) {
        if (writing) return;
        new AlertDialog.Builder(this).setTitle("删除 #" + TaskTags.value(tag, "name"))
            .setMessage("删除后，所有任务将移除此标签；任务底色会使用下一个标签的颜色。")
            .setNegativeButton("取消", null).setPositiveButton("删除", (dialog, which) -> {
                if (writing) return; writing = true;
                actions.add(repository.delete(Objects.requireNonNull(NumberCompat.toLong(TaskTags.value(tag, "id")))).observeOn(AndroidSchedulers.mainThread()).subscribe(done -> {
                    writing = false; load();
                }, error -> { writing = false; Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); }));
            }).show();
    }
    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            LinearLayout row = new LinearLayout(TaskTagManageActivity.this); row.setPadding(dp(16), dp(8), dp(16), dp(8)); row.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = text("", 16); label.setPadding(dp(12), dp(12), dp(12), dp(12)); row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            MaterialButton edit = new MaterialButton(TaskTagManageActivity.this, null, com.google.android.material.R.attr.borderlessButtonStyle); edit.setText("修改"); row.addView(edit);
            MaterialButton delete = new MaterialButton(TaskTagManageActivity.this, null, com.google.android.material.R.attr.borderlessButtonStyle); delete.setText("删除"); row.addView(delete);
            row.setLayoutParams(new RecyclerView.LayoutParams(-1, -2)); return new RecyclerView.ViewHolder(row) {};
        }
        @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Map<String, Object> tag = rows.get(position); LinearLayout row = (LinearLayout) holder.itemView;
            TextView label = (TextView) row.getChildAt(0); label.setText("#" + TaskTags.value(tag, "name"));
            Long index = NumberCompat.toLong(TaskTags.value(tag, "color_index"));
            int fill = TaskTagColors.color(TaskTagManageActivity.this, index == null ? 0 : index.intValue());
            label.setBackgroundColor(fill); label.setTextColor(TaskTagColors.text(TaskTagManageActivity.this, fill));
            row.getChildAt(1).setOnClickListener(v -> edit(tag)); row.getChildAt(2).setOnClickListener(v -> delete(tag));
        }
        @Override public int getItemCount() { return rows.size(); }
    }
    private TextView text(String value, int size) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color(R.color.color_text_primary)); return t; }
    private int color(int resource) { return ContextCompat.getColor(this, resource); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override protected void onDestroy() { loading.dispose(); actions.dispose(); super.onDestroy(); }
}
