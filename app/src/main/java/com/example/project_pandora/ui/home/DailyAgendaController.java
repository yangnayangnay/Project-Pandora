package com.example.project_pandora.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.app.AlertDialog;
import com.example.project_pandora.core.util.NumberCompat;
import com.example.project_pandora.databinding.FragmentMindmapBinding;
import com.example.project_pandora.repository.DailyItemRepository;
import com.example.project_pandora.ui.widget.CalendarView;
import com.example.project_pandora.ui.widget.QuickDatePickerDialog;
import com.google.android.material.appbar.AppBarLayout;
import java.text.SimpleDateFormat;
import java.util.*;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.*;

/** Owns the personal month selection, checklist and home panel/calendar expansion. */
public final class DailyAgendaController {
    private final FragmentMindmapBinding binding;
    private final Context context;
    private final DailyItemRepository repository;
    private final SerialDisposable request = new SerialDisposable();
    private final CompositeDisposable actions = new CompositeDisposable();
    private final List<Map<String, Object>> items = new ArrayList<>();
    private final Set<Long> busy = new HashSet<>();
    private final DailyItemAdapter adapter;
    private final DailyItemEditor editor;
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    private final Calendar selected = Calendar.getInstance();
    private final AppBarLayout.OnOffsetChangedListener offsetListener;
    private AlertDialog datePicker;
    private boolean expanded, loading, failed, closed;
    private boolean restoringExpansion = true;

