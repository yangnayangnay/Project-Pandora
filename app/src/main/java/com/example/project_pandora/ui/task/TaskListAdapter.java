package com.example.project_pandora.ui.task;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.R;
import com.example.project_pandora.databinding.ItemTaskBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(Long taskId);
    }

    private final List<Map<String, Object>> tasks = new ArrayList<>();
    private final OnTaskClickListener listener;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public TaskListAdapter(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<Map<String, Object>> newTasks) {
        tasks.clear();
        if (newTasks != null) {
            tasks.addAll(newTasks);
        }
        sortByUrgency();
        notifyDataSetChanged();
    }

    private void sortByUrgency() {
        Collections.sort(tasks, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> a, Map<String, Object> b) {
                int ua = getUrgencyLevel(a);
                int ub = getUrgencyLevel(b);
                if (ua != ub) return ua - ub;

                Date da = getDeadline(a);
                Date db = getDeadline(b);
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return da.compareTo(db);
            }
        });
    }

    private int getUrgencyLevel(Map<String, Object> task) {
        Object overrideObj = task.get("urgency_override");
        if (overrideObj instanceof Number) {
            return ((Number) overrideObj).intValue();
        }
        return calculateDeadlineUrgency(task);
    }

    static int calculateDeadlineUrgency(Map<String, Object> task) {
        Date deadline = getDeadlineFromTask(task);
        if (deadline == null) return 4;

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar dl = Calendar.getInstance();
        dl.setTime(deadline);
        dl.set(Calendar.HOUR_OF_DAY, 0);
        dl.set(Calendar.MINUTE, 0);
        dl.set(Calendar.SECOND, 0);
        dl.set(Calendar.MILLISECOND, 0);

        long diffMs = dl.getTimeInMillis() - today.getTimeInMillis();
        long diffDays = diffMs / (1000 * 60 * 60 * 24);

        if (diffDays <= 0) return 1;
        if (diffDays < 3) return 2;
        if (diffDays <= 10) return 3;
        return 4;
    }

    private Date getDeadline(Map<String, Object> task) {
        return getDeadlineFromTask(task);
    }

    static Date getDeadlineFromTask(Map<String, Object> task) {
        Object endTime = task.get("end_time");
        if (endTime == null) return null;
        String dateStr = endTime.toString();
        if (dateStr.length() >= 10) dateStr = dateStr.substring(0, 10);
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTaskBinding binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Map<String, Object> task = tasks.get(position);
        holder.bind(task, position + 1, listener);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final ItemTaskBinding binding;

        TaskViewHolder(ItemTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Map<String, Object> task, int seqNumber, OnTaskClickListener listener) {
            String name = (String) task.get("name");
            binding.textTaskName.setText(name != null ? name : "");

            binding.textSeqNumber.setText(String.format(Locale.getDefault(), "%d", seqNumber));

            String status = (String) task.get("status");
            binding.textStatus.setText(getStatusDisplayName(status));

            String priority = (String) task.get("priority");
            binding.textPriority.setText(getPriorityDisplayName(priority));

            Object endTime = task.get("end_time");
            if (endTime != null) {
                String dateStr = endTime.toString();
                if (dateStr.length() >= 10) dateStr = dateStr.substring(0, 10);
                Date deadline = getDeadlineFromTask(task);
                int daysLeft = -1;
                if (deadline != null) {
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);
                    Calendar dl = Calendar.getInstance();
                    dl.setTime(deadline);
                    dl.set(Calendar.HOUR_OF_DAY, 0);
                    dl.set(Calendar.MINUTE, 0);
                    dl.set(Calendar.SECOND, 0);
                    dl.set(Calendar.MILLISECOND, 0);
                    long diff = (dl.getTimeInMillis() - today.getTimeInMillis()) / (1000 * 60 * 60 * 24);
                    daysLeft = (int) diff;
                }

                String deadlineLabel;
                if (daysLeft == 0) deadlineLabel = "今天交付";
                else if (daysLeft == 1) deadlineLabel = "明天交付";
                else if (daysLeft > 1) deadlineLabel = daysLeft + "天后交付";
                else deadlineLabel = "已逾期" + (-daysLeft) + "天";
                binding.textDeadline.setText("截止:" + dateStr + " (" + deadlineLabel + ")");
            } else {
                binding.textDeadline.setText("未设置截止日期");
            }

            int urgency = calculateDeadlineUrgency(task);
            int colorRes;
            switch (urgency) {
                case 1: colorRes = R.drawable.bg_urgent_critical; break;
                case 2: colorRes = R.drawable.bg_urgent_high; break;
                case 3: colorRes = R.drawable.bg_urgent_medium; break;
                default: colorRes = R.drawable.bg_urgent_low; break;
            }
            binding.indicatorUrgency.setBackgroundResource(colorRes);
            binding.textSeqNumber.setBackgroundResource(colorRes);

            Object idObj = task.get("id");
            if (idObj != null) {
                Long taskId = ((Number) idObj).longValue();
                itemView.setOnClickListener(v -> listener.onTaskClick(taskId));
            }
        }

        private String getStatusDisplayName(String status) {
            if (status == null) return "待接收";
            switch (status) {
                case "PENDING": return "待接收";
                case "IN_PROGRESS": return "进行中";
                case "COMPLETED": return "已完成";
                case "CONFIRMED": return "已确认";
                default: return status;
            }
        }

        private String getPriorityDisplayName(String priority) {
            if (priority == null) return "中";
            switch (priority) {
                case "LOW": return "低";
                case "MEDIUM": return "中";
                case "HIGH": return "高";
                case "URGENT": return "紧急";
                default: return priority;
            }
        }
    }
}
