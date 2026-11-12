package com.example.project_pandora.ui.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.example.project_pandora.ui.PandoraActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.project_pandora.R;
import com.example.project_pandora.core.exception.BusinessException;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.CompanyDirectoryApi;
import com.example.project_pandora.databinding.ActivityCompanyPersonLogsBinding;
import com.example.project_pandora.ui.people.PeopleDirectory;
import com.example.project_pandora.ui.widget.QuickDatePickerDialog;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.SerialDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/** Read-only monthly log calendar for a person visible to the current user. */
public final class CompanyPersonLogsActivity extends PandoraActivity {
    private ActivityCompanyPersonLogsBinding binding;
    private CompanyDirectoryApi api;
    private final SerialDisposable request = new SerialDisposable();
    private final List<Map<String, Object>> logs = new ArrayList<>();
    private final Calendar selected = Calendar.getInstance();
    private final SimpleDateFormat wireDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    private long userId;
    private AlertDialog datePicker;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivityCompanyPersonLogsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        userId = getIntent().getLongExtra("userId", -1L);
        if (userId <= 0) { finish(); return; }
        String name = getIntent().getStringExtra("personName");
        binding.toolbar.setTitle(name == null || name.isBlank() ? "人员日志" : name + "的日志");
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { finish(); }
        });
        api = NetworkClient.getInstance(this).createService(CompanyDirectoryApi.class);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.calendar.setOnDateSelectedListener((year, month, day) -> {
            selected.set(year, month - 1, day); renderDate();
        });
        binding.textMonth.setOnClickListener(v -> chooseDate());
        binding.buttonPreviousMonth.setOnClickListener(v -> shift(-1));
        binding.buttonNextMonth.setOnClickListener(v -> shift(1));
        binding.buttonToday.setOnClickListener(v -> { selected.setTimeInMillis(System.currentTimeMillis()); load(); });
        renderDate();
        load();
    }

    private void shift(int amount) {
        selected.set(Calendar.DAY_OF_MONTH, 1);
        selected.add(Calendar.MONTH, amount);
        load();
    }

    private void chooseDate() {
        if (datePicker != null && datePicker.isShowing()) return;
        datePicker = QuickDatePickerDialog.create(this, selected, 1900, date -> {
            selected.setTime(date.getTime());
            load();
        });
        datePicker.show();
    }

    private void load() {
        Calendar first = (Calendar) selected.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        Calendar last = (Calendar) first.clone();
        last.set(Calendar.DAY_OF_MONTH, last.getActualMaximum(Calendar.DAY_OF_MONTH));
        String expectedMonth = new SimpleDateFormat("yyyy-MM", Locale.ROOT).format(first.getTime());
        binding.progress.setVisibility(View.VISIBLE);
        binding.message.setVisibility(View.VISIBLE);
        binding.message.setText("正在加载日志…");
        renderDate();
        request.set(api.detail(userId, null, wireDate.format(first.getTime()), wireDate.format(last.getTime()))
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
                    if (!response.isSuccess() || response.getData() == null)
                        throw new BusinessException(response.getCode(), response.getMessage());
                    if (!expectedMonth.equals(new SimpleDateFormat("yyyy-MM", Locale.ROOT).format(selected.getTime()))) return;
                    logs.clear();
                    Object raw = response.getData().get("logs");
                    if (raw instanceof List<?>) for (Object item : (List<?>) raw)
                        if (item instanceof Map<?, ?>) logs.add(cast(item));
                    binding.progress.setVisibility(View.GONE);
                    binding.calendar.setItems(calendarEntries(logs));
                    renderDate();
                }, error -> {
                    binding.progress.setVisibility(View.GONE);
                    binding.message.setVisibility(View.VISIBLE);
                    binding.message.setText("日志加载失败，点击重试");
                    binding.message.setOnClickListener(v -> load());
                }));
    }

    private void renderDate() {
        binding.textMonth.setText(new SimpleDateFormat("yyyy年M月", Locale.CHINA).format(selected.getTime()));
        binding.calendar.setDate(selected);
        String day = wireDate.format(selected.getTime());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> log : logs) if (day.equals(text(log, "log_date"))) rows.add(log);
        binding.textHeading.setText(new SimpleDateFormat("M月d日", Locale.CHINA).format(selected.getTime()) + " · 日志 " + rows.size() + " 篇");
        binding.message.setVisibility(rows.isEmpty() && binding.progress.getVisibility() != View.VISIBLE ? View.VISIBLE : View.GONE);
        if (rows.isEmpty()) binding.message.setText("当天暂无日志");
        binding.list.setAdapter(new LogRowsAdapter(rows));
    }

    private static List<Map<String, Object>> calendarEntries(List<Map<String, Object>> logs) {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Map<String, Object> log : logs) {
            String date = text(log, "log_date");
            if (date.isEmpty()) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            String title = text(log, "work_item");
            if (title.isEmpty()) title = text(log, "work_content");
            entry.put("content", title.isEmpty() ? "工作日志" : title);
            entry.put("start_date", date);
            entry.put("end_date", date);
            entry.put("completed", "COMPLETED".equalsIgnoreCase(text(log, "completion_status")));
            entries.add(entry);
        }
        return entries;
    }

    @SuppressWarnings("unchecked") private static Map<String, Object> cast(Object value) { return (Map<String, Object>) value; }
    private static String text(Map<String, Object> row, String key) {
        return Objects.toString(PeopleDirectory.value(row, key), "").trim();
    }

    private static final class LogRowsAdapter extends RecyclerView.Adapter<LogRowsAdapter.Holder> {
        private final List<Map<String, Object>> rows;
        LogRowsAdapter(List<Map<String, Object>> rows) { this.rows = rows; }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            MaterialCardView card = new MaterialCardView(parent.getContext());
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, -2);
            params.bottomMargin = dp(parent, 10); card.setLayoutParams(params);
            card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(parent.getContext(), R.color.color_surface));
            card.setStrokeColor(androidx.core.content.ContextCompat.getColor(parent.getContext(), R.color.color_divider));
            card.setStrokeWidth(dp(parent, 1)); card.setRadius(dp(parent, 12));
            android.widget.TextView text = (android.widget.TextView) LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, card, false);
            text.setTextColor(androidx.core.content.ContextCompat.getColor(parent.getContext(), R.color.color_text_primary));
            text.setTextSize(14); text.setPadding(dp(parent, 16), dp(parent, 14), dp(parent, 16), dp(parent, 14));
            card.addView(text); return new Holder(card, text);
        }
        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            Map<String, Object> row = rows.get(position);
            String title = text(row, "work_item"), content = text(row, "work_content");
            holder.text.setText((title.isEmpty() ? "工作日志" : title) + (content.isEmpty() ? "" : "\n" + content));
        }
        @Override public int getItemCount() { return rows.size(); }
        private static int dp(View view, int value) { return Math.round(value * view.getResources().getDisplayMetrics().density); }
        static final class Holder extends RecyclerView.ViewHolder {
            final android.widget.TextView text;
            Holder(View item, android.widget.TextView text) { super(item); this.text = text; }
        }
    }

    @Override protected void onDestroy() {
        request.dispose(); if (datePicker != null) datePicker.dismiss(); binding = null; super.onDestroy();
    }
}
