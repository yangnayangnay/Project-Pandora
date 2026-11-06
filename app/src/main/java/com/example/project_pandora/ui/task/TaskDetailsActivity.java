package com.example.project_pandora.ui.task;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.*;
import com.example.project_pandora.R;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.util.*;
import com.example.project_pandora.repository.TaskRepository;
import com.example.project_pandora.repository.TaskDisplaySettingsRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.SerialDisposable;

public final class TaskDetailsActivity extends AppCompatActivity {
    private static final String[] STATUSES = {"", "PENDING", "IN_PROGRESS", "COMPLETED", "CONFIRMED", "REJECTED"};
    private final TaskDetailFilter.Query query = new TaskDetailFilter.Query();
    private final List<Map<String, Object>> tasks = new ArrayList<>();
    private final ArrayList<Long> assigneeIds = new ArrayList<>(), tagIds = new ArrayList<>();
    private final SerialDisposable request = new SerialDisposable();
    private final TaskDetailsAdapter adapter = new TaskDetailsAdapter();
    private Long user;
    private TaskRepository repository;
    private TaskDisplaySettingsRepository taskDisplaySettings;
    private EditText keyword;
    private Spinner scope, status, assignee, tag;
    private MaterialButton from, to, filterButton;
    private TextView count, message;
    private ProgressBar progress;
    private HorizontalScrollView table;
    private ScrollView filterPanel;
    private boolean loading, configuring, expanded, loadFailed;
    private DatePickerDialog datePicker;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); restore(state);
        user = TokenManager.getInstance(this).getUserId(); repository = new TaskRepository(this);
        taskDisplaySettings = new TaskDisplaySettingsRepository(this);
        LinearLayout root = vertical(); root.setFitsSystemWindows(true); root.setBackgroundColor(color(R.color.color_bg_page));
        MaterialToolbar toolbar = new MaterialToolbar(this); toolbar.setTitle("任务明细"); toolbar.setTitleTextColor(color(R.color.color_text_primary)); toolbar.setBackgroundColor(color(R.color.color_surface));
        toolbar.setNavigationIcon(R.drawable.ic_back); toolbar.setNavigationIconTint(color(R.color.color_text_primary)); toolbar.setNavigationOnClickListener(v -> finish()); root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(56)));
        LinearLayout searchRow = horizontal(); searchRow.setPadding(dp(12), 0, dp(12), 0);
        keyword = new EditText(this); keyword.setSingleLine(true); keyword.setTextSize(14); keyword.setTextColor(color(R.color.color_text_primary)); keyword.setHint("搜索任务、正文或 #标签"); keyword.setText(query.keyword);
        searchRow.addView(keyword, new LinearLayout.LayoutParams(0, dp(52), 1)); filterButton = button("筛选", v -> toggleFilters()); searchRow.addView(filterButton); root.addView(searchRow);
        filterPanel = new ScrollView(this); LinearLayout form = vertical(); form.setPadding(dp(12), 0, dp(12), dp(8)); filterPanel.addView(form);
        configuring = true;
        LinearLayout row = horizontal();
        scope = spinner(row, new String[]{"全部相关任务", "我负责的", "我布置的"}, "任务范围"); scope.setSelection(query.scope);
        status = spinner(row, new String[]{"全部状态", "待接收", "进行中", "已完成", "已确认", "已退回"}, "任务状态"); status.setSelection(Arrays.asList(STATUSES).indexOf(query.status)); form.addView(row);
        row = horizontal(); assignee = spinner(row, new String[]{"全部负责人"}, "负责人"); tag = spinner(row, new String[]{"全部标签"}, "标签"); form.addView(row);
        row = horizontal(); from = button("", v -> chooseDate(true)); to = button("", v -> chooseDate(false)); row.addView(from, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(to, new LinearLayout.LayoutParams(0, -2, 1)); form.addView(row); renderDates();
        from.setOnLongClickListener(v -> { query.from = ""; renderDates(); render(); return true; }); to.setOnLongClickListener(v -> { query.to = ""; renderDates(); render(); return true; });
        form.addView(button("重置筛选", v -> reset()));
        filterPanel.setVisibility(expanded ? View.VISIBLE : View.GONE); root.addView(filterPanel, new LinearLayout.LayoutParams(-1, dp(220))); configuring = false;
        LinearLayout resultRow = horizontal(); resultRow.setPadding(dp(12), 0, dp(12), 0);
        count = text("", 13); resultRow.addView(count, new LinearLayout.LayoutParams(0, -2, 1)); resultRow.addView(button("刷新", v -> load())); root.addView(resultRow);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); progress.setIndeterminate(true); root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));
        message = text("", 14); message.setPadding(dp(20), dp(12), dp(20), dp(12)); message.setMinHeight(dp(48)); message.setOnClickListener(v -> load()); root.addView(message);
        table = new HorizontalScrollView(this); table.setFillViewport(true); table.setContentDescription("任务明细表格，左右滑动查看全部字段，上下滑动查看更多任务");
        LinearLayout content = vertical(); int width = TaskDetailsAdapter.totalWidth(this);
        content.addView(TaskDetailsAdapter.row(this, true), new LinearLayout.LayoutParams(width, -2));
        RecyclerView list = new RecyclerView(this); list.setLayoutManager(new LinearLayoutManager(this)); list.setAdapter(adapter); list.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        content.addView(list, new LinearLayout.LayoutParams(width, 0, 1)); table.addView(content, new HorizontalScrollView.LayoutParams(width, -1)); root.addView(table, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root);
        onSelection(scope, position -> { query.scope = position; render(); });
        onSelection(status, position -> { if (position >= 0) query.status = STATUSES[position]; render(); });
        onSelection(assignee, position -> { if (position >= 0 && position < assigneeIds.size()) query.assignee = assigneeIds.get(position); render(); });
        onSelection(tag, position -> { if (position >= 0 && position < tagIds.size()) query.tag = tagIds.get(position); render(); });
        keyword.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { query.keyword = s.toString(); render(); }
            public void afterTextChanged(Editable s) {}
        });
    }
    @Override protected void onResume() { super.onResume(); load(); }
    private void load() {
        if (user == null) { message.setText("登录信息已失效，请重新登录"); progress.setVisibility(View.GONE); table.setVisibility(View.GONE); return; }
        loading = true; loadFailed = false; progress.setVisibility(View.VISIBLE); message.setVisibility(View.VISIBLE); message.setText("正在加载任务…"); table.setVisibility(View.GONE);
        request.set(taskDisplaySettings.showCompletedForLists().flatMap(showCompleted -> Observable.zip(
                repository.listTasksExtended(null, user, null, showCompleted),
                repository.listTasksExtended(null, null, user, showCompleted), TaskDetailFilter::merge))
            .observeOn(AndroidSchedulers.mainThread()).subscribe(data -> {
                loading = false; progress.setVisibility(View.GONE); tasks.clear(); tasks.addAll(data); fillOptions(); render();
            }, error -> { loading = false; loadFailed = true; progress.setVisibility(View.GONE); count.setText("加载失败"); message.setText("加载失败，点击重试：" + error.getMessage()); }));
    }
    private void fillOptions() {
        Map<Long, String> people = new LinkedHashMap<>(), tags = new LinkedHashMap<>();
        for (Map<String, Object> task : tasks) {
            Long id = NumberCompat.toLong(TaskTags.value(task, "assignee_id")); if (id != null) people.put(id, TaskDetailFilter.person(task, "assignee"));
            for (Map<String, Object> item : TaskTags.of(task)) {
                Long tagId = NumberCompat.toLong(TaskTags.value(item, "id")); if (tagId != null) tags.put(tagId, "#" + TaskTags.value(item, "name"));
            }
        }
        configuring = true;
        fillSpinner(assignee, assigneeIds, people, query.assignee, "全部负责人");
        fillSpinner(tag, tagIds, tags, query.tag, "全部标签"); configuring = false;
    }
    private void fillSpinner(Spinner spinner, List<Long> ids, Map<Long, String> options, Long selected, String prompt) {
        List<String> labels = new ArrayList<>(); labels.add(prompt); ids.clear(); ids.add(null);
        for (Map.Entry<Long, String> entry : options.entrySet()) { ids.add(entry.getKey()); labels.add(entry.getValue()); }
        if (selected != null && !ids.contains(selected)) { ids.add(selected); labels.add("已失效的筛选项（可重置）"); }
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels)); spinner.setSelection(selected == null ? 0 : ids.indexOf(selected));
    }
    private void render() {
        if (table == null || loading || configuring || loadFailed) return;
        List<Map<String, Object>> result = TaskDetailFilter.apply(tasks, user, query); adapter.submit(result);
        count.setText("显示 " + result.size() + " / " + tasks.size() + " 项 · 左右滑动查看完整字段");
        message.setText(tasks.isEmpty() ? "暂无相关任务" : "没有符合筛选条件的任务"); message.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE); table.setVisibility(result.isEmpty() ? View.GONE : View.VISIBLE);
        int filters = (query.scope == 0 ? 0 : 1) + (query.status.isEmpty() ? 0 : 1) + (query.assignee == null ? 0 : 1) + (query.tag == null ? 0 : 1) + (query.from.isEmpty() ? 0 : 1) + (query.to.isEmpty() ? 0 : 1);
        filterButton.setText((expanded ? "收起" : "筛选") + (filters == 0 ? "" : " (" + filters + ")"));
    }
    private void toggleFilters() { expanded = !expanded; filterPanel.setVisibility(expanded ? View.VISIBLE : View.GONE); render(); }
    private void chooseDate(boolean start) {
        Calendar initial = Calendar.getInstance(); String selected = start ? query.from : query.to;
        if (!selected.isEmpty()) try { initial.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(selected)); } catch (Exception ignored) {}
        datePicker = new DatePickerDialog(this, (picker, year, month, day) -> {
            String date = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day);
            if (start && !query.to.isEmpty() && date.compareTo(query.to) > 0 || !start && !query.from.isEmpty() && date.compareTo(query.from) < 0) { Toast.makeText(this, "筛选结束日期不能早于开始日期", Toast.LENGTH_LONG).show(); return; }
            if (start) query.from = date; else query.to = date; renderDates(); render();
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)); datePicker.show();
    }
    private void renderDates() { from.setText(query.from.isEmpty() ? "起始日期" : query.from); to.setText(query.to.isEmpty() ? "结束日期" : query.to); from.setContentDescription("起始日期 " + query.from + "，长按清除"); to.setContentDescription("结束日期 " + query.to + "，长按清除"); }
    private void reset() {
        configuring = true; query.scope = 0; query.status = ""; query.assignee = null; query.tag = null; query.from = ""; query.to = ""; query.keyword = "";
        keyword.setText(""); scope.setSelection(0); status.setSelection(0); assignee.setSelection(0); tag.setSelection(0); renderDates(); configuring = false; render();
    }
    private void restore(Bundle state) {
        if (state == null) return; query.keyword = state.getString("keyword", ""); query.scope = state.getInt("scope"); query.status = state.getString("status", ""); query.from = state.getString("from", ""); query.to = state.getString("to", "");
        if (state.containsKey("assignee")) query.assignee = state.getLong("assignee"); if (state.containsKey("tag")) query.tag = state.getLong("tag"); expanded = state.getBoolean("expanded");
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("keyword", query.keyword); state.putInt("scope", query.scope); state.putString("status", query.status); state.putString("from", query.from); state.putString("to", query.to); state.putBoolean("expanded", expanded);
        if (query.assignee != null) state.putLong("assignee", query.assignee); if (query.tag != null) state.putLong("tag", query.tag); super.onSaveInstanceState(state);
    }
    private void onSelection(Spinner spinner, java.util.function.IntConsumer changed) { spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { if (!configuring) changed.accept(position); }
        public void onNothingSelected(AdapterView<?> parent) {}
    }); }
    private Spinner spinner(LinearLayout row, String[] labels, String description) { Spinner spinner = new Spinner(this); spinner.setContentDescription(description); spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels)); row.addView(spinner, new LinearLayout.LayoutParams(0, dp(52), 1)); return spinner; }
    private LinearLayout vertical() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout horizontal() { LinearLayout v = new LinearLayout(this); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private MaterialButton button(String title, View.OnClickListener listener) { MaterialButton b = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle); b.setText(title); b.setOnClickListener(listener); return b; }
    private TextView text(String value, int size) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color(R.color.color_text_secondary)); return v; }
    private int color(int resource) { return ContextCompat.getColor(this, resource); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override protected void onDestroy() { request.dispose(); if (datePicker != null) datePicker.dismiss(); super.onDestroy(); }
}
