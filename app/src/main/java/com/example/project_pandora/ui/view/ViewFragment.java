package com.example.project_pandora.ui.view;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import android.view.Gravity;
import android.view.LayoutInflater;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.R;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.databinding.FragmentViewBinding;
import com.example.project_pandora.repository.SyncRoutingRepository;
import com.example.project_pandora.repository.TaskRepository;
import com.example.project_pandora.repository.ViewRepository;
import com.example.project_pandora.ui.task.TaskCreateActivity;
import com.example.project_pandora.ui.task.TaskDetailActivity;
import com.example.project_pandora.ui.task.TaskTableAdapter;
import com.example.project_pandora.ui.widget.CalendarView;
import com.example.project_pandora.ui.widget.TaskSpan;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class ViewFragment extends Fragment {

    private FragmentViewBinding binding;
    private ViewRepository viewRepository;
    private TaskRepository taskRepository;
    private Long currentUserId;

    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat monthLabelFmt = new SimpleDateFormat("yyyy年MM月", Locale.getDefault());
    private final SimpleDateFormat weekLabelFmt = new SimpleDateFormat("MM月dd日", Locale.getDefault());
    private final SimpleDateFormat dayLabelFmt = new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA);
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private int currentViewMode = 0;
    private Calendar selectedDate = Calendar.getInstance();
    private List<Map<String, Object>> allMonthTasks = new ArrayList<>();

    // Round24 REQ-24-02: 三视图共享底部任务面板（BottomSheet 三态，六列表格原样搬运）
    private BottomSheetBehavior<LinearLayout> taskPanelBehavior;
    private boolean taskPanelOffsetConfigured = false;
    private int currentPanelTab = 0;
    private int panelLoadedForTab = -1;

    // Round27 REQ-27-04/05/06: 状态格快速切换
    private SyncRoutingRepository syncRoutingRepository;
    private boolean switchingInFlight = false;
    /** REQ-31-01: 确认弹窗可见标志（与 switchingInFlight 分离——弹窗展示期间不算请求中，防叠窗） */
    private boolean statusDialogShowing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewRepository = new ViewRepository(requireContext());
        taskRepository = new TaskRepository(requireContext());
        syncRoutingRepository = new SyncRoutingRepository(requireContext());
        currentUserId = TokenManager.getInstance(requireContext()).getUserId();

        setupTabs();
        setupCalendarView();
        setupMonthNavigation();
        setupPanelTabs();
        setupTaskPanel();
        setupPanelEntryButton();
        setupFab();

        updateView(0);
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("月视图"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("周视图"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("日视图"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentViewMode = tab.getPosition();
                updateView(currentViewMode);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupCalendarView() {
        // Round24 REQ-24-02: 日期点选仅更新 selectedDate（供月导航/周日起算），不再按日过滤面板
        binding.calendarView.setOnDateSelectedListener((year, month, day) ->
                selectedDate.set(year, month - 1, day));
        // R36 REQ-36-B3: 旧日历垂直手势桥接废止（月视图缩放/单指滑动一律不触发面板）——
        // 面板唯一展开入口改为 btnPanelEntry 按钮上的上滑手势（见 setupPanelEntryButton）
    }

    private void setupMonthNavigation() {
        binding.btnPreviousMonth.setOnClickListener(v -> {
            binding.calendarView.previousMonth();
            selectedDate.set(Calendar.DAY_OF_MONTH, 1);
            selectedDate.add(Calendar.MONTH, -1);
            loadMonthViewData();
        });
        binding.btnNextMonth.setOnClickListener(v -> {
            binding.calendarView.nextMonth();
            selectedDate.set(Calendar.DAY_OF_MONTH, 1);
            selectedDate.add(Calendar.MONTH, 1);
            loadMonthViewData();
        });
    }

    /**
     * Round24 REQ-24-02: 面板任务 Tab 初始化
     * "我的任务"必有；非 EMPLOYEE 角色追加"我布置的任务"（沿 TaskShowActivity 角色判定）
     */
    private void setupPanelTabs() {
        String role = TokenManager.getInstance(requireContext()).getRole();
        binding.tabPanelTasks.addTab(binding.tabPanelTasks.newTab().setText("我的任务"));
        if (role != null && !role.equals("EMPLOYEE")) {
            binding.tabPanelTasks.addTab(binding.tabPanelTasks.newTab().setText("我布置的任务"));
        }

        binding.tabPanelTasks.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentPanelTab = tab.getPosition();
                loadPanelTasks(currentPanelTab);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * Round24 REQ-24-02: 底部任务面板状态机（延续 Round23 三态语义）
     * 收起态清空数据绑定（禁止"半常驻"残留）；半展开/展开态加载当前 Tab 任务
     */
    private void setupTaskPanel() {
        taskPanelBehavior = BottomSheetBehavior.from(binding.sheetTaskPanel);
        taskPanelBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

                if (binding == null) return;
                if (newState == BottomSheetBehavior.STATE_COLLAPSED
                        || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    // R36 REQ-36-B4: 隐藏/收起清空数据；HIDDEN 态额外 GONE（初始隐藏方案的闭合端）
                    clearPanelContent();
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        bottomSheet.setVisibility(View.GONE);
                    }
                } else if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED
                        || newState == BottomSheetBehavior.STATE_EXPANDED) {
                    loadPanelTasks(currentPanelTab);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
        binding.textPanelTaskError.setOnClickListener(v -> loadPanelTasks(currentPanelTab));
        // Round25 L1: 双击把手一步直达收起（全展开→收起，省去半展开中转）；
        // 单次下滑语义不变（全展开→半展开）。
        // R25-05 修复：onTouch 必须返回 true 消费事件——返回 false 时 DOWN 未被消费，
        // 系统 UP/MOVE 不再派发到本视图，GestureDetector 收不到完整 DOWN-UP 序列，双击永不触发；
        // 拖拽不受影响：CoordinatorLayout 的 Behavior.onInterceptTouchEvent 在 MOVE 超 touchSlop 时
        // 优先于子视图消费拦截手势，BottomSheet 拖拽照常工作
        GestureDetector handleTapDetector = new GestureDetector(requireContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(@NonNull MotionEvent e) {

                        if (taskPanelBehavior != null) {
                            // R36 REQ-36-B4: 双击把手一步回隐藏态（面板完全不可见）
                            taskPanelBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                            return true;
                        }
                        return false;
                    }
                });
        binding.viewSheetDragHandle.setOnTouchListener((v, event) -> {

            handleTapDetector.onTouchEvent(event);
            return true;
        });
        binding.getRoot().post(this::ensureTaskPanelConfigured);
        // R36 REQ-36-B1 终案：XML visibility=gone + 代码层双保险强制 GONE
        // （XML 声明在真机实测未生效——疑似资源合并缓存，此行兜底并打日志诊断）
        binding.sheetTaskPanel.setVisibility(View.GONE);
        android.util.Log.d("R36DEBUG", "setupTaskPanel: panel forced GONE (was " + binding.sheetTaskPanel.getVisibility() + ")");
    }

    /**
     * R37 REQ-37-A1: 面板唯一展开入口——纯单击（R36 上滑手势方案按用户指示替换删除）。
     * 月视图缩放/单指滑动仍不触发面板；expandTaskPanel 幂等（连点安全）。
     */
    private void setupPanelEntryButton() {
        binding.btnPanelEntry.setOnClickListener(v -> expandTaskPanel());
    }

    /** R36 REQ-36-B3: 面板展开链——GONE→VISIBLE→post(HALF_EXPANDED)（VISIBLE 后需布局完成才 setState） */
    private void expandTaskPanel() {
        if (binding == null || taskPanelBehavior == null) return;
        if (binding.sheetTaskPanel.getVisibility() != View.VISIBLE) {
            binding.sheetTaskPanel.setVisibility(View.VISIBLE);
        }
        binding.sheetTaskPanel.post(() -> {
            if (binding == null || taskPanelBehavior == null) return;
            taskPanelBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        });
    }

    /**
     * Round24 REQ-24-02: 面板全展开偏移动态计算
     * 布局 post 后取视图 Tab 栏 + 月导航条实测高度，保证全展开时标题/导航区可辨识
     */
    private void ensureTaskPanelConfigured() {
        if (taskPanelOffsetConfigured || binding == null || taskPanelBehavior == null) return;
        int tabBarHeight = binding.tabLayout.getHeight();
        int monthNavHeight = binding.layoutMonthNav.getHeight();
        if (tabBarHeight <= 0 || monthNavHeight <= 0) {
            binding.getRoot().post(this::ensureTaskPanelConfigured);
            return;
        }
        taskPanelOffsetConfigured = true;
        int expandedOffset = tabBarHeight + monthNavHeight;
        taskPanelBehavior.setExpandedOffset(expandedOffset);

        // Round25 R25-04: 面板高度 = 根容器高 - expandedOffset。
        // 此前面板 match_parent 撑满根容器，expanded 态底边越出 CoordinatorLayout 底部（被底部导航遮挡），
        // NSV 视口虚大导致内容溢出仅 6px，表格末行落在遮挡区永远不可见且垂直滚动无行程。
        // 收窄面板高度后 NSV 视口回到可见区，恢复真实溢出量，垂直滚动恢复。
        ViewGroup.LayoutParams lp = binding.sheetTaskPanel.getLayoutParams();
        int panelHeight = binding.getRoot().getHeight() - expandedOffset;
        if (panelHeight > 0 && lp.height != panelHeight) {
            lp.height = panelHeight;
            binding.sheetTaskPanel.setLayoutParams(lp);
        }
    }

    /**
     * Round24 REQ-24-02: 面板任务加载
     * 我的=assignee 过滤、我布置的=dispatcher 过滤、含已完成（沿 TaskShowActivity 参数语义）
     * 仅展开时拉取；panelLoadedForTab 去重防止拖拽中间态重复请求；失败后重置允许重试
     */
    private void loadPanelTasks(int tab) {
        if (binding == null || currentUserId == null || currentUserId < 0) return;
        if (taskPanelBehavior != null
                && taskPanelBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            return;
        }
        if (panelLoadedForTab == tab) return;
        panelLoadedForTab = tab;

        binding.textPanelTaskEmpty.setVisibility(View.GONE);
        binding.textPanelTaskError.setVisibility(View.GONE);
        binding.scrollPanelTaskTable.setVisibility(View.VISIBLE);

        taskRepository.listTasksExtended(null,
                tab == 0 ? currentUserId : null,
                tab == 1 ? currentUserId : null,
                true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        tasks -> {
                            if (binding == null) return;
                            if (taskPanelBehavior != null
                                    && taskPanelBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                                return;
                            }
                            // Round27 REQ-27-08: "我布置的任务"排除自建自派（展示层过滤，后端零改动，D-5）
                            if (tab == 1 && currentUserId != null) {
                                tasks.removeIf(t -> currentUserId.equals(
                                        com.example.project_pandora.core.util.NumberCompat.toLong(t.get("assignee_id"))));
                            }
                            updatePanelTaskTable(tasks);
                        },
                        error -> {
                            if (binding == null) return;
                            panelLoadedForTab = -1;
                            if (taskPanelBehavior != null
                                    && taskPanelBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                                return;
                            }
                            showPanelTaskError();
                        }
                );
    }

    private void updatePanelTaskTable(List<Map<String, Object>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            binding.textPanelTaskEmpty.setVisibility(View.VISIBLE);
            binding.textPanelTaskError.setVisibility(View.GONE);
            binding.scrollPanelTaskTable.setVisibility(View.GONE);
            return;
        }

        binding.textPanelTaskEmpty.setVisibility(View.GONE);
        binding.textPanelTaskError.setVisibility(View.GONE);
        binding.scrollPanelTaskTable.setVisibility(View.VISIBLE);
        binding.recyclerPanelTaskTable.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerPanelTaskTable.setAdapter(new TaskTableAdapter(tasks, currentUserId, this::performQuickToggle));
    }

    /**
     * Round27 REQ-27-04/05: 完成状态单元格快速切换编排
     * 判定顺序：CONFIRMED 终态保护 → 责任人判定（非本人跳详情）→ 防重 → 状态映射切换
     * PENDING/IN_PROGRESS/REJECTED → COMPLETED；COMPLETED → IN_PROGRESS（D-4 定稿）
     */
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
        syncRoutingRepository.transitionTaskStatus(taskId, target)
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
                            if (panelAdapter instanceof com.example.project_pandora.ui.task.TaskTableAdapter) {
                                ((com.example.project_pandora.ui.task.TaskTableAdapter) panelAdapter)
                                        .updateTaskStatus(taskId, target);
                            }
                            panelLoadedForTab = -1;
                            loadPanelTasks(currentPanelTab);
                            updateView(currentViewMode);
                        },
                        error -> {
                            switchingInFlight = false;
                            if (binding == null) return;
                            Toast.makeText(requireContext(),
                                    error.getMessage() != null ? error.getMessage() : "切换失败，请重试",
                                    Toast.LENGTH_SHORT).show();
                        }
                );
    }

    /**
     * Round24 REQ-24-02: 面板加载失败提示（面板内展示，点击重试）
     */
    private void showPanelTaskError() {
        if (binding == null) return;
        binding.textPanelTaskEmpty.setVisibility(View.GONE);
        binding.scrollPanelTaskTable.setVisibility(View.GONE);
        binding.textPanelTaskError.setVisibility(View.VISIBLE);
        RecyclerView.Adapter<?> adapter = binding.recyclerPanelTaskTable.getAdapter();
        if (adapter instanceof TaskTableAdapter) {
            ((TaskTableAdapter) adapter).clearData();
        }
    }

    /**
     * Round24 REQ-24-02: 收起态清空面板数据绑定（延续 Round23"禁止半常驻残留"）
     */
    private void clearPanelContent() {
        if (binding == null) return;
        panelLoadedForTab = -1;
        binding.textPanelTaskEmpty.setVisibility(View.GONE);
        binding.textPanelTaskError.setVisibility(View.GONE);
        binding.scrollPanelTaskTable.setVisibility(View.GONE);
        RecyclerView.Adapter<?> adapter = binding.recyclerPanelTaskTable.getAdapter();
        if (adapter instanceof TaskTableAdapter) {
            ((TaskTableAdapter) adapter).clearData();
        }
    }

    private void setupFab() {
        // Round23 REQ-23-04: FAB 直进任务创建，默认执行人为自己且不锁定，全程无对话框
        binding.fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TaskCreateActivity.class);
            Long uid = TokenManager.getInstance(requireContext()).getUserId();
            if (uid != null && uid > 0) {
                intent.putExtra("assigneeId", uid);
            }
            startActivity(intent);
        });

        // R36 R1: 布局已改 layout_gravity="bottom|end" 固定锚定（原锚定面板顶边代码偏移废止）

        // R37 REQ-37-C3: 恢复预设缩放按钮接线（resetZoom 此前全工程零调用，本轮闭合）
        binding.btnResetZoom.setOnClickListener(v -> binding.calendarView.resetZoom());

        // R37 REQ-37-D: 入口按钮动态避让底部导航（运行时实测顶边，目标间距 4dp 容差 0~8dp）
        binding.getRoot().post(this::calibrateEntryButtonMargin);
    }

    /**
     * R37 REQ-37-D2: 实测 bottom_navigation 顶边 → 校准 btnPanelEntry marginBottom。
     * 导航栏运行时钳制高度（MainActivity），XML 静态 170dp 硬编码废止；布局变化重算。
     */
    private void calibrateEntryButtonMargin() {
        if (binding == null) return;
        android.view.View nav = requireActivity().findViewById(R.id.bottom_navigation);
        android.view.View root = binding.getRoot();
        if (nav == null || root.getHeight() == 0 || nav.getHeight() == 0) {
            binding.getRoot().post(this::calibrateEntryButtonMargin);
            return;
        }
        int navTopInRoot = nav.getTop() - root.getTop();
        int desiredBottom = root.getBottom() - navTopInRoot; // 根底到导航顶的像素距离
        float density = getResources().getDisplayMetrics().density;
        int marginPx = Math.max(0, desiredBottom - (int) (4f * density)); // 目标 4dp 间距
        ViewGroup.LayoutParams lp = binding.btnPanelEntry.getLayoutParams();
        if (lp instanceof android.view.ViewGroup.MarginLayoutParams
                && ((android.view.ViewGroup.MarginLayoutParams) lp).bottomMargin != marginPx) {
            ((android.view.ViewGroup.MarginLayoutParams) lp).bottomMargin = marginPx;
            binding.btnPanelEntry.setLayoutParams(lp);
        }
        // 导航栏布局变化时重算（覆盖运行时钳制时序）
        nav.addOnLayoutChangeListener((v, left, top, right, bottom, ol, ot, orr, ob) -> calibrateEntryButtonMargin());
    }

    private void updateView(int position) {
        binding.containerMonthView.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        binding.containerWeekView.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        binding.containerDayView.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
        binding.fabAddTask.setVisibility(View.VISIBLE);

        switch (position) {
            case 0:
                // R37 REQ-37-C6: 切 tab 返回月视图时重置缩放（翻月保持由 CalendarView 不重置实现）
                binding.calendarView.resetZoom();
                loadMonthViewData();
                break;
            case 1: loadWeekViewData(); break;
            case 2:
                setupWeekCalendarNav();
                loadDayViewData();
                break;
        }
    }

    private void setupWeekCalendarNav() {
        LinearLayout container = binding.weekCalendarContainer;
        container.removeAllViews();

        Calendar weekStart = (Calendar) selectedDate.clone();
        int diff = (weekStart.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        weekStart.add(Calendar.DAY_OF_MONTH, -diff);

        String[] dayNames = {"一", "二", "三", "四", "五", "六", "日"};
        int todayDay = selectedDate.get(Calendar.DAY_OF_MONTH);
        int todayMonth = selectedDate.get(Calendar.MONTH);
        int todayYear = selectedDate.get(Calendar.YEAR);

        for (int i = 0; i < 7; i++) {
            Calendar dayCal = (Calendar) weekStart.clone();
            dayCal.add(Calendar.DAY_OF_MONTH, i);

            LinearLayout dayColumn = new LinearLayout(requireContext());
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            colParams.setMargins(8, 8, 8, 8);
            dayColumn.setLayoutParams(colParams);
            dayColumn.setOrientation(LinearLayout.VERTICAL);
            dayColumn.setGravity(Gravity.CENTER);

            TextView dayNameView = new TextView(requireContext());
            dayNameView.setText(dayNames[i]);
            dayNameView.setGravity(Gravity.CENTER);
            dayNameView.setTextSize(12);

            TextView dayNumView = new TextView(requireContext());
            dayNumView.setText(String.valueOf(dayCal.get(Calendar.DAY_OF_MONTH)));
            dayNumView.setGravity(Gravity.CENTER);
            dayNumView.setTextSize(16);
            dayNumView.setSingleLine(true);
            dayNumView.setMaxLines(1);
            dayNumView.setPadding(0, 8, 0, 8);

            boolean isToday = dayCal.get(Calendar.DAY_OF_MONTH) == todayDay
                    && dayCal.get(Calendar.MONTH) == todayMonth
                    && dayCal.get(Calendar.YEAR) == todayYear;
            if (isToday) {
                dayNumView.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.example.project_pandora.R.color.color_assignee_self));
                dayNumView.setTypeface(null, android.graphics.Typeface.BOLD);
                dayNameView.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.example.project_pandora.R.color.color_assignee_self));
            } else {
                dayNumView.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.example.project_pandora.R.color.color_text_primary));
                dayNameView.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.example.project_pandora.R.color.color_text_secondary));
            }

            final int year = dayCal.get(Calendar.YEAR);
            final int month = dayCal.get(Calendar.MONTH);
            final int day = dayCal.get(Calendar.DAY_OF_MONTH);
            dayColumn.setOnClickListener(v -> {
                selectedDate.set(year, month, day);
                loadDayViewData();
                setupWeekCalendarNav();
            });

            dayColumn.addView(dayNameView);
            dayColumn.addView(dayNumView);
            container.addView(dayColumn);
        }
    }

    /** R36 REQ-36-A1: 双请求聚合状态（我的任务源+我布置的任务源独立订阅，禁 zip） */
    private int monthRequestId = 0;
    private List<TaskSpan> pendingMineSpans = new ArrayList<>();
    private List<TaskSpan> pendingDispatchedSpans = new ArrayList<>();

    private void loadMonthViewData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String monthStr = String.format(Locale.getDefault(), "%04d-%02d",
                selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH) + 1);
        binding.textMonthLabel.setText(monthLabelFmt.format(selectedDate.getTime()));

        final int reqId = ++monthRequestId;
        pendingMineSpans = new ArrayList<>();
        pendingDispatchedSpans = new ArrayList<>();

        // 源一：我的任务（assignee 过滤，原链路）
        viewRepository.getMonthView(monthStr, true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        data -> {
                            if (binding == null || reqId != monthRequestId) return;
                            List<TaskSpan> mine = tasksToSpans(
                                    (List<Map<String, Object>>) data.get("tasks"), TaskSpan.SOURCE_MINE, 0);
                            pendingMineSpans = mine;
                            recombineMonthSpans();
                        },
                        error -> {
                            if (binding == null || reqId != monthRequestId) return;
                            pendingMineSpans = new ArrayList<>();
                            recombineMonthSpans(); // 源一失败不影响源二渲染
                        }
                );

        // 源二：我布置的任务（dispatcher 过滤+展示层排除自建自派，沿 REQ-27-08 语义）
        Long uid = TokenManager.getInstance(requireContext()).getUserId();
        if (uid != null && uid > 0) {
            taskRepository.listTasksExtended(null, null, uid, true)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            tasks -> {
                                if (binding == null || reqId != monthRequestId) return;
                                List<Map<String, Object>> filtered = new ArrayList<>();
                                if (tasks != null) {
                                    for (Map<String, Object> t : tasks) {
                                        Long assigneeId = com.example.project_pandora.core.util.NumberCompat
                                                .toLong(t.get("assignee_id") != null ? t.get("assignee_id") : t.get("assigneeId"));
                                        if (assigneeId == null || assigneeId.longValue() != uid.longValue()) {
                                            filtered.add(t); // 排除自建自派（已在"我的任务"中显示）
                                        }
                                    }
                                }
                                pendingDispatchedSpans = tasksToSpans(filtered, TaskSpan.SOURCE_DISPATCHED, 100);
                                recombineMonthSpans();
                            },
                            error -> {
                                if (binding == null || reqId != monthRequestId) return;
                                pendingDispatchedSpans = new ArrayList<>();
                                recombineMonthSpans(); // 源二失败不影响源一渲染
                            }
                    );
        } else {
            recombineMonthSpans();
        }
    }

    /** R36: 双源汇合点（每源到达即调用；progressBar 在汇合处按源计数关闭） */
    private int arrivedSources = 0;
    private void recombineMonthSpans() {
        arrivedSources++;
        if (binding == null) return;
        if (arrivedSources >= 2) {
            binding.progressBar.setVisibility(View.GONE);
        }
        List<TaskSpan> merged = new ArrayList<>();
        merged.addAll(pendingMineSpans);
        merged.addAll(pendingDispatchedSpans);
        merged.sort((a, b) -> Long.compare(a.getStartDate().getTimeInMillis(), b.getStartDate().getTimeInMillis()));
        assignLanesAndRender(merged);
    }

    private String parseLocalDate(Object timeObj) {
        // Round25 R25-02: 统一委托 DateCompat（逻辑同原实现，UTC ISO 按本地时区换算）
        return com.example.project_pandora.core.util.DateCompat.toLocalDate(timeObj);
    }

    @SuppressWarnings("unchecked")
    private void displayMonthView(Map<String, Object> data) {
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) data.get("tasks");
        if (tasks == null) tasks = new ArrayList<>();
        allMonthTasks = tasks;
        // R36: 旧单源渲染路径保留兼容（外部若有直调）——委托统一管线
        assignLanesAndRender(tasksToSpans(tasks, TaskSpan.SOURCE_MINE, 0));
    }

    /**
     * R36 REQ-36-A1: 任务列表 → TaskSpan 转换（双源共用；colorSeed 区分两源色带起点）。
     */
    @SuppressWarnings("unchecked")
    private List<TaskSpan> tasksToSpans(List<Map<String, Object>> tasks, int source, int colorSeed) {
        List<TaskSpan> spans = new ArrayList<>();
        if (tasks == null) return spans;
        int colorIndex = colorSeed;
        for (Map<String, Object> task : tasks) {
            Object startTimeObj = task.get("start_time");
            if (startTimeObj == null) startTimeObj = task.get("START_TIME");
            Object endTimeObj = task.get("end_time");
            if (endTimeObj == null) endTimeObj = task.get("END_TIME");
            Object nameObj = task.get("name");
            if (nameObj == null) nameObj = task.get("NAME");
            Object idObj = task.get("id");
            if (idObj == null) idObj = task.get("ID");

            if ((startTimeObj != null || endTimeObj != null) && nameObj != null) {
                try {
                    String startStr = parseLocalDate(startTimeObj != null ? startTimeObj : endTimeObj);
                    String endStr = endTimeObj != null ? parseLocalDate(endTimeObj) : startStr;
                    if (startStr == null) startStr = endStr;
                    if (startStr == null) continue;

                    java.sql.Date startDate = java.sql.Date.valueOf(startStr);
                    java.sql.Date endDate = java.sql.Date.valueOf(endStr);

                    Calendar startCal = Calendar.getInstance();
                    startCal.setTime(startDate);
                    Calendar endCal = Calendar.getInstance();
                    endCal.setTime(endDate);

                    if (endCal.before(startCal)) endCal = (Calendar) startCal.clone();

                    // Round26: 任务 ID 解析统一收敛至 NumberCompat（原吞异常→0 语义保持）
                    long taskId = com.example.project_pandora.core.util.NumberCompat.toLongOrDefault(idObj, 0L);

                    spans.add(new TaskSpan(taskId, nameObj.toString(), startCal, endCal, colorIndex, source));
                    colorIndex++;
                } catch (Exception ignored) {}
            }
        }

        return spans;
    }

    /**
     * R36: 排序+车道分配+渲染（双源汇合统一出口；沿用原 Round25 车道算法）。
     */
    private void assignLanesAndRender(List<TaskSpan> spans) {
        if (binding == null) return;
        spans.sort((a, b) -> Long.compare(a.getStartDate().getTimeInMillis(), b.getStartDate().getTimeInMillis()));

        List<Long> laneEndMillis = new ArrayList<>();
        for (TaskSpan span : spans) {
            long spanStartMillis = span.getStartDate().getTimeInMillis();
            long spanEndMillis = span.getEndDate().getTimeInMillis();

            int assignedLane = -1;
            for (int lane = 0; lane < laneEndMillis.size(); lane++) {
                if (laneEndMillis.get(lane) < spanStartMillis) {
                    assignedLane = lane;
                    break;
                }
            }

            if (assignedLane == -1) {
                assignedLane = laneEndMillis.size();
                laneEndMillis.add(spanEndMillis);
            } else {
                laneEndMillis.set(assignedLane, spanEndMillis);
            }

            span.setLaneIndex(assignedLane);
        }

        int totalLanes = Math.max(laneEndMillis.size(), 1);
        for (TaskSpan span : spans) {
            span.setTotalLanes(totalLanes);
        }

        binding.calendarView.setTaskSpans(spans);
    }

    private void loadWeekViewData() {
        binding.progressBar.setVisibility(View.VISIBLE);

        Calendar weekStart = (Calendar) selectedDate.clone();
        int diff = (weekStart.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        weekStart.add(Calendar.DAY_OF_MONTH, -diff);
        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, 6);

        binding.textWeekLabel.setText(weekLabelFmt.format(weekStart.getTime()) + " - " +
                weekLabelFmt.format(weekEnd.getTime()));

        viewRepository.getWeekView(false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        data -> {
                            if (binding == null) return;
                            binding.progressBar.setVisibility(View.GONE);
                            displayWeekView(data);
                        },
                        error -> {
                            if (binding == null) return;
                            binding.progressBar.setVisibility(View.GONE);
                        }
                );
    }

    @SuppressWarnings("unchecked")
    private void displayWeekView(Map<String, Object> data) {
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) data.get("tasks");
        if (tasks == null) tasks = new ArrayList<>();

        if (tasks.isEmpty()) {
            binding.recyclerWeekTasks.setVisibility(View.GONE);
            binding.weekEmptyState.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerWeekTasks.setVisibility(View.VISIBLE);
            binding.weekEmptyState.setVisibility(View.GONE);
            TaskCardAdapter adapter = new TaskCardAdapter(tasks, this::openTaskDetail);
            binding.recyclerWeekTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.recyclerWeekTasks.setAdapter(adapter);
        }
    }

    private void loadDayViewData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String dateStr = dateFmt.format(selectedDate.getTime());
        binding.textDayLabel.setText(dayLabelFmt.format(selectedDate.getTime()));

        viewRepository.getDayView(dateStr, false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        data -> {
                            if (binding == null) return;
                            binding.progressBar.setVisibility(View.GONE);
                            displayDayView(data);
                        },
                        error -> {
                            if (binding == null) return;
                            binding.progressBar.setVisibility(View.GONE);
                        }
                );
    }

    @SuppressWarnings("unchecked")
    private void displayDayView(Map<String, Object> data) {
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) data.get("tasks");
        if (tasks == null) tasks = new ArrayList<>();

        if (tasks.isEmpty()) {
            binding.recyclerDayTasks.setVisibility(View.GONE);
            binding.dayEmptyState.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerDayTasks.setVisibility(View.VISIBLE);
            binding.dayEmptyState.setVisibility(View.GONE);
            TaskCardAdapter adapter = new TaskCardAdapter(tasks, this::openTaskDetail);
            binding.recyclerDayTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.recyclerDayTasks.setAdapter(adapter);
        }
    }

    /**
     * Round24 REQ-24-02: 周/日视图任务卡点击跳转详情（与六列表格行点击一致，交互统一）
     */
    private void openTaskDetail(Map<String, Object> task) {
        Object idObj = task.get("id");
        if (idObj == null) idObj = task.get("ID");
        // Round26: 行 ID 解析收敛至 NumberCompat（原 instanceof Number 判定，串形态 ID 无法跳转）
        Long taskId = com.example.project_pandora.core.util.NumberCompat.toLong(idObj);
        if (taskId != null) {
            Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
            intent.putExtra("taskId", taskId);
            startActivity(intent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            updateView(currentViewMode);
            panelLoadedForTab = -1;
            loadPanelTasks(currentPanelTab);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * 周/日视图任务卡 Adapter（Round24 REQ-24-02: 宽度模型随捏合缩放废止一并移除，构造简化）
     */
    private static class TaskCardAdapter extends RecyclerView.Adapter<TaskCardAdapter.CardViewHolder> {
        private final List<Map<String, Object>> items;
        private final OnTaskClickListener listener;

        interface OnTaskClickListener {
            void onTaskClick(Map<String, Object> task);
        }

        TaskCardAdapter(List<Map<String, Object>> items, OnTaskClickListener listener) {
            this.items = items != null ? items : new ArrayList<>();
            this.listener = listener;
        }

        void clearData() {
            items.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout root = new FrameLayout(parent.getContext());
            root.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout card = new LinearLayout(parent.getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(parent, 16), dp(parent, 12), dp(parent, 16), dp(parent, 12));
            card.setBackgroundColor(Color.WHITE);
            FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.gravity = Gravity.CENTER_HORIZONTAL;
            cardLp.setMargins(dp(parent, 8), dp(parent, 4), dp(parent, 8), dp(parent, 4));
            card.setLayoutParams(cardLp);

            TextView taskName = new TextView(parent.getContext());
            taskName.setTextSize(15);
            taskName.setTextColor(androidx.core.content.ContextCompat.getColor(taskName.getContext(), com.example.project_pandora.R.color.color_text_primary));
            taskName.setMaxLines(2);
            taskName.setEllipsize(android.text.TextUtils.TruncateAt.END);

            LinearLayout metaRow = new LinearLayout(parent.getContext());
            metaRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            metaLp.topMargin = dp(parent, 6);
            metaRow.setLayoutParams(metaLp);

            TextView timeText = new TextView(parent.getContext());
            timeText.setTextSize(12);
            timeText.setTextColor(androidx.core.content.ContextCompat.getColor(timeText.getContext(), com.example.project_pandora.R.color.color_text_secondary));

            TextView statusText = new TextView(parent.getContext());
            statusText.setTextSize(12);
            statusText.setPadding(dp(parent, 8), 0, 0, 0);

            metaRow.addView(timeText);
            metaRow.addView(statusText);
            card.addView(taskName);
            card.addView(metaRow);
            root.addView(card);

            return new CardViewHolder(root, card, taskName, timeText, statusText);
        }

        @Override
        public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
            Map<String, Object> item = items.get(position);

            Object name = item.get("name");
            if (name == null) name = item.get("NAME");
            if (name == null) name = item.get("work_item");
            holder.taskName.setText(name != null ? name.toString() : "未命名");

            Object startTime = item.get("start_time");
            if (startTime == null) startTime = item.get("START_TIME");
            Object endTime = item.get("end_time");
            if (endTime == null) endTime = item.get("END_TIME");

            String timeText = "";
            // Round25 R25-02: UTC ISO 串按本地时区解析（原 substring 会把 00:00 截成前一天）
            String start = com.example.project_pandora.core.util.DateCompat.toLocalDate(startTime);
            String end = com.example.project_pandora.core.util.DateCompat.toLocalDate(endTime);
            if (start != null) {
                if (end != null) {
                    timeText = start.equals(end) ? start : start + " ~ " + end;
                } else {
                    timeText = start;
                }
            }
            holder.timeText.setText(timeText);

            Object status = item.get("status");
            if (status == null) status = item.get("completion_status");
            Object isUrgent = item.get("is_urgent");

            StringBuilder sb = new StringBuilder();
            if (status != null) {
                String s = status.toString();
                if (s.equals("COMPLETED") || s.equals("CONFIRMED")) {
                    sb.append("已完成");
                    holder.statusText.setTextColor(androidx.core.content.ContextCompat.getColor(holder.statusText.getContext(), com.example.project_pandora.R.color.color_calendar_today));
                } else {
                    sb.append("未完成");
                    holder.statusText.setTextColor(0xFFF57C00);
                }
            }
            if (isUrgent != null && Boolean.parseBoolean(isUrgent.toString())) {
                if (sb.length() > 0) sb.append("  ");
                sb.append("紧急");
                holder.statusText.setTextColor(androidx.core.content.ContextCompat.getColor(holder.statusText.getContext(), com.example.project_pandora.R.color.color_assignee_self));
            }
            holder.statusText.setText(sb.toString());

            holder.cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(item);
                }
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        private static int dp(ViewGroup parent, int value) {
            float density = parent.getContext().getResources().getDisplayMetrics().density;
            return (int) (value * density + 0.5f);
        }

        static class CardViewHolder extends RecyclerView.ViewHolder {
            final LinearLayout cardView;
            final TextView taskName;
            final TextView timeText;
            final TextView statusText;

            CardViewHolder(View itemView, LinearLayout cardView, TextView taskName,
                           TextView timeText, TextView statusText) {
                super(itemView);
                this.cardView = cardView;
                this.taskName = taskName;
                this.timeText = timeText;
                this.statusText = statusText;
            }
        }
    }
}
