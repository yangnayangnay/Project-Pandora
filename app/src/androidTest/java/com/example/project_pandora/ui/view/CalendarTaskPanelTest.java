package com.example.project_pandora.ui.view;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.project_pandora.R;
import com.example.project_pandora.databinding.FragmentViewBinding;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumentation tests for the real month task panel rendering path.
 *
 * <p>The fragment is deliberately unattached and no lifecycle method is called, so no repository,
 * network client, or Activity is created. The real {@code ViewFragment.renderDates()} is invoked
 * reflectively to exercise its selected-date filtering and the real {@link MonthTaskAdapter}.</p>
 */
@RunWith(AndroidJUnit4.class)
public class CalendarTaskPanelTest {

    private static final int YEAR = 2026;
    private static final long OWNER_ID = 7L;
    private static final String OWNER_NAME = "林晓";

    @Test
    public void selectedDateChangeRerendersRowsAndHeading() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Fixture fixture = fixture(baseMine(), baseDispatched(), YEAR, Calendar.OCTOBER, 28);
            fixture.render();

            assertHeading(fixture, "10月28日 · 全部任务 · 2 项");
            assertRenderedTasks(fixture.binding.recyclerPanelTaskTable,
                    Arrays.asList("跨日评审", "整理日报"));
            assertEmptyState(fixture, false);

            fixture.select(YEAR, Calendar.OCTOBER, 29);
            fixture.render();

            assertHeading(fixture, "10月29日 · 全部任务 · 2 项");
            assertRenderedTasks(fixture.binding.recyclerPanelTaskTable,
                    Arrays.asList("跨日评审", "外派盘点"));
            assertEmptyState(fixture, false);

            fixture.select(YEAR, Calendar.OCTOBER, 30);
            fixture.render();

