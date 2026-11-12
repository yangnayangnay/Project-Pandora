package com.example.project_pandora.ui.mindmap;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.OneShotPreDrawListener;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.example.project_pandora.R;
import com.example.project_pandora.core.util.ErrorMessageMapper;
import com.example.project_pandora.core.util.NetworkErrorClassifier;
import com.example.project_pandora.core.util.NumberCompat;
import com.example.project_pandora.databinding.FragmentMindmapBinding;
import com.example.project_pandora.model.Task;
import com.example.project_pandora.notification.NotificationPollWorker;
import com.example.project_pandora.repository.ViewRepository;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.data.remote.UserApi;
import com.example.project_pandora.ui.home.BirthdayConfettiView;
import com.example.project_pandora.ui.home.DailyAgendaController;
import com.example.project_pandora.ui.home.DailyItems;
import com.example.project_pandora.ui.home.HomeBoardLoader;
import com.example.project_pandora.ui.home.HomeBoardsActivity;
import com.example.project_pandora.ui.task.TaskDetailActivity;
import com.google.android.material.appbar.AppBarLayout;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.SerialDisposable;

/** Home keeps company events and assigned tasks above the independent personal agenda. */
public class MindMapFragment extends Fragment {
    private FragmentMindmapBinding binding;
    private ViewRepository repository;
    private DailyAgendaController agenda;
    private HomeBoardLoader boardLoader;
    private Bundle viewState;
    private boolean boardSizePending;
    private boolean returningFromBoards;
    private int pendingEventsScroll = -1, pendingTasksScroll = -1;
    private final SerialDisposable birthdayRequest = new SerialDisposable();
    private String birthdayCheckDate = "";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMindmapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle state = viewState != null ? viewState : savedInstanceState;
        returningFromBoards = false;
        pendingEventsScroll = state == null ? 0 : state.getInt("homeEventsScroll");
        pendingTasksScroll = state == null ? 0 : state.getInt("homeTasksScroll");
        boardSizePending = true;
        repository = new ViewRepository(requireContext());
        boardLoader = new HomeBoardLoader(repository::getHome, AndroidSchedulers.mainThread(), new HomeBoardLoader.Listener() {
            @Override public void onLoading(boolean events) {
                if (events) message(binding.listCompanyImportant, "正在加载事件纪…", false);
                message(binding.listCompanyDispatch, "正在加载本人任务…", false);
            }
            @Override public void onEvents(List<Map<String, Object>> events) { renderEvents(events); resizeBoards(); }
            @Override public void onTasks(List<Map<String, Object>> tasks) {
                renderTasks(tasks);
                // Keep both card bounds and the event scroll position stable during task filtering.
                if (boardSizePending) { resizeBoards(); boardSizePending = false; }
            }
            @Override public void onError(Throwable error, boolean events) {
                String text = errorMessage(error) + "\n点击重试";
                if (events) message(binding.listCompanyImportant, text, true);
                message(binding.listCompanyDispatch, text, true);
                if (boardSizePending) resizeBoards();
            }
        });
        agenda = new DailyAgendaController(requireContext(), binding, state);
        // Each board scrolls independently; only the agenda folds away the boards.
        binding.scrollCompanyImportant.setNestedScrollingEnabled(false);
        binding.scrollCompanyDispatch.setNestedScrollingEnabled(false);
        AppBarLayout.Behavior boardBehavior = new AppBarLayout.Behavior();
        boardBehavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
            @Override public boolean canDrag(@NonNull AppBarLayout appBarLayout) { return false; }
        });
        ((CoordinatorLayout.LayoutParams) binding.homeAppBar.getLayoutParams()).setBehavior(boardBehavior);
        binding.btnExpandEvents.setOnClickListener(v -> expandBoard(HomeBoardsActivity.TAB_EVENTS));
        binding.btnExpandTasks.setOnClickListener(v -> expandBoard(HomeBoardsActivity.TAB_TASKS));
        resizeBoards();
    }

    private void loadHome() {
        if (boardLoader != null) boardLoader.refresh();
    }

    private void checkBirthdayGreeting() {
        java.text.SimpleDateFormat day = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT);
        day.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
        String today = day.format(new java.util.Date());
        if (today.equals(birthdayCheckDate)) return;
        birthdayCheckDate = today;
        Long userId = TokenManager.getInstance(requireContext()).getUserId();
        if (userId == null || userId <= 0) return;
        android.content.SharedPreferences preferences = requireContext().getSharedPreferences("birthday_greeting", android.content.Context.MODE_PRIVATE);
        String key = "hidden_" + userId;
        if (today.equals(preferences.getString(key, ""))) return;
        UserApi api = NetworkClient.getInstance(requireContext()).createService(UserApi.class);
        birthdayRequest.set(api.getUserSettings().subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
            if (binding == null || !response.isSuccess() || response.getData() == null) return;
            String birthday = java.util.Objects.toString(response.getData().get("birthday"), "");
            if (birthday.length() < 10 || !birthday.substring(5, 10).equals(today.substring(5, 10))) return;
            showBirthdayGreeting(preferences, key, today);
        }, error -> birthdayCheckDate = ""));
    }

    private void showBirthdayGreeting(android.content.SharedPreferences preferences, String key, String today) {
        android.widget.LinearLayout content = new android.widget.LinearLayout(requireContext());
        content.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = dp(20); content.setPadding(padding, 0, padding, padding);
        BirthdayConfettiView confetti = new BirthdayConfettiView(requireContext());
        content.addView(confetti, new android.widget.LinearLayout.LayoutParams(-1, dp(180)));
        android.widget.TextView greeting = itemText("生日快乐！\n愿新的一岁平安顺遂，心愿皆成。\n今天也要好好庆祝自己。");
        greeting.setTextSize(17); greeting.setGravity(android.view.Gravity.CENTER); greeting.setLineSpacing(dp(4), 1f);
        content.addView(greeting, new android.widget.LinearLayout.LayoutParams(-1, -2));
        com.google.android.material.checkbox.MaterialCheckBox hideToday = new com.google.android.material.checkbox.MaterialCheckBox(requireContext());
        hideToday.setText("今天不再显示"); hideToday.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_primary));
        hideToday.setPadding(0, dp(12), 0, 0); content.addView(hideToday);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("生日祝福").setView(content).setPositiveButton("谢谢", null).create();
        dialog.setOnDismissListener(ignored -> {
            if (hideToday.isChecked()) preferences.edit().putString(key, today).apply();
        });
        dialog.show();
    }

    private void expandBoard(int tab) {
        returningFromBoards = true;
        startActivity(HomeBoardsActivity.createIntent(requireContext(), tab));
    }

    private void resizeBoards() {
        FragmentMindmapBinding current = binding;
        if (current == null) return;
        current.boardPanels.post(() -> {
            if (binding != current || current.quadrantTopLeft.getWidth() == 0) return;
            int contentHeight = Math.max(boardContentHeight(current.quadrantTopLeft, current.listCompanyImportant),
                    boardContentHeight(current.quadrantTopRight, current.listCompanyDispatch));
            int maximum = Math.max(dp(160), Math.min(dp(280), Math.round(current.homeCoordinator.getHeight() * .36f)));
            int height = Math.min(maximum, Math.max(dp(160), contentHeight
                    + current.boardPanels.getPaddingTop() + current.boardPanels.getPaddingBottom()));
            ViewGroup.LayoutParams params = current.boardPanels.getLayoutParams();
            if (params.height != height) {
                params.height = height; current.boardPanels.setLayoutParams(params);
                if (agenda != null) agenda.onBoardsResized();
            }
        });
    }

    private int boardContentHeight(LinearLayout panel, LinearLayout list) {
        int width = View.MeasureSpec.makeMeasureSpec(panel.getWidth(), View.MeasureSpec.EXACTLY);
        int wrap = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        View header = panel.getChildAt(0);
        header.measure(width, wrap);
        list.measure(width, wrap);
        return header.getMeasuredHeight() + list.getMeasuredHeight();
    }

    private void renderEvents(List<Map<String, Object>> items) {
        int scroll = pendingEventsScroll >= 0 ? pendingEventsScroll : binding.scrollCompanyImportant.getScrollY();
        pendingEventsScroll = -1;
        LinearLayout container = binding.listCompanyImportant;
        container.removeAllViews();
        if (items.isEmpty()) message(container, "暂无重要事项", false);
        for (int i = 0; i < items.size(); i++) {
            String content = Objects.toString(DailyItems.value(items.get(i), "content"), "").trim();
            if (content.isEmpty() || content.matches("[0-9]+")) content = "未命名";
            TextView row = itemText((i + 1) + ". " + content);
            row.setMaxLines(2); row.setEllipsize(android.text.TextUtils.TruncateAt.END);
            addRow(container, row);
        }
        restoreScroll(binding.scrollCompanyImportant, scroll);
    }

    private void renderTasks(List<Map<String, Object>> items) {
        int scroll = pendingTasksScroll >= 0 ? pendingTasksScroll : binding.scrollCompanyDispatch.getScrollY();
        pendingTasksScroll = -1;
        LinearLayout container = binding.listCompanyDispatch;
        container.removeAllViews();
        if (items.isEmpty()) message(container, "暂无分派给我的未完成任务", false);
        for (Map<String, Object> item : items) {
            Task display = new Task();
            display.setStatus(Objects.toString(DailyItems.value(item, "status"), ""));
            String name = Objects.toString(DailyItems.value(item, "name"), "未命名任务");
            String dispatcher = Objects.toString(DailyItems.value(item, "dispatcher_name"), "未设置");
            String date = Objects.toString(DailyItems.value(item, "end_time"), "");
            if (date.length() >= 10) date = date.substring(0, 10);
            TextView row = itemText(name + "\n" + display.getStatusDisplayName() + " · " + dispatcher
                    + (date.isEmpty() ? "" : "\n截止 " + date));
            row.setMinHeight(dp(64));
            row.setBackgroundResource(R.drawable.bg_home_task_row);
            row.setContentDescription(name + "，" + display.getStatusDisplayName() + "，派发人 " + dispatcher + "，点击查看任务详情");
            row.setOnClickListener(v -> {
                Long id = NumberCompat.toLong(DailyItems.value(item, "id"));
                if (id != null) startActivity(new Intent(requireContext(), TaskDetailActivity.class).putExtra("taskId", id.longValue()));
            });
            addRow(container, row);
        }
        restoreScroll(binding.scrollCompanyDispatch, scroll);
    }

    private void restoreScroll(NestedScrollView scroll, int position) {
        FragmentMindmapBinding current = binding;
        OneShotPreDrawListener.add(scroll, () -> {
            if (binding == current) scroll.scrollTo(0, position);
        });
    }

    private void saveBoardState(Bundle state) {
        if (binding != null) {
            state.putInt("homeEventsScroll", pendingEventsScroll >= 0 ? pendingEventsScroll : binding.scrollCompanyImportant.getScrollY());
            state.putInt("homeTasksScroll", pendingTasksScroll >= 0 ? pendingTasksScroll : binding.scrollCompanyDispatch.getScrollY());
        }
    }

    private void addRow(LinearLayout container, TextView row) {
        boolean events = container == binding.listCompanyImportant;
        row.setTextColor(ContextCompat.getColor(requireContext(), events ? R.color.home_event_text : R.color.home_task_text));
        if (container.getChildCount() > 0) {
            View divider = new View(requireContext());
            divider.setBackgroundColor(ContextCompat.getColor(requireContext(), events ? R.color.home_event_divider : R.color.home_task_divider));
            container.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        }
        container.addView(row);
    }

    private TextView itemText(String text) {
        TextView row = new TextView(requireContext());
        row.setText(text); row.setTextSize(13);
        row.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_primary));
        row.setPadding(dp(4), dp(6), dp(4), dp(6));
        return row;
    }

    private void message(LinearLayout container, String text, boolean retry) {
        container.removeAllViews();
        TextView row = itemText(text);
        row.setTextColor(ContextCompat.getColor(requireContext(), container == binding.listCompanyImportant
                ? R.color.home_event_secondary : R.color.home_task_secondary));
        row.setGravity(android.view.Gravity.CENTER);
        row.setMinHeight(dp(48));
        if (retry) row.setOnClickListener(v -> boardLoader.load(container == binding.listCompanyImportant));
        container.addView(row);
    }

    private static String errorMessage(Throwable error) {
        if (error instanceof com.example.project_pandora.core.exception.BusinessException) {
            com.example.project_pandora.core.exception.BusinessException business = (com.example.project_pandora.core.exception.BusinessException) error;
            return ErrorMessageMapper.map(business.getCode(), business.getMessage());
        }
        return NetworkErrorClassifier.classify(error);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override public void onResume() {
        super.onResume(); loadHome();
        checkBirthdayGreeting();
        // The boards cannot edit the personal agenda; keep its content and scroll intact on return.
        if (agenda != null && !returningFromBoards) agenda.refresh();
        returningFromBoards = false;
        NotificationPollWorker.enqueueOneTimeWork(requireContext());
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (agenda != null) agenda.saveState(outState);
        else if (viewState != null) outState.putAll(viewState);
        saveBoardState(outState);
    }

    @Override public void onDestroyView() {
        viewState = new Bundle(); saveBoardState(viewState);
        if (agenda != null) { agenda.saveState(viewState); agenda.close(); agenda = null; }
        if (boardLoader != null) { boardLoader.close(); boardLoader = null; }
        birthdayRequest.set(null);
        binding = null;
        super.onDestroyView();
    }
}
