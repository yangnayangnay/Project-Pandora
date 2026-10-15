package com.example.project_pandora.ui.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.R;
import com.example.project_pandora.databinding.FragmentViewBinding;
import com.example.project_pandora.repository.ViewRepository;
import com.example.project_pandora.ui.task.TaskCreateActivity;
import com.example.project_pandora.ui.widget.TaskSpan;
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

    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat monthLabelFmt = new SimpleDateFormat("yyyy年MM月", Locale.getDefault());
    private final SimpleDateFormat weekLabelFmt = new SimpleDateFormat("MM月dd日", Locale.getDefault());
    private final SimpleDateFormat dayLabelFmt = new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA);
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private int currentViewMode = 0;
    private Calendar selectedDate = Calendar.getInstance();
    private List<Map<String, Object>> allMonthTasks = new ArrayList<>();

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

        setupTabs();
        setupCalendarView();
        setupMonthNavigation();
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
        binding.calendarView.setOnDateSelectedListener((year, month, day) -> {
            selectedDate.set(year, month - 1, day);
            showMonthDayTasks();
        });
    }

    private void setupMonthNavigation() {
        binding.btnPreviousMonth.setOnClickListener(v -> {
            binding.calendarView.previousMonth();
            loadMonthViewData();
        });
        binding.btnNextMonth.setOnClickListener(v -> {
            binding.calendarView.nextMonth();
            loadMonthViewData();
        });
    }

    private void setupFab() {
        binding.fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TaskCreateActivity.class);
            startActivity(intent);
        });
    }

    private void updateView(int position) {
        binding.containerMonthView.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        binding.containerWeekView.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        binding.containerDayView.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
        binding.fabAddTask.setVisibility(position == 2 ? View.VISIBLE : View.GONE);

        switch (position) {
            case 0: loadMonthViewData(); break;
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
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

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
                dayNumView.setTextColor(Color.parseColor("#E53935"));
                dayNumView.setTypeface(null, android.graphics.Typeface.BOLD);
                dayNameView.setTextColor(Color.parseColor("#E53935"));
            } else {
                dayNumView.setTextColor(Color.parseColor("#4E342E"));
                dayNameView.setTextColor(Color.parseColor("#8D6E63"));
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

    private void loadMonthViewData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String monthStr = String.format(Locale.getDefault(), "%04d-%02d",
                selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH) + 1);
        binding.textMonthLabel.setText(monthLabelFmt.format(selectedDate.getTime()));

        viewRepository.getMonthView(monthStr, true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        data -> {
                            binding.progressBar.setVisibility(View.GONE);
                            displayMonthView(data);
                        },
                        error -> {
                            binding.progressBar.setVisibility(View.GONE);
                        }
                );
    }

    @SuppressWarnings("unchecked")
    private void displayMonthView(Map<String, Object> data) {
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) data.get("tasks");
        if (tasks == null) tasks = new ArrayList<>();
        allMonthTasks = tasks;

        List<TaskSpan> spans = new ArrayList<>();
        int colorIndex = 0;
        for (Map<String, Object> task : tasks) {
            Object startTimeObj = task.get("start_time");
            if (startTimeObj == null) startTimeObj = task.get("START_TIME");
            Object endTimeObj = task.get("end_time");
            if (endTimeObj == null) endTimeObj = task.get("END_TIME");
            Object nameObj = task.get("name");
            if (nameObj == null) nameObj = task.get("NAME");
            Object idObj = task.get("id");
            if (idObj == null) idObj = task.get("ID");

            if (startTimeObj != null && nameObj != null) {
                try {
                    String startStr = startTimeObj.toString().substring(0, 10);
                    String endStr = endTimeObj != null ? endTimeObj.toString().substring(0, 10) : startStr;

                    java.sql.Date startDate = java.sql.Date.valueOf(startStr);
                    java.sql.Date endDate = java.sql.Date.valueOf(endStr);

                    Calendar startCal = Calendar.getInstance();
                    startCal.setTime(startDate);
                    Calendar endCal = Calendar.getInstance();
                    endCal.setTime(endDate);

                    if (endCal.before(startCal)) endCal = (Calendar) startCal.clone();

                    long taskId = 0;
                    if (idObj != null) {
                        try { taskId = Long.parseLong(idObj.toString()); } catch (NumberFormatException ignored) {}
                    }

                    spans.add(new TaskSpan(taskId, nameObj.toString(), startCal, endCal, colorIndex));
                    colorIndex++;
                } catch (Exception ignored) {}
            }
        }

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
        showMonthDayTasks();
    }

    @SuppressWarnings("unchecked")
    private void showMonthDayTasks() {
        String selectedDateStr = dateFmt.format(selectedDate.getTime());
        List<Map<String, Object>> dayTasks = new ArrayList<>();

        for (Map<String, Object> task : allMonthTasks) {
            Object startTimeObj = task.get("start_time");
            if (startTimeObj == null) startTimeObj = task.get("START_TIME");
            Object endTimeObj = task.get("end_time");
            if (endTimeObj == null) endTimeObj = task.get("END_TIME");

            if (startTimeObj != null) {
                try {
                    String startStr = startTimeObj.toString().substring(0, 10);
                    String endStr = endTimeObj != null ? endTimeObj.toString().substring(0, 10) : startStr;
                    if (selectedDateStr.compareTo(startStr) >= 0 && selectedDateStr.compareTo(endStr) <= 0) {
                        dayTasks.add(task);
                    }
                } catch (Exception ignored) {}
            }
        }

        if (dayTasks.isEmpty()) {
            binding.textSelectedDateLabel.setVisibility(View.GONE);
            binding.recyclerMonthDayTasks.setVisibility(View.GONE);
        } else {
            SimpleDateFormat labelFmt = new SimpleDateFormat("MM月dd日 EEEE", Locale.CHINA);
            binding.textSelectedDateLabel.setText(labelFmt.format(selectedDate.getTime()) + " （" + dayTasks.size() + "项任务）");
            binding.textSelectedDateLabel.setVisibility(View.VISIBLE);
            binding.recyclerMonthDayTasks.setVisibility(View.VISIBLE);
            TaskCardAdapter adapter = new TaskCardAdapter(dayTasks);
            binding.recyclerMonthDayTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.recyclerMonthDayTasks.setAdapter(adapter);
        }
    }

    private void loadWeekViewData() {
        binding.progressBar.setVisibility(View.VISIBLE);

        Calendar weekStart = (Calendar) selectedDate.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_WEEK, 6);

        binding.textWeekLabel.setText(weekLabelFmt.format(weekStart.getTime()) + " - " +
                weekLabelFmt.format(weekEnd.getTime()));

        viewRepository.getWeekView(false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        data -> {
                            binding.progressBar.setVisibility(View.GONE);
                            displayWeekView(data);
                        },
                        error -> {
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
            TaskCardAdapter adapter = new TaskCardAdapter(tasks);
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
                            binding.progressBar.setVisibility(View.GONE);
                            displayDayView(data);
                        },
                        error -> {
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
            TaskCardAdapter adapter = new TaskCardAdapter(tasks);
            binding.recyclerDayTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.recyclerDayTasks.setAdapter(adapter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class TaskCardAdapter extends RecyclerView.Adapter<TaskCardAdapter.CardViewHolder> {
        private final List<Map<String, Object>> items;

        TaskCardAdapter(List<Map<String, Object>> items) {
            this.items = items != null ? items : new ArrayList<>();
        }

        @NonNull
        @Override
        public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(parent.getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(parent, 16), dp(parent, 12), dp(parent, 16), dp(parent, 12));
            card.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(parent, 8), dp(parent, 4), dp(parent, 8), dp(parent, 4));
            card.setLayoutParams(lp);

            TextView taskName = new TextView(parent.getContext());
            taskName.setTextSize(15);
            taskName.setTextColor(Color.parseColor("#263238"));
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
            timeText.setTextColor(Color.parseColor("#8D6E63"));

            TextView statusText = new TextView(parent.getContext());
            statusText.setTextSize(12);
            statusText.setPadding(dp(parent, 8), 0, 0, 0);

            metaRow.addView(timeText);
            metaRow.addView(statusText);
            card.addView(taskName);
            card.addView(metaRow);

            return new CardViewHolder(card, taskName, timeText, statusText);
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
            if (startTime != null && startTime.toString().length() >= 10) {
                String start = startTime.toString().substring(0, 10);
                if (endTime != null && endTime.toString().length() >= 10) {
                    String end = endTime.toString().substring(0, 10);
                    if (start.equals(end)) {
                        timeText = start;
                    } else {
                        timeText = start + " ~ " + end;
                    }
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
                    holder.statusText.setTextColor(Color.parseColor("#1976D2"));
                } else {
                    sb.append("未完成");
                    holder.statusText.setTextColor(Color.parseColor("#F57C00"));
                }
            }
            if (isUrgent != null && Boolean.parseBoolean(isUrgent.toString())) {
                if (sb.length() > 0) sb.append("  ");
                sb.append("紧急");
                holder.statusText.setTextColor(Color.parseColor("#D32F2F"));
            }
            holder.statusText.setText(sb.toString());

            holder.itemView.setOnClickListener(v -> {
                Object taskId = item.get("id");
                if (taskId == null) taskId = item.get("ID");
                if (taskId instanceof Number) {
                    Intent intent = new Intent(v.getContext(), com.example.project_pandora.ui.task.TaskDetailActivity.class);
                    intent.putExtra("taskId", ((Number) taskId).longValue());
                    v.getContext().startActivity(intent);
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
            final TextView taskName;
            final TextView timeText;
            final TextView statusText;

            CardViewHolder(View itemView, TextView taskName, TextView timeText, TextView statusText) {
                super(itemView);
                this.taskName = taskName;
                this.timeText = timeText;
                this.statusText = statusText;
            }
        }
    }
}
