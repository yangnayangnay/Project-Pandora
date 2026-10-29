package com.example.project_pandora.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.util.NumberCompat;
import com.example.project_pandora.databinding.FragmentViewBinding;
import com.example.project_pandora.repository.SyncRoutingRepository;
import com.example.project_pandora.repository.TaskRepository;
import com.example.project_pandora.ui.task.TaskCreateActivity;
import com.example.project_pandora.ui.task.TaskDetailActivity;
import com.example.project_pandora.ui.widget.MonthCalendarLayout;
import com.google.android.material.tabs.TabLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.SerialDisposable;

public class ViewFragment extends Fragment {
    private FragmentViewBinding binding;
    private TaskRepository taskRepository;
    private SyncRoutingRepository syncRoutingRepository;
    private Long currentUserId;
    private int currentViewMode;
    private int currentPanelTab;
    private Calendar selectedDate = Calendar.getInstance();
    private final List<Map<String, Object>> mine = new ArrayList<>();
    private final List<Map<String, Object>> dispatched = new ArrayList<>();
    private final SerialDisposable request = new SerialDisposable();
    private final CompositeDisposable actions = new CompositeDisposable();
    private boolean switchingInFlight, statusDialogShowing;
    private boolean loading, loadFailed;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        taskRepository = new TaskRepository(requireContext());
        syncRoutingRepository = new SyncRoutingRepository(requireContext());
        currentUserId = TokenManager.getInstance(requireContext()).getUserId();
        binding.recyclerPanelTaskTable.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerPanelTaskTable.setNestedScrollingEnabled(false);
        setupTabs();
        binding.calendarView.setOnDateSelectedListener((year, month, day) -> {
            selectedDate.set(year, month - 1, day); renderDates();
        });
        binding.calendarView.setOnVerticalSwipeListener(new com.example.project_pandora.ui.widget.CalendarView.OnVerticalSwipeListener() {
            @Override public void onSwipeUp() { binding.containerMonthView.swipeUp(); }
            @Override public void onSwipeDown() { binding.containerMonthView.swipeDown(); }
        });
        binding.monthWeekStrip.setOnDateSelected(date -> { selectedDate = date; renderDates(); });
        binding.monthWeekStrip.setSelectionColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.example.project_pandora.R.color.color_month_selected));
        binding.calendarView.setOnTaskClick(this::openTaskDetail);
        binding.weekSchedule.setOnDateChanged(date -> { selectedDate = date; renderDates(); if (loadFailed) refreshTasks(); });
        binding.daySchedule.setOnDateChanged(date -> { selectedDate = date; renderDates(); if (loadFailed) refreshTasks(); });
        binding.weekSchedule.setOnTaskClick(this::openTaskDetail);
        binding.daySchedule.setOnTaskClick(this::openTaskDetail);
        binding.btnPreviousMonth.setOnClickListener(v -> shiftMonth(-1));
        binding.btnNextMonth.setOnClickListener(v -> shiftMonth(1));
        binding.btnCompactPreviousMonth.setOnClickListener(v -> shiftMonth(-1));
        binding.btnCompactNextMonth.setOnClickListener(v -> shiftMonth(1));
        binding.btnResetZoom.setOnClickListener(v -> {
            selectedDate = Calendar.getInstance();
            binding.containerMonthView.setState(MonthCalendarLayout.NORMAL, true);
            renderDates();
        });
        binding.containerMonthView.setOnStateChanged(monthState -> {
            if (binding == null) return;
            binding.expandedMonthHeader.setVisibility(monthState == MonthCalendarLayout.LIST ? View.GONE : View.VISIBLE);
            binding.compactMonthHeader.setVisibility(monthState == MonthCalendarLayout.LIST ? View.VISIBLE : View.GONE);
            binding.calendarView.setDetailed(monthState == MonthCalendarLayout.DETAIL);
            binding.monthDragHandle.setText(monthState == MonthCalendarLayout.DETAIL ? "⌃" : "━");
            binding.scrollPanelTaskTable.scrollTo(0, 0);
        });
        android.view.GestureDetector handleGesture = new android.view.GestureDetector(requireContext(),
                new android.view.GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(android.view.MotionEvent event) { return true; }
                    @Override public boolean onFling(android.view.MotionEvent first, android.view.MotionEvent last, float velocityX, float velocityY) {
                        if (first == null || Math.abs(last.getY() - first.getY()) < 20) return false;
                        if (velocityY < 0) binding.containerMonthView.swipeUp(); else binding.containerMonthView.swipeDown();
                        return true;
                    }
                    @Override public boolean onSingleTapUp(android.view.MotionEvent event) {
                        binding.monthDragHandle.performClick(); return true;
                    }
                });
        binding.monthDragHandle.setOnClickListener(v -> {
            if (binding.containerMonthView.getState() == MonthCalendarLayout.DETAIL) binding.containerMonthView.swipeUp();
            else binding.containerMonthView.swipeDown();
        });
        binding.monthDragHandle.setOnTouchListener((v, event) -> handleGesture.onTouchEvent(event));
        binding.textPanelTaskError.setOnClickListener(v -> refreshTasks());
        binding.fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TaskCreateActivity.class);
            if (currentUserId != null) intent.putExtra("assigneeId", currentUserId);
            startActivity(intent);
        });
        updateView(currentViewMode);
    }

    private void setupTabs() {
        for (String name : new String[]{"月视图", "周视图", "日视图"}) binding.tabLayout.addTab(binding.tabLayout.newTab().setText(name));
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { updateView(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(currentViewMode));
        binding.tabPanelTasks.addTab(binding.tabPanelTasks.newTab().setText("全部任务"));
        binding.tabPanelTasks.addTab(binding.tabPanelTasks.newTab().setText("我的任务"));
        binding.tabPanelTasks.addTab(binding.tabPanelTasks.newTab().setText("我布置的任务"));
        binding.tabPanelTasks.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { currentPanelTab = tab.getPosition(); renderTaskTable(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        binding.tabPanelTasks.selectTab(binding.tabPanelTasks.getTabAt(currentPanelTab));
    }

    private void shiftMonth(int direction) {
        selectedDate.set(Calendar.DAY_OF_MONTH, 1);
        selectedDate.add(Calendar.MONTH, direction);
        renderDates();
    }

    private void updateView(int mode) {
        currentViewMode = mode;
        binding.containerMonthView.setVisibility(mode == 0 ? View.VISIBLE : View.GONE);
        binding.containerWeekView.setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
        binding.containerDayView.setVisibility(mode == 2 ? View.VISIBLE : View.GONE);
        renderDates();
    }

    private void renderDates() {
        if (binding == null) return;
        String title = new SimpleDateFormat("yyyy年M月", Locale.CHINA).format(selectedDate.getTime());
        binding.textMonthLabel.setText(title); binding.compactMonthLabel.setText(title);
        binding.calendarView.setDate(selectedDate);
        binding.monthWeekStrip.setDate(selectedDate);
        if (currentViewMode == 1) binding.weekSchedule.setDate(selectedDate);
        if (currentViewMode == 2) binding.daySchedule.setDate(selectedDate);
    }

    private void refreshTasks() {
        if (binding == null || currentUserId == null) return;
        loading = true; loadFailed = false;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textPanelTaskError.setVisibility(View.GONE);
        request.set(Observable.zip(
                taskRepository.listTasksExtended(null, currentUserId, null, true),
                taskRepository.listTasksExtended(null, null, currentUserId, true),
                (assigned, created) -> new TaskLists(assigned, created))
                .observeOn(AndroidSchedulers.mainThread()).subscribe(result -> {
                    if (binding == null) return;
                    loading = false;
                    binding.progressBar.setVisibility(View.GONE);
                    mine.clear(); mine.addAll(result.mine);
                    dispatched.clear();
                    for (Map<String, Object> task : result.created) {
                        if (!currentUserId.equals(NumberCompat.toLong(task.get("assignee_id")))) dispatched.add(task);
                    }
                    renderTaskTable();
                    List<Map<String, Object>> all = mergedTasks();
                    binding.calendarView.setTasks(all);
                    binding.weekSchedule.setTasks(all);
                    binding.daySchedule.setTasks(all);
                }, error -> {
                    if (binding == null) return;
                    loading = false; loadFailed = true;
                    binding.progressBar.setVisibility(View.GONE);
                    renderTaskTable();
                    binding.weekSchedule.setError("任务加载失败"); binding.daySchedule.setError("任务加载失败");
                }));
    }

    private List<Map<String, Object>> mergedTasks() {
        Map<Object, Map<String, Object>> unique = new LinkedHashMap<>();
        for (Map<String, Object> task : mine) unique.put(task.get("id"), task);
        for (Map<String, Object> task : dispatched) unique.put(task.get("id"), task);
        return new ArrayList<>(unique.values());
    }

    private void renderTaskTable() {
        if (binding == null) return;
        List<Map<String, Object>> rows = currentPanelTab == 0 ? mergedTasks()
                : new ArrayList<>(currentPanelTab == 1 ? mine : dispatched);
        rows.sort(java.util.Comparator.comparing(task -> java.util.Objects.toString(task.get("start_time"), "9999")));
        String scope = currentPanelTab == 0 ? "全部任务" : currentPanelTab == 1 ? "我的任务" : "我布置的任务";
        binding.monthTaskHeading.setText(scope + " · " + rows.size() + " 项");
        binding.recyclerPanelTaskTable.setAdapter(new MonthTaskAdapter(rows, this::openTaskDetail, this::performQuickToggle));
        binding.textPanelTaskEmpty.setText("暂无任务");
        binding.textPanelTaskError.setVisibility(loadFailed ? View.VISIBLE : View.GONE);
        binding.textPanelTaskEmpty.setVisibility(!loading && !loadFailed && rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static final class TaskLists {
        final List<Map<String, Object>> mine, created;
        TaskLists(List<Map<String, Object>> mine, List<Map<String, Object>> created) { this.mine = mine; this.created = created; }
    }

    private void performQuickToggle(Map<String, Object> task) {
        String status = task.get("status") != null ? task.get("status").toString() : "";
        if ("CONFIRMED".equals(status)) {
            Toast.makeText(requireContext(), "已确认任务不可快速切换，请到详情页处理", Toast.LENGTH_SHORT).show();
            return;
        }
        Long assigneeId = com.example.project_pandora.core.util.NumberCompat.toLong(task.get("assignee_id"));
        if (assigneeId == null || currentUserId == null || !assigneeId.equals(currentUserId)) {
            openTaskDetail(task);
            return;
        }
        if (switchingInFlight || statusDialogShowing) return;
        Long taskId = com.example.project_pandora.core.util.NumberCompat.toLong(task.get("id"));
        if (taskId == null) return;
        String target = "COMPLETED".equals(status) ? "IN_PROGRESS" : "COMPLETED";
        String name = task.get("name") != null ? task.get("name").toString() : "";
        String targetDisplayName = "COMPLETED".equals(target) ? "已完成" : "进行中";

        // REQ-31-01: 二次确认弹窗——取消/框外不发起任何请求
        statusDialogShowing = true;
        com.example.project_pandora.ui.common.StatusConfirmDialog.show(requireContext(), name, targetDisplayName,
                () -> {
                    statusDialogShowing = false;
                    executeStatusTransition(task, taskId, target);
                },
                () -> statusDialogShowing = false);
    }

    /**
     * REQ-31-01/02: 确认后的状态变更链（防重 → 请求 → 局部更新 + 后台重拉校准）。
     */
    private void executeStatusTransition(Map<String, Object> task, Long taskId, String target) {
        switchingInFlight = true;
        actions.add(syncRoutingRepository.transitionTaskStatus(taskId, target)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            switchingInFlight = false;
                            if (binding == null) return;
                            String name = task.get("name") != null ? task.get("name").toString() : "";
                            Toast.makeText(requireContext(),
                                    "COMPLETED".equals(target)
                                            ? "『" + name + "』已完成"
                                            : "『" + name + "』已恢复未完成",
                                    Toast.LENGTH_SHORT).show();
                            // REQ-31-02: 局部更新该行（服务器已确认返回，非乐观更新）——消除重拉往返期旧值窗口
                            RecyclerView.Adapter panelAdapter = binding.recyclerPanelTaskTable.getAdapter();
                            if (panelAdapter instanceof MonthTaskAdapter) {
                                ((MonthTaskAdapter) panelAdapter)
                                        .updateTaskStatus(taskId, target);
                            }
                            refreshTasks();
                        },
                        error -> {
                            switchingInFlight = false;
                            if (binding == null) return;
                            Toast.makeText(requireContext(),
                                    error.getMessage() != null ? error.getMessage() : "切换失败，请重试",
                                    Toast.LENGTH_SHORT).show();
                        }
                ));
    }

    private void openTaskDetail(Map<String, Object> task) {
        Long id = NumberCompat.toLong(task.get("id"));
        if (id == null) id = NumberCompat.toLong(task.get("ID"));
        if (id != null) {
            Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
            intent.putExtra("taskId", id); startActivity(intent);
        }
    }

    @Override public void onResume() {
        super.onResume();
        if (binding != null) refreshTasks();
    }

    @Override public void onDestroyView() {
        request.set(null); actions.clear(); switchingInFlight = false; statusDialogShowing = false;
        binding = null;
        super.onDestroyView();
    }
}
