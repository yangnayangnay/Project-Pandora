package com.example.project_pandora.ui.task;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.example.project_pandora.ui.PandoraActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.*;
import com.example.project_pandora.R;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.util.*;
import com.example.project_pandora.repository.TaskRepository;
import com.example.project_pandora.repository.TaskDisplaySettingsRepository;
import com.example.project_pandora.ui.widget.QuickDatePickerDialog;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.SerialDisposable;

public final class TaskDetailsActivity extends PandoraActivity {
    private static final int REQUEST_ASSIGNEE = 7101;
    private static final String[] STATUSES = {"", "IN_PROGRESS", "COMPLETED"};
    private final TaskDetailFilter.Query query = new TaskDetailFilter.Query();
    private final List<Map<String, Object>> tasks = new ArrayList<>();
    private final List<TagOption> tagOptions = new ArrayList<>();
    private final SerialDisposable request = new SerialDisposable();
    private final TaskDetailsAdapter adapter = new TaskDetailsAdapter();
    private Long user;
    private TaskRepository repository;
    private com.example.project_pandora.core.config.TaskPinPrefs pins;
    private TaskDisplaySettingsRepository taskDisplaySettings;
    private EditText keyword;
    private MaterialButton scope, status, assignee, tag, from, to, filterButton;
    private String assigneeName = "全部负责人";
    private TextView count, message, filterSummary;
    private MaterialButton retry;
    private ProgressBar progress;
    private HorizontalScrollView table;
    private LinearLayout filterPanel;
    private boolean loading, configuring, expanded, loadFailed, tableOnly;
    private androidx.appcompat.app.AlertDialog datePicker;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); restore(state);
        user = TokenManager.getInstance(this).getUserId(); repository = new TaskRepository(this);
        pins = new com.example.project_pandora.core.config.TaskPinPrefs(this, user);
        taskDisplaySettings = new TaskDisplaySettingsRepository(this);
        LinearLayout root = vertical(); root.setFitsSystemWindows(true); root.setBackgroundColor(color(R.color.color_bg_page));
        MaterialToolbar toolbar = new MaterialToolbar(this); toolbar.setTitle("任务明细"); toolbar.setTitleTextColor(color(R.color.color_text_primary)); toolbar.setBackgroundColor(color(R.color.color_surface));
        toolbar.setNavigationIcon(R.drawable.ic_back); toolbar.setNavigationIconTint(color(R.color.color_text_primary)); toolbar.setNavigationOnClickListener(v -> finish()); root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(56)));
        androidx.core.widget.NestedScrollView pageScroll = new androidx.core.widget.NestedScrollView(this);
        pageScroll.setFillViewport(true);
        LinearLayout body = vertical(); pageScroll.addView(body);
        root.addView(pageScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        boolean portrait = getResources().getConfiguration().orientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        tableOnly = !portrait;
        if (tableOnly) getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        MaterialButton landscape = darkButton(portrait ? "横屏" : "竖屏", v -> setRequestedOrientation(portrait ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        landscape.setIconResource(portrait ? R.drawable.ic_enter_landscape : R.drawable.ic_exit_landscape);
        landscape.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_END); landscape.setIconSize(dp(20)); landscape.setIconPadding(dp(5));
        landscape.setIconTint(landscape.getTextColors());
        landscape.setContentDescription(portrait ? "切换为横屏表格" : "切换为竖屏表格");
        androidx.appcompat.widget.Toolbar.LayoutParams landscapeParams = new androidx.appcompat.widget.Toolbar.LayoutParams(dp(104), dp(48));
        landscapeParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL; landscapeParams.setMarginEnd(dp(8)); toolbar.addView(landscape, landscapeParams);
        LinearLayout searchRow = horizontal(); searchRow.setPadding(dp(12), 0, dp(12), 0);
        keyword = new EditText(this); keyword.setSingleLine(true); keyword.setTextSize(14); keyword.setTextColor(color(R.color.color_text_primary)); keyword.setHint("搜索任务、正文或 #标签"); keyword.setText(query.keyword); keyword.setBackgroundResource(R.drawable.bg_task_filter_control); keyword.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams keywordParams = new LinearLayout.LayoutParams(0, dp(48), 1); keywordParams.setMargins(0, dp(6), dp(8), dp(6)); searchRow.addView(keyword, keywordParams);
        filterButton = darkButton("筛选", v -> toggleFilters());
        filterButton.setMaxLines(1);
        filterButton.setIconResource(R.drawable.ic_triangle_down); filterButton.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_END);
        filterButton.setIconSize(dp(16)); filterButton.setIconPadding(dp(4));
        filterButton.setIconTint(filterButton.getTextColors());
        searchRow.addView(filterButton, new LinearLayout.LayoutParams(dp(104), dp(48))); body.addView(searchRow);
        searchRow.setVisibility(tableOnly ? View.GONE : View.VISIBLE);
        filterPanel = vertical(); filterPanel.setBackgroundResource(R.drawable.bg_task_filter_panel);
        LinearLayout form = filterPanel; form.setPadding(dp(8), dp(6), dp(8), dp(6));
        configuring = true;
        LinearLayout row = horizontal();
        scope = filterControl(scopeLabel(), "任务范围", v -> showChoiceMenu(scope,
                new String[]{"全部相关任务", "我负责的", "我布置的"}, position -> { query.scope = position; scope.setText(scopeLabel()); render(); }));
        status = filterControl(statusLabel(), "任务状态", v -> showChoiceMenu(status,
                new String[]{"全部状态", "进行中", "已完成"}, position -> { query.status = STATUSES[position]; status.setText(statusLabel()); render(); }));
        row.addView(scope, filterControlParams()); row.addView(status, filterControlParams()); form.addView(row);
        row = horizontal();
        assignee = filterControl(assigneeName, "负责人", v -> startActivityForResult(new android.content.Intent(this, TaskFilterPeopleActivity.class), REQUEST_ASSIGNEE));
        assignee.setContentDescription("按部门和团队选择负责人"); row.addView(assignee, filterControlParams());
        tag = filterControl("全部标签", "标签", v -> showTagChoices()); row.addView(tag, filterControlParams()); form.addView(row);
        row = horizontal(); from = filterControl("", "起始日期", v -> chooseDate(true)); to = filterControl("", "结束日期", v -> chooseDate(false)); row.addView(from, filterControlParams()); row.addView(to, filterControlParams()); form.addView(row); renderDates();
        MaterialButton reset = button("重置筛选", v -> reset());
        form.removeAllViews();
        MaterialButton[] controls = {scope, status, assignee, tag, from, to, reset};
        int columns = portrait ? 2 : (getResources().getConfiguration().fontScale > 1.3f ? 3 : 4);
        LinearLayout controlRow = null;
        for (int i = 0; i < controls.length; i++) {
            if (i % columns == 0) { controlRow = horizontal(); form.addView(controlRow); }
            if (controls[i].getParent() instanceof ViewGroup) ((ViewGroup) controls[i].getParent()).removeView(controls[i]);
            controlRow.addView(controls[i], filterControlParams());
        }
        filterPanel.setVisibility(expanded && !tableOnly ? View.VISIBLE : View.GONE); LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(-1, -2); filterParams.setMargins(dp(12), dp(4), dp(12), dp(8)); body.addView(filterPanel, filterParams); configuring = false;
        filterSummary = text("", 13); filterSummary.setPadding(dp(16), dp(4), dp(16), dp(4)); body.addView(filterSummary);
        LinearLayout resultRow = horizontal(); resultRow.setPadding(dp(12), 0, dp(12), 0);
        resultRow.setVisibility(tableOnly ? View.GONE : View.VISIBLE);
        count = text("", 13); resultRow.addView(count, new LinearLayout.LayoutParams(0, -2, 1)); resultRow.addView(button("刷新", v -> load())); body.addView(resultRow);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); progress.setIndeterminate(true); body.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));
        message = text("", 14); message.setPadding(dp(20), dp(12), dp(20), dp(12)); message.setMinHeight(dp(48)); message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); body.addView(message);
        retry = button("重新加载", v -> load()); retry.setVisibility(View.GONE); body.addView(retry);
        table = new HorizontalScrollView(this); table.setFillViewport(true); table.setContentDescription("任务明细表格，左右滑动查看全部字段，上下滑动查看更多任务");
        LinearLayout content = vertical(); int width = TaskDetailsAdapter.totalWidth(this);
        content.addView(TaskDetailsAdapter.row(this, true), new LinearLayout.LayoutParams(width, -2));
        RecyclerView list = new RecyclerView(this); list.setLayoutManager(new LinearLayoutManager(this)); list.setAdapter(adapter); list.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        content.addView(list, new LinearLayout.LayoutParams(width, 0, 1)); table.addView(content, new HorizontalScrollView.LayoutParams(width, -1)); body.addView(table, new LinearLayout.LayoutParams(-1, dp(320))); setContentView(root);
        body.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (table.getVisibility() == View.GONE) return;
            int controlsHeight = table.getTop();
            int available = pageScroll.getHeight() - controlsHeight;
            int height = tableOnly ? Math.max(1, available) : Math.max(dp(240), available);
            ViewGroup.LayoutParams params = table.getLayoutParams();
            if (params.height != height) { params.height = height; table.setLayoutParams(params); }
        });
        keyword.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { query.keyword = s.toString(); render(); }
            public void afterTextChanged(Editable s) {}
        });
    }
    @Override protected void onResume() { super.onResume(); load(); }
    private void load() {
        if (user == null) { message.setText("登录信息已失效，请重新登录"); progress.setVisibility(View.GONE); table.setVisibility(View.GONE); return; }
        loading = true; loadFailed = false; retry.setVisibility(View.GONE); progress.setVisibility(View.VISIBLE);
        message.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE); message.setText("正在加载任务…");
        request.set(Observable.zip(
                repository.listTasksExtended(null, user, null, true),
                repository.listTasksExtended(null, null, user, true), TaskDetailFilter::merge)
            .observeOn(AndroidSchedulers.mainThread()).subscribe(data -> {
                loading = false; progress.setVisibility(View.GONE); tasks.clear(); tasks.addAll(data); fillOptions(); render();
            }, error -> { loading = false; loadFailed = tasks.isEmpty(); progress.setVisibility(View.GONE);
                message.setVisibility(View.VISIBLE); message.setText(tasks.isEmpty() ? "任务加载失败，请稍后重试" : "刷新失败，当前保留上次加载的任务"); retry.setVisibility(View.VISIBLE); }));
    }
    private void fillOptions() {
        Map<Long, Map<String, Object>> tags = new LinkedHashMap<>();
        for (Map<String, Object> task : tasks) {
            Object rawPeople = TaskTags.value(task, "assignees");
            if (rawPeople instanceof List<?>) for (Object raw : (List<?>) rawPeople) if (raw instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked") Map<String, Object> person = (Map<String, Object>) raw;
                Long id = NumberCompat.toLong(TaskTags.value(person, "id"));
                if (id != null && id.equals(query.assignee)) assigneeName = com.example.project_pandora.ui.people.PeopleDirectory.name(person);
            }
            Long legacyId = NumberCompat.toLong(TaskTags.value(task, "assignee_id"));
            if (legacyId != null && legacyId.equals(query.assignee)) assigneeName = TaskDetailFilter.person(task, "assignee");
            for (Map<String, Object> item : TaskTags.of(task)) {
                Long tagId = NumberCompat.toLong(TaskTags.value(item, "id")); if (tagId != null) tags.put(tagId, item);
            }
        }
        configuring = true;
        assignee.setText(query.assignee == null ? "全部负责人" : assigneeName);
        fillTagOptions(tags, query.tag); configuring = false;
    }
    private void fillTagOptions(Map<Long, Map<String, Object>> options, Long selected) {
        tagOptions.clear(); tagOptions.add(new TagOption(null, "全部标签", null));
        for (Map.Entry<Long, Map<String, Object>> entry : options.entrySet()) {
            Long color = NumberCompat.toLong(TaskTags.value(entry.getValue(), "color_index"));
            tagOptions.add(new TagOption(entry.getKey(), "#" + TaskTags.value(entry.getValue(), "name"), color));
        }
        TagOption selectedOption = findTag(selected);
        if (selected != null && selectedOption == null) {
            selectedOption = new TagOption(selected, "已失效的筛选项（可重置）", null);
            tagOptions.add(selectedOption);
        }
        tag.setText(selectedOption == null ? "全部标签" : selectedOption.label);
    }
    private void render() {
        if (table == null || loading || configuring || loadFailed) return;
        List<Map<String, Object>> result = TaskDetailFilter.apply(tasks, user, query);
        java.util.Set<String> pinned = pins.ids();
        TaskPinOrder.sort(result, pinned);
        adapter.setPins(pinned, task -> {
            Long id = NumberCompat.toLong(TaskTags.value(task, "id"));
            if (id != null && user != null) { pins.toggle(id); render(); }
        });
        adapter.submit(result);
        count.setText("显示 " + result.size() + " / " + tasks.size() + " 项 · 长按任务可置顶"
                + (TaskDetailsAdapter.totalWidth(this) > getResources().getDisplayMetrics().widthPixels ? " · 左右滑动查看" : ""));
        message.setText(tasks.isEmpty() ? "暂无相关任务" : "没有符合筛选条件的任务"); message.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE); table.setVisibility(result.isEmpty() ? View.GONE : View.VISIBLE);
        int filters = (query.scope == 0 ? 0 : 1) + (query.status.isEmpty() ? 0 : 1) + (query.assignee == null ? 0 : 1) + (query.tag == null ? 0 : 1) + (query.from.isEmpty() ? 0 : 1) + (query.to.isEmpty() ? 0 : 1);
        filterButton.setText("筛选");
        filterButton.setIconResource(expanded ? R.drawable.ic_triangle_up : R.drawable.ic_triangle_down);
        filterButton.setIconTint(filterButton.getTextColors());
        List<String> labels = new ArrayList<>();
        if (query.scope != 0) labels.add(scopeLabel()); if (!query.status.isEmpty()) labels.add(statusLabel());
        if (query.assignee != null) labels.add(assigneeName); if (query.tag != null) labels.add(tag.getText().toString());
        if (!query.from.isEmpty()) labels.add("起始 " + query.from); if (!query.to.isEmpty()) labels.add("结束 " + query.to);
        filterSummary.setText(String.join(" · ", labels)); filterSummary.setVisibility(tableOnly || labels.isEmpty() ? View.GONE : View.VISIBLE);
        filterButton.setContentDescription((expanded ? "收起" : "展开") + "筛选，已选 " + filters + " 项条件");
    }
    private void toggleFilters() { expanded = !expanded; filterPanel.setVisibility(expanded && !tableOnly ? View.VISIBLE : View.GONE); render(); }
    private void chooseDate(boolean start) {
        Calendar initial = Calendar.getInstance(); String selected = start ? query.from : query.to;
        if (!selected.isEmpty()) try { initial.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(selected)); } catch (Exception ignored) {}
        datePicker = QuickDatePickerDialog.create(this, initial, chosen -> {
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(chosen.getTime());
            if (start && !query.to.isEmpty() && date.compareTo(query.to) > 0 || !start && !query.from.isEmpty() && date.compareTo(query.from) < 0) { Toast.makeText(this, "筛选结束日期不能早于开始日期", Toast.LENGTH_LONG).show(); return; }
            if (start) query.from = date; else query.to = date; renderDates(); render();
        });
        datePicker.setButton(android.content.DialogInterface.BUTTON_NEUTRAL, "清除日期", (dialog, which) -> {
            if (start) query.from = ""; else query.to = ""; renderDates(); render();
        });
        datePicker.show();
    }
    private void renderDates() { from.setText(query.from.isEmpty() ? "起始日期" : query.from); to.setText(query.to.isEmpty() ? "结束日期" : query.to); from.setContentDescription("起始日期 " + query.from + "，点击选择或清除"); to.setContentDescription("结束日期 " + query.to + "，点击选择或清除"); }
    private void reset() {
        configuring = true; query.scope = 0; query.status = ""; query.assignee = null; query.tag = null; query.from = ""; query.to = ""; query.keyword = "";
        assigneeName = "全部负责人";
        keyword.setText(""); scope.setText(scopeLabel()); status.setText(statusLabel()); assignee.setText(assigneeName); tag.setText("全部标签"); renderDates(); configuring = false; render();
    }
    private void restore(Bundle state) {
        if (state == null) return; query.keyword = state.getString("keyword", ""); query.scope = state.getInt("scope"); query.status = state.getString("status", ""); query.from = state.getString("from", ""); query.to = state.getString("to", "");
        if (state.containsKey("assignee")) query.assignee = state.getLong("assignee"); if (state.containsKey("tag")) query.tag = state.getLong("tag"); assigneeName = state.getString("assigneeName", "全部负责人"); expanded = state.getBoolean("expanded");
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("keyword", query.keyword); state.putInt("scope", query.scope); state.putString("status", query.status); state.putString("from", query.from); state.putString("to", query.to); state.putBoolean("expanded", expanded);
        state.putString("assigneeName", assigneeName); if (query.assignee != null) state.putLong("assignee", query.assignee); if (query.tag != null) state.putLong("tag", query.tag); super.onSaveInstanceState(state);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_ASSIGNEE || resultCode != RESULT_OK || data == null) return;
        long id = data.getLongExtra(TaskFilterPeopleActivity.EXTRA_ID, -1L);
        query.assignee = id > 0 ? id : null;
        assigneeName = data.getStringExtra(TaskFilterPeopleActivity.EXTRA_NAME);
        if (assigneeName == null || assigneeName.isBlank()) assigneeName = "全部负责人";
        assignee.setText(assigneeName); render();
    }
    private MaterialButton filterControl(String title, String description, View.OnClickListener listener) {
        MaterialButton control = button(title, listener); control.setContentDescription(description);
        control.setGravity(Gravity.CENTER_VERTICAL); control.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        control.setMaxLines(1); control.setEllipsize(android.text.TextUtils.TruncateAt.END);
        control.setPadding(dp(10), 0, dp(10), 0); return control;
    }
    private void showChoiceMenu(MaterialButton anchor, String[] labels, java.util.function.IntConsumer selected) {
        androidx.appcompat.widget.PopupMenu menu = new androidx.appcompat.widget.PopupMenu(this, anchor);
        for (int i = 0; i < labels.length; i++) menu.getMenu().add(0, i, i, labels[i]);
        menu.setOnMenuItemClickListener(item -> { selected.accept(item.getItemId()); return true; });
        menu.show();
    }
    private void showTagChoices() {
        LinearLayout choices = vertical(); choices.setPadding(dp(12), dp(8), dp(12), dp(8));
        choices.setBackgroundColor(color(R.color.color_surface_soft));
        final androidx.appcompat.app.AlertDialog[] holder = new androidx.appcompat.app.AlertDialog[1];
        for (TagOption option : tagOptions) {
            LinearLayout choice = horizontal(); choice.setMinimumHeight(dp(52)); choice.setPadding(dp(12), dp(6), dp(12), dp(6));
            android.util.TypedValue selectable = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, selectable, true);
            choice.setBackgroundResource(selectable.resourceId); choice.setClickable(true); choice.setFocusable(true);
            TextView chip = text(option.label, 14);
            chip.setPadding(dp(10), dp(5), dp(10), dp(5));
            android.graphics.drawable.GradientDrawable chipBackground = new android.graphics.drawable.GradientDrawable();
            chipBackground.setCornerRadius(dp(10));
            int fill = option.color == null ? color(R.color.color_surface) : TaskTagColors.color(this, option.color.intValue());
            chipBackground.setColor(fill); chip.setTextColor(TaskTagColors.text(this, fill));
            chipBackground.setStroke(dp(1), color(R.color.color_divider)); chip.setBackground(chipBackground);
            choice.addView(chip, new LinearLayout.LayoutParams(-2, -2));
            TextView selectedMark = text(Objects.equals(query.tag, option.id) ? "✓" : "", 16);
            selectedMark.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            selectedMark.setTextColor(color(R.color.color_accent)); choice.addView(selectedMark, new LinearLayout.LayoutParams(0, -1, 1));
            choice.setContentDescription(option.label + (Objects.equals(query.tag, option.id) ? "，已选择" : ""));
            choice.setOnClickListener(v -> {
                query.tag = option.id; tag.setText(option.label); render(); if (holder[0] != null) holder[0].dismiss();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52)); params.setMargins(0, dp(2), 0, dp(2));
            choices.addView(choice, params);
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(this); scroll.addView(choices);
        holder[0] = new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("选择标签")
                .setView(scroll).setNegativeButton("取消", null).create(); holder[0].show();
    }
    private TagOption findTag(Long id) {
        if (id == null) return null;
        for (TagOption option : tagOptions) if (Objects.equals(id, option.id)) return option;
        return null;
    }
    private String scopeLabel() { return new String[]{"全部相关任务", "我负责的", "我布置的"}[Math.max(0, Math.min(query.scope, 2))]; }
    private String statusLabel() {
        int index = Arrays.asList(STATUSES).indexOf(query.status); return new String[]{"全部状态", "进行中", "已完成"}[Math.max(0, index)];
    }
    private LinearLayout vertical() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout horizontal() { LinearLayout v = new LinearLayout(this); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private MaterialButton button(String title, View.OnClickListener listener) {
        MaterialButton b = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(title); b.setTextSize(14); b.setAllCaps(false); b.setOnClickListener(listener); b.setMinHeight(dp(48)); b.setCornerRadius(dp(14));
        int[][] states = {new int[]{android.R.attr.state_pressed}, new int[]{android.R.attr.state_hovered}, new int[]{}};
        int[] fills = {color(R.color.color_accent_soft), color(R.color.color_surface_soft), color(R.color.color_surface)};
        b.setBackgroundTintList(new android.content.res.ColorStateList(states, fills));
        b.setStrokeColor(android.content.res.ColorStateList.valueOf(color(R.color.color_divider))); b.setStrokeWidth(dp(1));
        b.setRippleColor(android.content.res.ColorStateList.valueOf(color(R.color.color_accent_soft)));
        b.setStateListAnimator(android.animation.AnimatorInflater.loadStateListAnimator(this, R.animator.pandora_button_press));
        b.setElevation(dp(1));
        return b;
    }
    private MaterialButton darkButton(String title, View.OnClickListener listener) {
        MaterialButton b = button(title, listener);
        int[][] states = {new int[]{android.R.attr.state_pressed}, new int[]{android.R.attr.state_hovered}, new int[]{}};
        int[] fills = {color(R.color.color_accent_pressed), color(R.color.color_accent_hover), color(R.color.color_accent)};
        b.setBackgroundTintList(new android.content.res.ColorStateList(states, fills));
        b.setStrokeColor(new android.content.res.ColorStateList(states, fills));
        b.setTextColor(color(R.color.color_on_accent));
        b.setIconTint(b.getTextColors());
        return b;
    }

    private static final class TagOption {
        final Long id, color; final String label;
        TagOption(Long id, String label, Long color) { this.id = id; this.label = label; this.color = color; }
        @Override public String toString() { return label; }
    }
    private LinearLayout.LayoutParams filterControlParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        return params;
    }
    private TextView text(String value, int size) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color(R.color.color_text_secondary)); return v; }
    private int color(int resource) { return ContextCompat.getColor(this, resource); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override protected void onDestroy() { request.dispose(); if (datePicker != null) datePicker.dismiss(); super.onDestroy(); }
}
