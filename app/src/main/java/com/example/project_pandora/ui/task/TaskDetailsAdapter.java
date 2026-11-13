package com.example.project_pandora.ui.task;

import android.content.Intent;
import android.content.Context;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.project_pandora.R;
import com.example.project_pandora.core.util.*;
import java.util.*;

/** A shared column definition keeps the scrolling header and all task rows aligned. */
final class TaskDetailsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    static final String[] LABELS = {"任务名称", "截止日期", "任务标签", "紧急程度", "完成情况", "负责人"};
    private static int[] widths(Context context) {
        int available = context.getResources().getConfiguration().screenWidthDp;
        float fontScale = Math.max(1f, context.getResources().getConfiguration().fontScale);
        int[] values = available >= 600 ? new int[]{136, 128, 150, 92, 92, 136}
                : new int[]{174, 132, 210, 100, 100, 160};
        int sum = 0;
        for (int i = 0; i < values.length; i++) { values[i] = Math.round(values[i] * fontScale); sum += values[i]; }
        if (available > sum) {
            int extra = available - sum; values[0] += extra / 3; values[2] += extra / 3; values[5] += extra - 2 * (extra / 3);
        }
        return values;
    }
    private final List<Map<String, Object>> rows = new ArrayList<>();
    private Set<String> pinned = java.util.Collections.emptySet();
    private java.util.function.Consumer<Map<String, Object>> pinClick;
    void setPins(Set<String> ids, java.util.function.Consumer<Map<String, Object>> listener) { pinned = ids; pinClick = listener; }
    void submit(List<Map<String, Object>> data) { rows.clear(); rows.addAll(data); notifyDataSetChanged(); }
    static int totalWidth(Context context) { int width = 0; for (int value : widths(context)) width += dp(context, value); return width; }
    static LinearLayout row(Context context, boolean header) {
        int[] WIDTHS = widths(context);
        LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(ContextCompat.getColor(context, header ? R.color.color_surface_soft : R.color.color_surface));
        for (int i = 0; i < LABELS.length; i++) {
            if (!header && i == 2) {
                TagFlowLayout tags = new TagFlowLayout(context);
                tags.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8)); tags.setMinimumHeight(dp(context, 56));
                row.addView(tags, new LinearLayout.LayoutParams(dp(context, WIDTHS[i]), -2));
            } else if (!header && (i == 3 || i == 4)) {
                FrameLayout badgeCell = new FrameLayout(context); badgeCell.setMinimumHeight(dp(context, 56)); badgeCell.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));
                TextView badge = new TextView(context); badge.setTextSize(13); badge.setGravity(Gravity.CENTER); badge.setPadding(dp(context, 10), dp(context, 5), dp(context, 10), dp(context, 5));
                badgeCell.addView(badge, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
                row.addView(badgeCell, new LinearLayout.LayoutParams(dp(context, WIDTHS[i]), -2));
            } else {
                TextView cell = new TextView(context); cell.setTextSize(header ? 13 : 14); cell.setTextColor(ContextCompat.getColor(context, R.color.color_text_primary));
                cell.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12)); cell.setGravity(Gravity.CENTER_VERTICAL); cell.setMinHeight(dp(context, 56));
                cell.setMaxLines(2); cell.setEllipsize(android.text.TextUtils.TruncateAt.END);
                if (header) { cell.setTypeface(null, Typeface.BOLD); cell.setText(LABELS[i]); }
                row.addView(cell, new LinearLayout.LayoutParams(dp(context, WIDTHS[i]), -2));
            }
        }
        return row;
    }
    @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        LinearLayout row = row(parent.getContext(), false); row.setLayoutParams(new RecyclerView.LayoutParams(totalWidth(parent.getContext()), -2));
        android.util.TypedValue ripple = new android.util.TypedValue(); parent.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        row.setForeground(androidx.appcompat.content.res.AppCompatResources.getDrawable(parent.getContext(), ripple.resourceId));
        return new RecyclerView.ViewHolder(row) {};
    }
    @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Map<String, Object> task = rows.get(position); LinearLayout row = (LinearLayout) holder.itemView;
        String status = plain(task, "status");
        String[] values = {plain(task, "name"), time(task, "end_time"), "", NumberCompat.toBool(TaskTags.value(task, "is_urgent")) ? "紧急" : "不紧急",
                "COMPLETED".equals(status) ? "已完成" : "进行中", TaskDetailFilter.assigneeNames(task)};
        for (int i = 0; i < values.length; i++) if (i != 2) textCell(row, i).setText(values[i].isEmpty() ? "—" : values[i]);
        boolean isPinned = TaskPinOrder.isPinned(task, pinned);
        if (isPinned) textCell(row, 0).setText("置顶 · " + values[0]);
        textCell(row, 0).setTypeface(null, isPinned ? Typeface.BOLD : Typeface.NORMAL);
        bindTags((ViewGroup) row.getChildAt(2), task);
        bindBadge(textCell(row, 3), true);
        bindBadge(textCell(row, 4), false);
        row.setContentDescription(values[0] + "，" + values[4] + "，负责人 " + values[5] + "，点击查看完整详情，长按" + (isPinned ? "取消置顶" : "置顶"));
        row.setOnLongClickListener(v -> {
            androidx.appcompat.widget.PopupMenu menu = new androidx.appcompat.widget.PopupMenu(v.getContext(), v);
            menu.getMenu().add(isPinned ? "取消置顶" : "置顶任务");
            menu.setOnMenuItemClickListener(item -> { if (pinClick != null) pinClick.accept(task); return true; });
            menu.show(); return true;
        });
        row.setOnClickListener(v -> {
            Long id = NumberCompat.toLong(TaskTags.value(task, "id")); if (id == null) return;
            v.getContext().startActivity(new Intent(v.getContext(), TaskDetailActivity.class).putExtra("taskId", id.longValue()));
        });
    }
    @Override public int getItemCount() { return rows.size(); }
    private static TextView textCell(LinearLayout row, int index) {
        View cell = row.getChildAt(index);
        return cell instanceof TextView ? (TextView) cell : (TextView) ((ViewGroup) cell).getChildAt(0);
    }
    private static void bindBadge(TextView badge, boolean urgency) {
        Context context = badge.getContext();
        badge.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        badge.setBackgroundResource(urgency ? R.drawable.bg_quadrant_tag : R.drawable.bg_status_tag);
    }
    private static void bindTags(ViewGroup container, Map<String, Object> task) {
        container.removeAllViews();
        List<Map<String, Object>> tags = TaskTags.of(task);
        if (tags.isEmpty()) { TextView empty = new TextView(container.getContext()); empty.setText("—"); container.addView(empty); return; }
        for (Map<String, Object> tag : tags) {
            TextView chip = new TextView(container.getContext()); chip.setText(Objects.toString(TaskTags.value(tag, "name"), "")); chip.setTextSize(12);
            chip.setPadding(dp(container.getContext(), 8), dp(container.getContext(), 4), dp(container.getContext(), 8), dp(container.getContext(), 4));
            android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable(); background.setCornerRadius(dp(container.getContext(), 10));
            Long index = NumberCompat.toLong(TaskTags.value(tag, "color_index"));
            int fill = TaskTagColors.color(container.getContext(), index == null ? 0 : index.intValue());
            background.setColor(fill); chip.setTextColor(TaskTagColors.text(container.getContext(), fill));
            background.setStroke(dp(container.getContext(), 1), ContextCompat.getColor(container.getContext(), R.color.color_divider)); chip.setBackground(background);
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(-2, -2);
            params.setMarginEnd(dp(container.getContext(), 6)); params.bottomMargin = dp(container.getContext(), 6);
            container.addView(chip, params);
        }
    }

    /** Wraps chips by available width and always starts a new line after three tags. */
    private static final class TagFlowLayout extends ViewGroup {
        TagFlowLayout(Context context) { super(context); }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            int available = Math.max(0, width - getPaddingLeft() - getPaddingRight());
            int lineWidth = 0, lineHeight = 0, totalHeight = 0, inLine = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i); measureChildWithMargins(child, widthSpec, 0, heightSpec, 0);
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                int childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                int childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
                if (inLine == 3 || inLine > 0 && lineWidth + childWidth > available) {
                    totalHeight += lineHeight; lineWidth = 0; lineHeight = 0; inLine = 0;
                }
                lineWidth += childWidth; lineHeight = Math.max(lineHeight, childHeight); inLine++;
            }
            totalHeight += lineHeight + getPaddingTop() + getPaddingBottom();
            setMeasuredDimension(resolveSize(width, widthSpec), resolveSize(Math.max(getSuggestedMinimumHeight(), totalHeight), heightSpec));
        }
        @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int available = right - left - getPaddingLeft() - getPaddingRight();
            int x = getPaddingLeft(), y = getPaddingTop(), lineHeight = 0, inLine = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i); MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                int childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                int childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
                if (inLine == 3 || inLine > 0 && x - getPaddingLeft() + childWidth > available) {
                    x = getPaddingLeft(); y += lineHeight; lineHeight = 0; inLine = 0;
                }
                int childLeft = x + lp.leftMargin, childTop = y + lp.topMargin;
                child.layout(childLeft, childTop, childLeft + child.getMeasuredWidth(), childTop + child.getMeasuredHeight());
                x += childWidth; lineHeight = Math.max(lineHeight, childHeight); inLine++;
            }
        }
        @Override protected LayoutParams generateDefaultLayoutParams() { return new MarginLayoutParams(-2, -2); }
        @Override protected LayoutParams generateLayoutParams(LayoutParams source) { return new MarginLayoutParams(source); }
        @Override public LayoutParams generateLayoutParams(android.util.AttributeSet attrs) { return new MarginLayoutParams(getContext(), attrs); }
        @Override protected boolean checkLayoutParams(LayoutParams params) { return params instanceof MarginLayoutParams; }
    }
    private static String plain(Map<String, Object> task, String key) { return Objects.toString(TaskTags.value(task, key), ""); }
    private static String time(Map<String, Object> task, String key) { return Objects.toString(DateCompat.toLocalDateTime(TaskTags.value(task, key)), "未设置"); }
    private static int dp(Context context, int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
}
