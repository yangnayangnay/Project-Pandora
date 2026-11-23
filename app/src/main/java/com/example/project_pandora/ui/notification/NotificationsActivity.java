package com.example.project_pandora.ui.notification;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.core.content.ContextCompat;
import androidx.core.view.*;
import androidx.recyclerview.widget.*;
import com.example.project_pandora.R;
import com.example.project_pandora.ui.PandoraActivity;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.util.*;
import com.example.project_pandora.data.remote.NotificationApi;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.*;
import java.util.*;

/** All notifications, including read records; every request is scoped by server authentication. */
public class NotificationsActivity extends PandoraActivity {
    private final List<Map<String, Object>> rows = new ArrayList<>();
    private final SerialDisposable loading = new SerialDisposable();
    private final CompositeDisposable requests = new CompositeDisposable();
    private final Set<Long> marking = new HashSet<>();
    private NotificationApi api;
    private TextView status;
    private MaterialButton refresh;
    private RecyclerView list;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        setContentView(root);
        com.example.project_pandora.ui.widget.CompanyBackgrounds.bind(root,
                com.example.project_pandora.ui.widget.CompanyBackgrounds.Scene.HOME);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        MaterialToolbar toolbar = new MaterialToolbar(this); toolbar.setTitle("全部通知");
        toolbar.setTitleTextColor(color(R.color.color_text_primary)); root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(56)));
        toolbar.setBackgroundColor(color(R.color.color_surface));
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationIconTint(color(R.color.color_text_primary));
        toolbar.setNavigationContentDescription("返回首页");
        toolbar.setNavigationOnClickListener(v -> finish());
        refresh = new MaterialButton(this); refresh.setText("刷新通知");
        LinearLayout.LayoutParams refreshLayout = new LinearLayout.LayoutParams(-1, -2);
        refreshLayout.setMargins(dp(16), dp(8), dp(16), dp(8)); root.addView(refresh, refreshLayout);
        status = new TextView(this); status.setPadding(dp(20), dp(12), dp(20), dp(12));
        status.setTextColor(color(R.color.color_text_primary)); status.setTextSize(14); status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));
        list = new RecyclerView(this); list.setLayoutManager(new LinearLayoutManager(this)); list.setAdapter(adapter);
        list.setPadding(dp(16), 0, dp(16), dp(16)); list.setClipToPadding(false);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        api = NetworkClient.getInstance(this).createService(NotificationApi.class);
        refresh.setOnClickListener(v -> load()); load();
    }
    private void load() {
        refresh.setEnabled(false); status.setVisibility(View.VISIBLE); status.setText("正在加载通知…");
        loading.set(api.listNotifications(false).subscribeOn(Schedulers.io()).timeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
            refresh.setEnabled(true);
            if (!response.isSuccess()) { status.setText("加载失败：" + response.getMessage() + "，请点击刷新重试"); return; }
            rows.clear(); if (response.getData() != null) rows.addAll(response.getData());
            adapter.notifyDataSetChanged(); status.setText(rows.isEmpty() ? "暂无通知" : "共 " + rows.size() + " 条通知 · 点击未读通知可标记已读");
        }, error -> { refresh.setEnabled(true); status.setText("加载失败：" + NetworkErrorClassifier.classify(error) + "，请点击刷新重试"); }));
    }
    private void mark(Map<String, Object> notification) {
        Long id = NumberCompat.toLong(value(notification, "id"));
        if (id == null || NumberCompat.toBool(value(notification, "is_read")) || !marking.add(id)) return;
        requests.add(api.markAsRead(id).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
            marking.remove(id);
            if (response.isSuccess()) { notification.put("is_read", true); adapter.notifyDataSetChanged(); }
            else Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show();
        }, error -> { marking.remove(id); Toast.makeText(this, "标记失败，请重试", Toast.LENGTH_LONG).show(); }));
    }
    private final RecyclerView.Adapter<Holder> adapter = new RecyclerView.Adapter<Holder>() {
        @Override public Holder onCreateViewHolder(ViewGroup parent, int type) {
            MaterialCardView card = new MaterialCardView(NotificationsActivity.this); card.setRadius(dp(16));
            card.setCardElevation(dp(1)); card.setStrokeWidth(dp(1)); card.setStrokeColor(color(R.color.color_divider)); card.setClickable(true); card.setFocusable(true);
            LinearLayout body = new LinearLayout(NotificationsActivity.this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(16), dp(14), dp(16), dp(14));
            TextView heading = new TextView(NotificationsActivity.this); heading.setTextSize(13); heading.setTextColor(color(R.color.color_text_secondary)); body.addView(heading);
            TextView content = new TextView(NotificationsActivity.this); content.setTextSize(15); content.setTextColor(color(R.color.color_text_primary)); content.setPadding(0, dp(8), 0, dp(6)); body.addView(content);
            TextView time = new TextView(NotificationsActivity.this); time.setTextSize(12); time.setTextColor(color(R.color.color_text_secondary)); body.addView(time);
            card.addView(body); RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, -2); params.bottomMargin = dp(10); card.setLayoutParams(params);
            return new Holder(card, heading, content, time);
        }
        @Override public void onBindViewHolder(Holder holder, int position) {
            Map<String, Object> row = rows.get(position); boolean read = NumberCompat.toBool(value(row, "is_read"));
            holder.heading.setText((read ? "已读 · " : "未读 · ") + title(Objects.toString(value(row, "type"), "")));
            holder.content.setText(Objects.toString(value(row, "content"), ""));
            holder.time.setText(Objects.toString(value(row, "created_at"), "").replace('T', ' '));
            ((MaterialCardView) holder.itemView).setCardBackgroundColor(color(read ? R.color.color_surface : R.color.color_surface_soft));
            holder.itemView.setOnClickListener(v -> {
                mark(row);
                Long logId = com.example.project_pandora.notification.NotificationContent.logId(row);
                if (logId != null) startActivity(new android.content.Intent(NotificationsActivity.this,
                        com.example.project_pandora.ui.log.LogDetailActivity.class)
                        .putExtra(com.example.project_pandora.ui.log.LogDetailActivity.EXTRA_LOG_ID, logId.longValue()));
            });
        }
        @Override public int getItemCount() { return rows.size(); }
    };
    private static String title(String type) {
        return com.example.project_pandora.notification.NotificationContent.listTitle(type);
    }
    private static Object value(Map<String, Object> row, String key) { Object result = row.get(key); return result == null ? row.get(key.toUpperCase(Locale.ROOT)) : result; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int color(int id) { return ContextCompat.getColor(this, id); }
    private static final class Holder extends RecyclerView.ViewHolder {
        final TextView heading, content, time;
        Holder(View item, TextView heading, TextView content, TextView time) { super(item); this.heading = heading; this.content = content; this.time = time; }
    }
    @Override protected void onDestroy() { loading.dispose(); requests.dispose(); super.onDestroy(); }
}
