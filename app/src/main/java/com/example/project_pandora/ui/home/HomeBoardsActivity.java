package com.example.project_pandora.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import com.example.project_pandora.ui.PandoraActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project_pandora.R;
import com.example.project_pandora.core.exception.BusinessException;
import com.example.project_pandora.core.util.ErrorMessageMapper;
import com.example.project_pandora.core.util.NetworkErrorClassifier;
import com.example.project_pandora.databinding.ActivityHomeBoardsBinding;
import com.example.project_pandora.repository.ViewRepository;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/** A separate screen leaves the original home, selected date and board positions on the back stack. */
public final class HomeBoardsActivity extends PandoraActivity {
    public static final int TAB_EVENTS = 0, TAB_TASKS = 1;
    public static final String EXTRA_TAB = "homeBoardTab";
    private ActivityHomeBoardsBinding binding;
    private HomeBoardLoader loader;
    private final HomeBoardAdapter events = new HomeBoardAdapter(true), tasks = new HomeBoardAdapter(false);
    private LinearLayoutManager eventLayout, taskLayout;
    private Parcelable eventScroll, taskScroll;
    private int tab;
    private boolean eventsLoaded, tasksLoaded, eventsLoading, tasksLoading, resetTasksScroll;
    private String eventsError, tasksError;

    public static Intent createIntent(Context context, int tab) {
        return new Intent(context, HomeBoardsActivity.class).putExtra(EXTRA_TAB, tab == TAB_TASKS ? TAB_TASKS : TAB_EVENTS);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        tab = state == null ? getIntent().getIntExtra(EXTRA_TAB, TAB_EVENTS) : state.getInt(EXTRA_TAB);
        tab = tab == TAB_TASKS ? TAB_TASKS : TAB_EVENTS;
        if (state != null) { eventScroll = state.getParcelable("eventsScroll"); taskScroll = state.getParcelable("tasksScroll"); }
        binding = ActivityHomeBoardsBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        com.example.project_pandora.ui.widget.CompanyBackgrounds.bind(binding.getRoot(),
                com.example.project_pandora.ui.widget.CompanyBackgrounds.Scene.HOME);
        binding.toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        binding.boardTabs.setTabRippleColor(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(this, R.color.home_task_press)));
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        ViewCompat.requestApplyInsets(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        eventLayout = new LinearLayoutManager(this); taskLayout = new LinearLayoutManager(this);
        binding.eventsList.setLayoutManager(eventLayout); binding.eventsList.setAdapter(events);
        binding.tasksList.setLayoutManager(taskLayout); binding.tasksList.setAdapter(tasks);
        binding.eventsList.setItemAnimator(null); binding.tasksList.setItemAnimator(null);
        ViewRepository repository = new ViewRepository(this);
        loader = new HomeBoardLoader(repository::getHome, AndroidSchedulers.mainThread(), new HomeBoardLoader.Listener() {
            @Override public void onLoading(boolean includeEvents) {
                if (includeEvents) { eventsLoading = true; eventsError = null; }
                tasksLoading = true; tasksLoaded = false; tasksError = null; updateDisplay();
            }
            @Override public void onEvents(List<Map<String, Object>> rows) {
                Parcelable scroll = eventScroll != null ? eventScroll : eventLayout.onSaveInstanceState();
                events.submit(rows); eventsLoaded = true; eventsLoading = false; eventsError = null;
                eventLayout.onRestoreInstanceState(scroll); eventScroll = null; updateDisplay();
            }
            @Override public void onTasks(List<Map<String, Object>> rows) {
                Parcelable scroll = taskScroll != null ? taskScroll : taskLayout.onSaveInstanceState();
                tasks.submit(rows); tasksLoaded = true; tasksLoading = false; tasksError = null;
                if (resetTasksScroll) taskLayout.scrollToPositionWithOffset(0, 0);
                else taskLayout.onRestoreInstanceState(scroll);
                resetTasksScroll = false; taskScroll = null; updateDisplay();
            }
            @Override public void onError(Throwable error, boolean includeEvents) {
                if (includeEvents) { eventsLoading = false; eventsError = errorMessage(error); }
                tasksLoading = false; tasksError = errorMessage(error); updateDisplay();
            }
        });
        binding.boardTabs.addTab(binding.boardTabs.newTab().setText("事件纪"), false);
        binding.boardTabs.addTab(binding.boardTabs.newTab().setText("任务看板"), false);
        binding.boardTabs.selectTab(binding.boardTabs.getTabAt(tab));
        binding.boardTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab selected) { tab = selected.getPosition(); updateDisplay(); }
            @Override public void onTabUnselected(TabLayout.Tab selected) {}
            @Override public void onTabReselected(TabLayout.Tab selected) {}
        });
        binding.retryBoard.setOnClickListener(view -> loader.load(tab == TAB_EVENTS));
        updateDisplay();
    }

    private void updateDisplay() {
        boolean eventTab = tab == TAB_EVENTS;
        boolean loading = eventTab ? eventsLoading : tasksLoading;
        boolean loaded = eventTab ? eventsLoaded : tasksLoaded;
        String error = eventTab ? eventsError : tasksError;
        int count = eventTab ? events.getItemCount() : tasks.getItemCount();
        boolean content = loaded && count > 0;
        binding.eventsList.setVisibility(eventTab && content ? View.VISIBLE : View.GONE);
        binding.tasksList.setVisibility(!eventTab && content ? View.VISIBLE : View.GONE);
        binding.boardProgress.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
        binding.boardCount.setText(eventTab ? (loaded ? "公司重要事项 · 共 " + count + " 项" : "公司重要事项")
                : (loaded ? "分派给我的任务 · 共 " + count + " 项" : "分派给我的任务"));
        binding.boardFeedback.setVisibility(content ? View.GONE : View.VISIBLE);
        binding.boardMessage.setText(loading ? "正在加载…" : error != null ? error : eventTab ? "暂无重要事项"
                : "暂无分派给我的未完成任务");
        binding.retryBoard.setVisibility(!loading && error != null ? View.VISIBLE : View.GONE);
    }

    private static String errorMessage(Throwable error) {
        if (error instanceof BusinessException) {
            BusinessException business = (BusinessException) error;
            return ErrorMessageMapper.map(business.getCode(), business.getMessage());
        }
        return NetworkErrorClassifier.classify(error);
    }

    @Override protected void onResume() { super.onResume(); loader.refresh(); }

    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state); state.putInt(EXTRA_TAB, tab);
        state.putParcelable("eventsScroll", eventScroll != null ? eventScroll : eventLayout.onSaveInstanceState());
        if (!resetTasksScroll) state.putParcelable("tasksScroll", taskScroll != null ? taskScroll : taskLayout.onSaveInstanceState());
    }

    @Override protected void onDestroy() { if (loader != null) loader.close(); super.onDestroy(); }
}
