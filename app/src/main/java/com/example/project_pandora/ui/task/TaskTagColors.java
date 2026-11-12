package com.example.project_pandora.ui.task;

import android.content.Context;
import android.graphics.Color;
import com.example.project_pandora.R;
import com.example.project_pandora.core.util.TaskTags;
import java.util.Map;

public final class TaskTagColors {
    public static final String[] NAMES = {"浅黄", "珊瑚", "淡紫", "雾蓝", "浅绿", "浅杏", "青绿", "粉红"};
    private TaskTagColors() {}
    public static int color(Context context, int index) {
        String[] colors = context.getResources().getStringArray(R.array.calendar_month_detail_colors);
        return Color.parseColor(colors[Math.max(0, Math.min(index, colors.length - 1))]);
    }
    public static int task(Context context, Map<String, Object> task) {
        if (TaskTags.of(task).isEmpty()) return androidx.core.content.ContextCompat.getColor(context, R.color.color_calendar_neutral_entry);
        return color(context, TaskTags.colorIndex(task, NAMES.length));
    }
    public static int text(Context context, int background) {
        return text(context, background, androidx.core.content.ContextCompat.getColor(context, R.color.color_text_primary));
    }
    public static int text(Context context, int background, int preferred) {
        return com.example.project_pandora.core.util.ReadableTextColors.foreground(background,
                androidx.core.content.ContextCompat.getColor(context, R.color.color_surface), preferred);
    }
}
