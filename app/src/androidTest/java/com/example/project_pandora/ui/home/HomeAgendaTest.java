package com.example.project_pandora.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.project_pandora.R;
import com.example.project_pandora.databinding.FragmentMindmapBinding;
import com.google.android.material.appbar.AppBarLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** Device-only view contracts. Source compilation alone is not a device test pass. */
@RunWith(AndroidJUnit4.class)
public class HomeAgendaTest {
    private static Context context() {
        return new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_Project_Pandora);
    }

    @Test public void homePreservesLeftRightBoardsAndPlacesAgendaBelowThem() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            FragmentMindmapBinding binding = FragmentMindmapBinding.inflate(LayoutInflater.from(context()));
            assertSame(binding.quadrantTopLeft, binding.boardPanels.getChildAt(0));
            assertSame(binding.quadrantTopRight, binding.boardPanels.getChildAt(1));
            assertSame(binding.homeAppBar, binding.homeCoordinator.getChildAt(0));
            assertSame(binding.scrollDailyAgenda, binding.homeCoordinator.getChildAt(1));
            assertEquals(2, binding.boardPanels.getChildCount());
            assertTrue((((AppBarLayout.LayoutParams) binding.boardPanels.getLayoutParams()).getScrollFlags() & AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL) != 0);
            assertEquals("首页", context().getString(R.string.nav_mindmap));
            assertEquals("公司", context().getString(R.string.nav_view));
        });
    }

    @Test public void boardTitlesShareFontAndBaselineWithAccessibleExpandTargetsOnSmallScreens() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = context(); float density = context.getResources().getDisplayMetrics().density;
            for (int width : new int[]{320, 390}) {
                FragmentMindmapBinding binding = FragmentMindmapBinding.inflate(LayoutInflater.from(context));
                ViewGroup root = (ViewGroup) binding.getRoot();
                root.measure(View.MeasureSpec.makeMeasureSpec(Math.round(width * density), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(Math.round(800 * density), View.MeasureSpec.EXACTLY));
                root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
                Rect eventBounds = new Rect(), taskBounds = new Rect();
                binding.textEventsTitle.getDrawingRect(eventBounds); root.offsetDescendantRectToMyCoords(binding.textEventsTitle, eventBounds);
                binding.textTasksTitle.getDrawingRect(taskBounds); root.offsetDescendantRectToMyCoords(binding.textTasksTitle, taskBounds);
                assertEquals(binding.textEventsTitle.getTextSize(), binding.textTasksTitle.getTextSize(), 0f);
                assertEquals(eventBounds.top + binding.textEventsTitle.getBaseline(), taskBounds.top + binding.textTasksTitle.getBaseline());
                assertEquals(Math.round(48 * density), binding.btnExpandEvents.getMeasuredWidth());
                assertEquals(Math.round(48 * density), binding.btnExpandTasks.getMeasuredHeight());
                assertNotNull(binding.btnExpandEvents.getContentDescription()); assertNotNull(binding.btnExpandTasks.getContentDescription());
            }
        });
    }

    @Test public void expansionIntentsKeepTheExistingBackStackAndPassTheSelectedBoardAndFilter() {
        for (int tab : new int[]{HomeBoardsActivity.TAB_EVENTS, HomeBoardsActivity.TAB_TASKS}) {
            for (boolean done : new boolean[]{false, true}) {
                Intent intent = HomeBoardsActivity.createIntent(context(), tab, done);
                assertEquals(HomeBoardsActivity.class.getName(), intent.getComponent().getClassName());
                assertEquals(tab, intent.getIntExtra(HomeBoardsActivity.EXTRA_TAB, -1));
                assertEquals(done, intent.getBooleanExtra(HomeBoardsActivity.EXTRA_COMPLETED, !done));
                assertEquals(0, intent.getFlags());
            }
        }
    }

    @Test public void expandedCardsShowCompleteContentAndOfferTaskDetailNavigation() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            String longText = String.join("", Collections.nCopies(200, "完整内容，不应截断。"));
            HomeBoardAdapter eventAdapter = new HomeBoardAdapter(true);
            eventAdapter.submit(Collections.singletonList(Collections.singletonMap("content", longText)));
            HomeBoardAdapter.Holder event = eventAdapter.onCreateViewHolder(new FrameLayout(context()), 0);
            eventAdapter.onBindViewHolder(event, 0);
            assertEquals("1. " + longText, event.title.getText().toString()); assertNull(event.title.getEllipsize());
            assertEquals(Integer.MAX_VALUE, event.title.getMaxLines()); assertFalse(event.itemView.isClickable());
            HomeBoardAdapter taskAdapter = new HomeBoardAdapter(false);
            Map<String, Object> task = new HashMap<>(); task.put("id", 1L); task.put("name", "完整任务"); task.put("content", longText);
            task.put("status", "IN_PROGRESS"); task.put("dispatcher_name", "同事甲"); task.put("assignee_name", "同事乙");
            taskAdapter.submit(Collections.singletonList(task));
            HomeBoardAdapter.Holder holder = taskAdapter.onCreateViewHolder(new FrameLayout(context()), 0);
            taskAdapter.onBindViewHolder(holder, 0);
            assertEquals(longText, holder.body.getText().toString()); assertNull(holder.body.getEllipsize());
            assertEquals("进行中", holder.status.getText().toString()); assertTrue(holder.itemView.hasOnClickListeners());
            assertTrue(holder.details.getText().toString().contains("同事甲")); assertTrue(holder.details.getText().toString().contains("同事乙"));
        });
    }

    @Test public void fullCalendarExpandsAndKeepsSelectedDateAcrossModeChanges() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = context(); DailyItemsMonthView calendar = new DailyItemsMonthView(context);
            Calendar date = Calendar.getInstance(); date.clear(); date.set(2026, Calendar.NOVEMBER, 3);
            calendar.setDate(date);
            int width = Math.round(390 * context.getResources().getDisplayMetrics().density);
            int exact = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            int wrap = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            calendar.measure(exact, wrap); int compact = calendar.getMeasuredHeight();
            calendar.layout(0, 0, width, compact); int selectedTop = calendar.selectedDateTop();
            calendar.setDetailed(true); calendar.measure(exact, wrap);
            assertTrue(calendar.getMeasuredHeight() > compact * 2);
            calendar.setDetailed(false); calendar.measure(exact, wrap); calendar.layout(0, 0, width, compact);
            assertEquals(compact, calendar.getMeasuredHeight()); assertEquals(selectedTop, calendar.selectedDateTop());
        });
    }

    @Test public void checklistRestoresServerConfirmedValueAfterRejectedCompletionAndDisablesBusyRows() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Set<Long> busy = new HashSet<>(); AtomicInteger changes = new AtomicInteger(); AtomicInteger edits = new AtomicInteger();
            DailyItemAdapter adapter = new DailyItemAdapter(busy, (item, completed) -> changes.incrementAndGet(), item -> edits.incrementAndGet());
            Map<String, Object> todo = new LinkedHashMap<>(); todo.put("id", 1.0); todo.put("content", "购物"); todo.put("completed", false);
            adapter.submit(Collections.singletonList(todo));
            DailyItemAdapter.Holder holder = adapter.onCreateViewHolder(new FrameLayout(context()), 0);
            adapter.onBindViewHolder(holder, 0); holder.check.performClick(); assertEquals(1, changes.get());
            adapter.onBindViewHolder(holder, 0); assertFalse(holder.check.isChecked()); assertEquals(1, changes.get());
            holder.edit.performClick(); assertEquals(1, edits.get());
            busy.add(1L); adapter.onBindViewHolder(holder, 0); assertFalse(holder.check.isEnabled()); assertFalse(holder.edit.isEnabled());
            busy.clear(); todo.put("completed", true); adapter.onBindViewHolder(holder, 0);
            assertEquals("已完成", holder.status.getText().toString()); assertTrue(holder.check.isChecked());
            assertTrue((holder.content.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) != 0);
            todo.put("completed", false); adapter.onBindViewHolder(holder, 0);
            assertEquals(0, holder.content.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG);
        });
    }
}
