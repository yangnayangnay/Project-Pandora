package com.example.project_pandora.ui.task;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.View;
import android.widget.*;
import android.text.*;
import androidx.core.content.ContextCompat;
import com.example.project_pandora.R;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.util.*;
import com.example.project_pandora.repository.TaskTagRepository;
import com.google.android.material.chip.*;
import com.google.android.material.button.MaterialButton;
import java.util.*;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.SerialDisposable;

/** Existing-tag selection; selecting a chip again promotes it to the first colour. */
public final class TaskTagPickerView extends LinearLayout {
    private final List<Map<String, Object>> available = new ArrayList<>();
    private final ArrayList<Long> selected = new ArrayList<>();
    private final ChipGroup selectedGroup, choices;
    private final EditText search;
    private final TextView message;
    private final MaterialButton retryButton;
    private final SerialDisposable request = new SerialDisposable();
    private Runnable onChanged;
    private boolean loaded;
    private boolean loading;
    private boolean loadFailed;

    public TaskTagPickerView(Context context, AttributeSet attrs) { super(context, attrs);
        setOrientation(VERTICAL); setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout heading = new LinearLayout(context); heading.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = text("# 标签（选填）"); title.setTextSize(16); title.setTypeface(null, 1);
        heading.addView(title, new LayoutParams(0, -2, 1));
        if (TaskTags.canManage(TokenManager.getInstance(context).getRole())) {
            MaterialButton manage = new MaterialButton(context, null, com.google.android.material.R.attr.borderlessButtonStyle);
            manage.setText("管理标签"); manage.setOnClickListener(v -> context.startActivity(new Intent(context, TaskTagManageActivity.class)));
            heading.addView(manage);
        }
        addView(heading); addView(text("首个标签决定任务底色；点击已选标签可设为首个"));
        selectedGroup = new ChipGroup(context); addView(selectedGroup);
        search = new EditText(context); search.setSingleLine(true); search.setHint("输入 # 搜索已有标签"); search.setTextColor(color(R.color.color_text_primary)); search.setTextSize(14);
        addView(search, new LayoutParams(-1, dp(48)));
        choices = new ChipGroup(context); addView(choices);
        message = text(""); message.setMinHeight(dp(48)); message.setGravity(android.view.Gravity.CENTER_VERTICAL);
        message.setClickable(false); message.setFocusable(false); message.setFocusableInTouchMode(false); addView(message);
        retryButton = new MaterialButton(context, null, com.google.android.material.R.attr.borderlessButtonStyle);
        retryButton.setText("重试"); retryButton.setVisibility(GONE); retryButton.setOnClickListener(v -> reload()); addView(retryButton);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { render(); }
            public void afterTextChanged(Editable e) {}
        });
    }
    public TaskTagPickerView(Context context) { this(context, null); }
    public void setOnChanged(Runnable callback) { onChanged = callback; }
    public ArrayList<Long> getSelectedIds() { return new ArrayList<>(selected); }
    public void setSelectedIds(List<Long> ids) {
        selected.clear(); for (Long id : ids) if (id != null && id > 0 && !selected.contains(id) && selected.size() < 10) selected.add(id);
        if (loaded) prune(); render();
    }
    public boolean isLoaded() { return loaded; }
    public void reload() {
        loading = true; loadFailed = false; loaded = false;
        retryButton.setVisibility(GONE); message.setText("标签加载中…");
        request.set(new TaskTagRepository(getContext()).list().observeOn(AndroidSchedulers.mainThread()).subscribe(rows -> {
            loading = false; loadFailed = false; retryButton.setVisibility(GONE);
            available.clear(); available.addAll(rows); loaded = true; prune(); render();
        }, error -> {
            loading = false; loadFailed = true;
            retryButton.setVisibility(VISIBLE); message.setText("标签加载失败");
        }));
    }
    private void prune() {
        boolean removed = selected.removeIf(id -> find(id) == null);
        if (removed) { Toast.makeText(getContext(), "已移除不存在的标签，请检查选择后保存", Toast.LENGTH_LONG).show(); changed(); }
    }
    private Map<String, Object> find(long id) {
        for (Map<String, Object> row : available) if (Objects.equals(id, NumberCompat.toLong(TaskTags.value(row, "id")))) return row;
        return null;
    }
    private void render() {
        selectedGroup.removeAllViews(); choices.removeAllViews();
        for (int i = 0; i < selected.size(); i++) {
            long id = selected.get(i); Map<String, Object> tag = find(id);
            if (tag == null) continue;
            Chip chip = chip(tag, (i == 0 ? "① " : "") + "#" + TaskTags.value(tag, "name") + (i == 0 ? " · 主色" : ""));
            chip.setCloseIconVisible(true); chip.setCloseIconContentDescription("移除标签 " + TaskTags.value(tag, "name"));
            chip.setOnCloseIconClickListener(v -> { selected.remove(id); render(); changed(); });
            chip.setOnClickListener(v -> { selected.remove(id); selected.add(0, id); render(); changed(); }); selectedGroup.addView(chip);
        }
        String query = TaskTags.searchText(search.getText().toString());
        for (Map<String, Object> tag : available) {
            Long id = NumberCompat.toLong(TaskTags.value(tag, "id"));
            if (id == null || selected.contains(id) || !Objects.toString(TaskTags.value(tag, "name"), "").toLowerCase(Locale.ROOT).contains(query)) continue;
            Chip chip = chip(tag, "#" + TaskTags.value(tag, "name"));
            chip.setOnClickListener(v -> {
                if (selected.size() >= 10) { Toast.makeText(getContext(), "每项任务最多10个标签", Toast.LENGTH_SHORT).show(); return; }
                selected.add(id); render(); changed();
            }); choices.addView(chip);
        }
        if (loading) message.setText("标签加载中…");
        else if (loadFailed) message.setText("标签加载失败");
        else if (loaded) message.setText(choices.getChildCount() > 0 ? "点击标签引用" : available.isEmpty() ? "暂无标签，请由创始人创建" : "没有更多匹配的标签");
    }
    private Chip chip(Map<String, Object> tag, String title) {
        Chip chip = new Chip(getContext()); chip.setText(title); chip.setTextColor(color(R.color.color_text_primary));
        Long index = NumberCompat.toLong(TaskTags.value(tag, "color_index"));
        chip.setChipBackgroundColor(ColorStateList.valueOf(TaskTagColors.color(getContext(), index == null ? 0 : index.intValue())));
        chip.setEnsureMinTouchTargetSize(true); return chip;
    }
    private TextView text(String value) { TextView v = new TextView(getContext()); v.setText(value); v.setTextSize(13); v.setTextColor(color(R.color.color_text_secondary)); return v; }
    private int color(int id) { return ContextCompat.getColor(getContext(), id); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void changed() { if (onChanged != null) onChanged.run(); }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); reload(); }
    @Override protected void onDetachedFromWindow() { request.set(null); super.onDetachedFromWindow(); }
}