    public DailyAgendaController(Context context, FragmentMindmapBinding binding, Bundle state) {
        this.context = context; this.binding = binding; repository = new DailyItemRepository(context);
        if (state != null) {
            String day = state.getString("dailySelectedDate", "");
            if (DailyItems.validDate(day)) setDateFields(day);
            expanded = state.getBoolean("dailyExpanded");
        }
        editor = new DailyItemEditor(context, repository, item -> {
            if (closed) return;
            if (item != null) setDateFields(DailyItems.date(item));
            renderDate(); load();
        });
        adapter = new DailyItemAdapter(busy, this::complete, item -> editor.show(item, dayFormat.format(selected.getTime())));
        binding.recyclerDailyItems.setLayoutManager(new LinearLayoutManager(context)); binding.recyclerDailyItems.setNestedScrollingEnabled(false); binding.recyclerDailyItems.setAdapter(adapter);
        binding.dailyCalendar.setOnDateSelectedListener((year, month, day) -> { select(year, month - 1, day); revealDay(); });
        binding.dailyCalendar.setOnDayTasksClick(day -> revealDay());
        binding.dailyCalendar.setOnVerticalSwipeListener(new CalendarView.OnVerticalSwipeListener() {
            @Override public void onSwipeUp() { setExpanded(true); }
            @Override public void onSwipeDown() { if (binding.scrollDailyAgenda.getScrollY() == 0) setExpanded(false); }
        });
        binding.textDailyMonth.setOnClickListener(v -> chooseDate());
        binding.btnPreviousDailyMonth.setOnClickListener(v -> shiftMonth(-1)); binding.btnNextDailyMonth.setOnClickListener(v -> shiftMonth(1));
        binding.btnDailyToday.setOnClickListener(v -> { Calendar now = Calendar.getInstance(); select(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DATE)); });
        binding.btnAddDailyItem.setOnClickListener(v -> editor.show(null, dayFormat.format(selected.getTime())));
        binding.textDailyMessage.setOnClickListener(v -> { if (failed) load(); });
        binding.btnExpandAgenda.setOnClickListener(v -> setExpanded(!expanded));
        final boolean initialExpanded = expanded;
        offsetListener = (bar, offset) -> {
            if (closed || restoringExpansion || bar.getTotalScrollRange() == 0) return;
            if (offset == 0) applyExpanded(false);
            else if (Math.abs(offset) >= bar.getTotalScrollRange()) applyExpanded(true);
        };
        binding.homeAppBar.addOnOffsetChangedListener(offsetListener);
        binding.dailyCalendar.setDetailed(expanded); renderExpansion(); renderDate();
        binding.homeAppBar.post(() -> {
            if (closed) return;
            restoringExpansion = false; binding.homeAppBar.setExpanded(!initialExpanded, false);
        });
    }

    public void refresh() { if (!closed) load(); }
    public void saveState(Bundle state) { state.putString("dailySelectedDate", dayFormat.format(selected.getTime())); state.putBoolean("dailyExpanded", expanded); }
    private String month() { return dayFormat.format(selected.getTime()).substring(0, 7); }
    private void setDateFields(String day) {
        String[] parts = day.split("-"); selected.set(Calendar.DAY_OF_MONTH, 1); selected.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
    }
    private void select(int year, int month, int day) {
        if (!DailyItems.validDate(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day))) return;
        String previous = month(); selected.set(Calendar.DAY_OF_MONTH, 1); selected.set(year, month, day); renderDate();
        if (!previous.equals(month()) || failed) load(); else renderItems();
    }
    private void shiftMonth(int direction) {
        Calendar next = (Calendar) selected.clone(); next.set(Calendar.DAY_OF_MONTH, 1); next.add(Calendar.MONTH, direction);
        if (next.get(Calendar.YEAR) < 1900 || next.get(Calendar.YEAR) > 9999) return;
        select(next.get(Calendar.YEAR), next.get(Calendar.MONTH), 1);
    }
    private void chooseDate() {
        if (closed || (datePicker != null && datePicker.isShowing())) return;
        datePicker = QuickDatePickerDialog.create(context, selected, 1900, date -> {
            if (closed) return;
            select(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
            revealDay();
        });
        datePicker.show();
    }
    private void load() {
        loading = true; failed = false; items.clear(); adapter.submit(Collections.emptyList()); binding.dailyCalendar.setItems(Collections.emptyList());
        binding.dailyProgress.setVisibility(View.VISIBLE); renderMessage("正在加载个人事项…"); renderDate();
        String loadingMonth = month();
        request.set(repository.list(loadingMonth).observeOn(AndroidSchedulers.mainThread()).subscribe(rows -> {
            if (closed || !loadingMonth.equals(month())) return;
            loading = false; binding.dailyProgress.setVisibility(View.GONE); items.addAll(rows); renderItems();
        }, error -> {
            if (closed || !loadingMonth.equals(month())) return;
            loading = false; failed = true; binding.dailyProgress.setVisibility(View.GONE); renderMessage("事项加载失败，点击重试：" + error.getMessage());
        }));
    }
    private void renderDate() {
        binding.textDailyMonth.setText(new SimpleDateFormat("yyyy年M月", Locale.CHINA).format(selected.getTime()));
        binding.textDailyMonth.setContentDescription(binding.textDailyMonth.getText() + "，选择日期"); binding.dailyCalendar.setDate(selected);
        binding.textDailyHeading.setText(new SimpleDateFormat("M月d日", Locale.CHINA).format(selected.getTime()) + " · 个人事项");
    }
    private void renderItems() {
        if (loading || failed) return;
        List<Map<String, Object>> day = DailyItems.onDate(items, dayFormat.format(selected.getTime()));
        int done = 0; for (Map<String, Object> item : day) if (DailyItems.completed(item)) done++;
        binding.textDailyHeading.setText(new SimpleDateFormat("M月d日", Locale.CHINA).format(selected.getTime()) + " · 已完成 " + done + "/" + day.size());
        binding.dailyCalendar.setItems(items); adapter.submit(day);
        binding.textDailyMessage.setVisibility(day.isEmpty() ? View.VISIBLE : View.GONE); binding.textDailyMessage.setText("当天暂无事项，点击右侧＋记录");
    }
    private void renderMessage(String message) { binding.textDailyMessage.setText(message); binding.textDailyMessage.setVisibility(View.VISIBLE); }
    private void complete(Map<String, Object> item, boolean completed) {
        Long id = NumberCompat.toLong(DailyItems.value(item, "id")); if (id == null || !busy.add(id)) return;
        adapter.notifyDataSetChanged();
        actions.add(repository.complete(id, completed).observeOn(AndroidSchedulers.mainThread()).subscribe(saved -> {
            busy.remove(id); if (closed) return;
            if (loading) { load(); return; }
            for (int i = 0; i < items.size(); i++) if (Objects.equals(id, NumberCompat.toLong(DailyItems.value(items.get(i), "id")))) { items.set(i, saved); break; }
            renderItems();
        }, error -> { busy.remove(id); if (!closed) { adapter.notifyDataSetChanged(); Toast.makeText(context, "保存失败，请重试：" + error.getMessage(), Toast.LENGTH_LONG).show(); } }));
    }
    public void setExpanded(boolean value) {
        binding.scrollDailyAgenda.scrollTo(0, 0); binding.homeAppBar.setExpanded(!value, true);
    }
    private void applyExpanded(boolean value) {
        if (expanded == value) return;
        expanded = value; binding.dailyCalendar.setDetailed(value); renderExpansion();
    }
    private void renderExpansion() { binding.btnExpandAgenda.setText(expanded ? "显示板块" : "展开日历"); binding.btnExpandAgenda.setContentDescription(expanded ? "收起完整日历并显示事件纪和任务看板" : "收起顶部板块并展开完整日历"); }
    private void revealDay() {
        binding.scrollDailyAgenda.post(() -> {
            if (closed) return;
            android.graphics.Rect target = new android.graphics.Rect();
            binding.textDailyHeading.getDrawingRect(target);
            ((android.view.ViewGroup) binding.scrollDailyAgenda.getChildAt(0)).offsetDescendantRectToMyCoords(binding.textDailyHeading, target);
            binding.scrollDailyAgenda.smoothScrollTo(0, Math.max(0, target.top - Math.round(12 * context.getResources().getDisplayMetrics().density)));
        });
    }
    public void close() {
        closed = true; request.dispose(); actions.dispose(); editor.close(); if (datePicker != null) datePicker.dismiss(); binding.homeAppBar.removeOnOffsetChangedListener(offsetListener); binding.recyclerDailyItems.setAdapter(null);
    }
}
