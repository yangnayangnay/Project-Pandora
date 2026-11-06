package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.icu.util.ChineseCalendar;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** An anchored day agenda with a quiet surface and vertically paired start/end times. */
public final class DayTasksPopup extends PopupWindow {
    private final Context context;
    private final LinearLayout content;
    private final RecyclerView list;
    private final int taskCount;

    public DayTasksPopup(Context context, Calendar date, List<Map<String, Object>> tasks,
                         Consumer<Map<String, Object>> onTaskClick) {
        super(context);
        this.context = context;
        taskCount = tasks.size();
        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(12));
        GradientDrawable background = new GradientDrawable();
        background.setColor(color(R.color.color_surface));
        background.setCornerRadius(dp(24));
        content.setBackground(background);
        content.setClipToOutline(true);
        String title = new SimpleDateFormat("M月d日EEEE", Locale.CHINA).format(date.getTime());
        content.addView(text(title, 20, R.color.color_text_primary));
        TextView lunar = text(lunarDate(date), 14, R.color.color_text_secondary);
        LinearLayout.LayoutParams lunarParams = new LinearLayout.LayoutParams(-1, -2);
        lunarParams.setMargins(0, dp(6), 0, dp(16));
        content.addView(lunar, lunarParams);
        list = new RecyclerView(context);
        list.setLayoutManager(new LinearLayoutManager(context));
        list.setContentDescription(title + "，" + tasks.size() + "项任务");
        list.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
                LinearLayout row = new LinearLayout(context);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setMinimumHeight(dp(88));
                row.setPadding(dp(4), dp(16), dp(4), dp(16));
                android.util.TypedValue ripple = new android.util.TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
                row.setBackgroundResource(ripple.resourceId);
                View marker = new View(context);
                GradientDrawable markerBackground = new GradientDrawable();
                markerBackground.setColor(color(R.color.color_day_task_marker));
                markerBackground.setCornerRadius(dp(2));
                marker.setBackground(markerBackground);
                row.addView(marker, new LinearLayout.LayoutParams(dp(3), dp(18)));
                TextView name = text("", 17, R.color.color_text_primary);
                LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1);
                nameParams.setMargins(dp(12), 0, dp(12), 0);
                row.addView(name, nameParams);
                TextView time = text("", 13, R.color.color_text_secondary);
                time.setGravity(Gravity.END);
                time.setLineSpacing(dp(7), 1);
                row.addView(time, new LinearLayout.LayoutParams(-2, -2));
                row.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
                return new RecyclerView.ViewHolder(row) {};
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                Map<String, Object> task = tasks.get(position);
                LinearLayout row = (LinearLayout) holder.itemView;
                String name = java.util.Objects.toString(DayTaskSelection.value(task, "name"), "未命名任务");
                String time = taskTimes(task, date);
                String tags = com.example.project_pandora.core.util.TaskTags.names(task);
                if (!tags.isEmpty()) name += "\n" + tags;
                ((GradientDrawable) row.getChildAt(0).getBackground()).setColor(com.example.project_pandora.ui.task.TaskTagColors.task(context, task));
                ((TextView) row.getChildAt(1)).setText(name);
                ((TextView) row.getChildAt(2)).setText(time);
                row.setContentDescription(name + "，" + time.replace('\n', '至'));
                row.setOnClickListener(v -> { dismiss(); onTaskClick.accept(task); });
            }
            @Override public int getItemCount() { return tasks.size(); }
        });
        content.addView(list, new LinearLayout.LayoutParams(-1, -2));
        setContentView(content);
        setBackgroundDrawable(background);
        setElevation(dp(12));
        setFocusable(true);
        setOutsideTouchable(true);
        setInputMethodMode(INPUT_METHOD_NOT_NEEDED);
    }

    public void show(View anchor, Rect taskBounds) {
        Rect frame = new Rect();
        anchor.getWindowVisibleDisplayFrame(frame);
        int margin = dp(16);
        int availableWidth = Math.max(1, frame.width() - margin * 2);
        int width = Math.min(availableWidth, Math.max(dp(248), Math.min(dp(304), Math.round(frame.width() * .68f))));
        int availableHeight = Math.max(1, frame.height() - margin * 2);
        content.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(availableHeight, View.MeasureSpec.AT_MOST));
        int headerHeight = content.getMeasuredHeight() - list.getMeasuredHeight();
        list.getLayoutParams().height = Math.max(dp(48), Math.min(taskCount * dp(88), availableHeight - headerHeight));
        content.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(availableHeight, View.MeasureSpec.AT_MOST));
        int height = Math.min(availableHeight, content.getMeasuredHeight());
        int x = taskBounds == null ? frame.right - width - margin : taskBounds.right - dp(8);
        int y = taskBounds == null ? frame.centerY() - height / 2 : taskBounds.top - height / 2;
        x = Math.max(frame.left + margin, Math.min(x, frame.right - width - margin));
        y = Math.max(frame.top + margin, Math.min(y, frame.bottom - height - margin));
        int[] screen = new int[2], window = new int[2];
        anchor.getLocationOnScreen(screen);
        anchor.getLocationInWindow(window);
        setWidth(width); setHeight(height);
        showAtLocation(anchor, Gravity.TOP | Gravity.LEFT, x - screen[0] + window[0], y - screen[1] + window[1]);
    }

    private static String taskTimes(Map<String, Object> task, Calendar date) {
        String start = DayTaskSelection.start(task), end = DayTaskSelection.end(task);
        if (start == null) start = end;
        if (start == null) return "";
        if (end == null || end.compareTo(start) < 0) end = start;
        if (start.length() == 10 && end.length() == 10) return "全天";
        String day = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(date.getTime());
        return timeLabel(start, day) + "\n" + timeLabel(end, day);
    }
    private static String timeLabel(String time, String day) {
        if (time.length() < 16) return time.length() >= 10 ? time.substring(5, 10).replace('-', '/') : time;
        return time.startsWith(day) ? time.substring(11, 16) : time.substring(5, 16).replace('-', '/');
    }
    private static String lunarDate(Calendar day) {
        ChineseCalendar chinese = new ChineseCalendar();
        chinese.setTimeInMillis(day.getTimeInMillis());
        String month = new String[]{"正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月"}[chinese.get(android.icu.util.Calendar.MONTH) % 12];
        int value = chinese.get(android.icu.util.Calendar.DAY_OF_MONTH);
        String number = value == 10 ? "初十" : value == 20 ? "二十" : value == 30 ? "三十"
                : (value < 10 ? "初" : value < 20 ? "十" : "廿") + new String[]{"", "一", "二", "三", "四", "五", "六", "七", "八", "九"}[value % 10];
        return "农历" + (chinese.get(ChineseCalendar.IS_LEAP_MONTH) == 1 ? "闰" : "") + month + number;
    }
    private TextView text(String value, int size, int color) {
        TextView text = new TextView(context); text.setText(value); text.setTextSize(size); text.setTextColor(color(color)); return text;
    }
    private int color(int resource) { return ContextCompat.getColor(context, resource); }
    private int dp(float value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
}