            assertHeading(fixture, "10月30日 · 全部任务 · 0 项");
            assertRenderedTasks(fixture.binding.recyclerPanelTaskTable, Collections.emptyList());
            assertEmptyState(fixture, true);
        });
    }

    @Test
    public void panelTabsPreserveScopeWhileFilteringToSelectedDate() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Fixture fixture = fixture(baseMine(), baseDispatched(), YEAR, Calendar.OCTOBER, 28);

            fixture.tab(0);
            fixture.render();
            assertHeading(fixture, "10月28日 · 全部任务 · 2 项");
            assertRenderedTasks(fixture.binding.recyclerPanelTaskTable,
                    Arrays.asList("跨日评审", "整理日报"));
            assertEmptyState(fixture, false);

            fixture.tab(1);
            fixture.render();
            assertHeading(fixture, "10月28日 · 我的任务 · 2 项");
            assertRenderedTasks(fixture.binding.recyclerPanelTaskTable,
                    Arrays.asList("跨日评审", "整理日报"));
            assertEmptyState(fixture, false);

            fixture.tab(2);
            fixture.render();
            assertHeading(fixture, "10月28日 · 我布置的任务 · 0 项");
            assertRenderedTasks(fixture.binding.recyclerPanelTaskTable, Collections.emptyList());
            assertEmptyState(fixture, true);

            fixture.select(YEAR, Calendar.OCTOBER, 29);

            fixture.tab(0);
            fixture.render();
            assertHeading(fixture, "10月29日 · 全部任务 · 2 项");
            assertRenderedTasks(fixture.binding.recyclerPanelTaskTable,
                    Arrays.asList("跨日评审", "外派盘点"));
            assertEmptyState(fixture, false);

            fixture.tab(1);
            fixture.render();
            assertHeading(fixture, "10月29日 · 我的任务 · 1 项");
            assertRenderedTasks(fixture.binding.recyclerPanelTaskTable,
                    Collections.singletonList("跨日评审"));
            assertEmptyState(fixture, false);

            fixture.tab(2);
            fixture.render();
            assertHeading(fixture, "10月29日 · 我布置的任务 · 1 项");
            assertRenderedTasks(fixture.binding.recyclerPanelTaskTable,
                    Collections.singletonList("外派盘点"));
            assertEmptyState(fixture, false);
        });
    }

    @Test
    public void midnightEndIsExclusiveWhileDateOnlyEndIsInclusive() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Fixture fixture = fixture(boundaryMine(), baseDispatched(), YEAR, Calendar.OCTOBER, 28);
            fixture.render();

            assertHeading(fixture, "10月28日 · 全部任务 · 3 项");
            assertRenderedTasks(fixture.binding.recyclerPanelTaskTable,
                    Arrays.asList("跨日评审", "整理日报", "夜间值守"));

            fixture.select(YEAR, Calendar.OCTOBER, 29);
            fixture.render();

            assertHeading(fixture, "10月29日 · 全部任务 · 2 项");
            List<String> october29Names = renderedTaskNames(fixture.binding.recyclerPanelTaskTable);
            assertEquals(Arrays.asList("跨日评审", "外派盘点"), october29Names);
            assertFalse("22:00-00:00 的任务不应覆盖次日",
                    october29Names.contains("夜间值守"));
            assertTrue("仅日期的结束日期应包含 10 月 29 日",
                    october29Names.contains("跨日评审"));
            assertEmptyState(fixture, false);
        });
    }

    private static Fixture fixture(List<Map<String, Object>> mine,
                                   List<Map<String, Object>> dispatched,
                                   int year, int month, int dayOfMonth) {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context themed = new ContextThemeWrapper(targetContext, R.style.Theme_Project_Pandora);
        ViewFragment fragment = new ViewFragment();
        FragmentViewBinding binding = FragmentViewBinding.inflate(LayoutInflater.from(themed));

        setField(fragment, "binding", binding);
        setField(fragment, "selectedDate", day(year, month, dayOfMonth));
        setField(fragment, "currentPanelTab", 0);
        setField(fragment, "loading", false);
        setField(fragment, "loadFailed", false);

        replace(tasks(fragment, "mine"), mine);
        replace(tasks(fragment, "dispatched"), dispatched);

        return new Fixture(fragment, binding);
    }

    private static void replace(List<Map<String, Object>> target, List<Map<String, Object>> source) {
        target.clear();
        target.addAll(source);
    }

    private static List<Map<String, Object>> baseMine() {
        List<Map<String, Object>> tasks = new ArrayList<>();
        tasks.add(task(101L, "整理日报", "2026-10-28", "2026-10-28", OWNER_ID, OWNER_NAME));
        tasks.add(task(102L, "跨日评审", "2026-10-27", "2026-10-29", OWNER_ID, OWNER_NAME));
        tasks.add(task(103L, "十一月规划", "2026-11-01", "2026-11-01", OWNER_ID, OWNER_NAME));
        return tasks;
    }

    private static List<Map<String, Object>> baseDispatched() {
        List<Map<String, Object>> tasks = new ArrayList<>();
        tasks.add(task(104L, "外派盘点", "2026-10-29", "2026-10-29", 8L, "周宁"));
        return tasks;
    }

    /** The midnight-boundary row is kept out of the base fixture so the specified daily counts stay 2. */
    private static List<Map<String, Object>> boundaryMine() {
        List<Map<String, Object>> tasks = baseMine();
        tasks.add(task(105L, "夜间值守", "2026-10-28 22:00", "2026-10-29 00:00",
                OWNER_ID, OWNER_NAME));
        return tasks;
    }

    private static Map<String, Object> task(long id, String name, String start, String end,
                                            long assigneeId, String assigneeName) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", id);
        task.put("name", name);
        task.put("start_time", start);
        task.put("end_time", end);
        task.put("status", "IN_PROGRESS");
        task.put("assignee_id", assigneeId);
        task.put("assignee_name", assigneeName);
        task.put("assignee_real_name", assigneeName);
        return task;
    }

    private static Calendar day(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, dayOfMonth, 0, 0, 0);
        return calendar;
    }

    private static void setField(ViewFragment fragment, String name, Object value) {
        try {
            field(name).set(fragment, value);
        } catch (Exception e) {
            throw new AssertionError("无法写入 ViewFragment." + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> tasks(ViewFragment fragment, String name) {
        try {
            return (List<Map<String, Object>>) field(name).get(fragment);
        } catch (Exception e) {
            throw new AssertionError("无法读取 ViewFragment." + name, e);
        }
    }

    private static Field field(String name) {
        try {
            Field field = ViewFragment.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            throw new AssertionError("无法反射 ViewFragment." + name, e);
        }
    }

    private static void renderDates(ViewFragment fragment) {
        try {
            Method method = ViewFragment.class.getDeclaredMethod("renderDates");
            method.setAccessible(true);
            method.invoke(fragment);
        } catch (Exception e) {
            throw new AssertionError("无法反射执行 ViewFragment.renderDates()", e);
        }
    }

    private static void assertHeading(Fixture fixture, String expected) {
        assertEquals(expected, fixture.binding.monthTaskHeading.getText().toString());
    }

    private static void assertEmptyState(Fixture fixture, boolean visible) {
        TextView empty = fixture.binding.textPanelTaskEmpty;
        assertEquals("当天暂无任务", empty.getText().toString());
        assertEquals(visible ? View.VISIBLE : View.GONE, empty.getVisibility());
    }

    private static void assertRenderedTasks(RecyclerView recycler, List<String> expected) {
        List<List<String>> rows = renderedRows(recycler);
        assertEquals("任务行数不符", expected.size(), rows.size());
        for (int position = 0; position < expected.size(); position++) {
            List<String> texts = rows.get(position);
            assertEquals("第 " + position + " 行的任务名不符",
                    expected.get(position), texts.get(0));
            assertTrue("第 " + position + " 行递归收集的 TextView 中应包含任务名",
                    texts.contains(expected.get(position)));
        }
    }

    private static List<String> renderedTaskNames(RecyclerView recycler) {
        List<String> names = new ArrayList<>();
        for (List<String> texts : renderedRows(recycler)) {
            names.add(texts.get(0));
        }
        return names;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<List<String>> renderedRows(RecyclerView recycler) {
        RecyclerView.Adapter adapter = recycler.getAdapter();
        assertNotNull("renderTaskTable 应安装真实适配器", adapter);
        assertTrue("renderTaskTable 应安装 MonthTaskAdapter", adapter instanceof MonthTaskAdapter);

        List<List<String>> rows = new ArrayList<>();
        for (int position = 0; position < adapter.getItemCount(); position++) {
            RecyclerView.ViewHolder holder =
                    adapter.createViewHolder(recycler, adapter.getItemViewType(position));
            adapter.bindViewHolder(holder, position);
            List<String> texts = textValues(holder.itemView);
            assertFalse("绑定的任务卡至少应包含名称 TextView", texts.isEmpty());
            rows.add(texts);
        }
        return rows;
    }

    private static List<String> textValues(View view) {
        List<String> values = new ArrayList<>();
        collectTextValues(view, values);
        return values;
    }

    private static void collectTextValues(View view, List<String> values) {
        if (view instanceof TextView) {
            values.add(((TextView) view).getText().toString());
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                collectTextValues(group.getChildAt(index), values);
            }
        }
    }

    private static final class Fixture {
        final ViewFragment fragment;
        final FragmentViewBinding binding;

        Fixture(ViewFragment fragment, FragmentViewBinding binding) {
            this.fragment = fragment;
            this.binding = binding;
        }

        void select(int year, int month, int dayOfMonth) {
            setField(fragment, "selectedDate", day(year, month, dayOfMonth));
        }

        void tab(int index) {
            setField(fragment, "currentPanelTab", index);
        }

        void render() {
            renderDates(fragment);
        }
    }
}
