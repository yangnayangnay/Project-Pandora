package com.example.project_pandora.ui.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.R;

import com.example.project_pandora.databinding.FragmentViewBinding;
import com.example.project_pandora.repository.ViewRepository;
import com.example.project_pandora.ui.widget.CalendarView;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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

    private int currentViewMode = 0;
    private Calendar selectedDate = Calendar.getInstance();

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
        setupLogNavigation();

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
            binding.tabLayout.getTabAt(2).select();
            currentViewMode = 2;
            updateView(2);
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

    private void setupLogNavigation() {
        binding.btnViewLogs.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.logFragment));
    }

    private void updateView(int position) {
        binding.containerMonthView.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        binding.containerWeekView.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        binding.containerDayView.setVisibility(position == 2 ? View.VISIBLE : View.GONE);

        switch (position) {
            case 0: loadMonthViewData(); break;
            case 1: loadWeekViewData(); break;
            case 2: loadDayViewData(); break;
        }
    }

    private void loadMonthViewData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String monthStr = String.format(Locale.getDefault(), "%04d-%02d",
                selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH) + 1);
        binding.textMonthLabel.setText(monthLabelFmt.format(selectedDate.getTime()));

        viewRepository.getMonthView(monthStr)
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

        Map<Integer, List<String>> dayEvents = new HashMap<>();
        for (Map<String, Object> task : tasks) {
            Object startTimeObj = task.get("start_time");
            if (startTimeObj != null) {
                try {
                    String dateStr = startTimeObj.toString();
                    if (dateStr.length() >= 10) {
                        int day = Integer.parseInt(dateStr.substring(8, 10));
                        dayEvents.computeIfAbsent(day, k -> new ArrayList<>())
                                .add((String) task.get("name"));
                    }
                } catch (Exception ignored) {}
            }
        }
        binding.calendarView.setDayEvents(dayEvents);
    }

    private void loadWeekViewData() {
        binding.progressBar.setVisibility(View.VISIBLE);

        Calendar weekStart = (Calendar) selectedDate.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_WEEK, 6);

        binding.textWeekLabel.setText(weekLabelFmt.format(weekStart.getTime()) + " - " +
                weekLabelFmt.format(weekEnd.getTime()));

        viewRepository.getWeekView()
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
        setupSimpleList(binding.recyclerWeekTasks, tasks);
    }

    private void loadDayViewData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String dateStr = dateFmt.format(selectedDate.getTime());
        binding.textDayLabel.setText(dayLabelFmt.format(selectedDate.getTime()));

        viewRepository.getDayView(dateStr)
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
        setupSimpleList(binding.recyclerDayTasks, tasks);

        List<Map<String, Object>> logs = (List<Map<String, Object>>) data.get("logs");
        if (logs == null) logs = new ArrayList<>();
        setupSimpleList(binding.recyclerDayLogs, logs);
    }

    private void setupSimpleList(RecyclerView recyclerView, List<Map<String, Object>> items) {
        SimpleItemAdapter adapter = new SimpleItemAdapter(items);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class SimpleItemAdapter extends RecyclerView.Adapter<SimpleItemAdapter.ItemViewHolder> {
        private final List<Map<String, Object>> items;

        SimpleItemAdapter(List<Map<String, Object>> items) {
            this.items = items != null ? items : new ArrayList<>();
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setPadding(32, 24, 32, 24);
            textView.setTextSize(15);
            return new ItemViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            Map<String, Object> item = items.get(position);
            Object name = item.get("name");
            if (name == null) name = item.get("work_item");
            Object status = item.get("status");
            if (status == null) status = item.get("completion_status");

            String text = (name != null ? name.toString() : "未命名");
            if (status != null) text += " [" + status.toString() + "]";
            ((TextView) holder.itemView).setText(text);
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ItemViewHolder extends RecyclerView.ViewHolder {
            ItemViewHolder(View itemView) { super(itemView); }
        }
    }
}
