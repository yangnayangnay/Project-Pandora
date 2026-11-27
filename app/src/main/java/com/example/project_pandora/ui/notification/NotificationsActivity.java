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
    private MenuItem refresh, readAll;
    private boolean listLoading, bulkReading, loaded;
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
        MaterialToolbar toolbar = new MaterialToolbar(this); toolbar.setTitle(R.string.notifications_title);
        toolbar.setTitleTextColor(color(R.color.color_text_primary)); root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(56)));
        toolbar.setBackgroundColor(color(R.color.color_surface));
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationIconTint(color(R.color.color_text_primary));
        toolbar.setNavigationContentDescription(R.string.notifications_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_notifications);
        refresh=toolbar.getMenu().findItem(R.id.action_refresh_notifications);
        readAll=toolbar.getMenu().findItem(R.id.action_read_all_notifications);
        toolbar.setOnMenuItemClickListener(item -> {
            if(item.getItemId()==R.id.action_refresh_notifications) { load(); return true; }
            if(item.getItemId()==R.id.action_read_all_notifications) { markAll(); return true; }
            return false;
        });
        status = new TextView(this); status.setPadding(dp(20), dp(12), dp(20), dp(12));
        status.setTextColor(color(R.color.color_text_primary)); status.setTextSize(14); status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));
        list = new RecyclerView(this); list.setLayoutManager(new LinearLayoutManager(this)); list.setAdapter(adapter);
        list.setPadding(dp(16), 0, dp(16), dp(16)); list.setClipToPadding(false);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        api = NetworkClient.getInstance(this).createService(NotificationApi.class);
        load();
    }
    private void load() {
        if(bulkReading || listLoading) return;
        listLoading=true; updateActions(); status.setVisibility(View.VISIBLE); status.setText(R.string.notifications_loading);
        loading.set(api.listNotifications(false).subscribeOn(Schedulers.io()).timeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
            listLoading=false;
            if (!response.isSuccess()) { updateActions(); status.setText(getString(R.string.notifications_load_error,response.getMessage())); return; }
            rows.clear(); if (response.getData() != null) rows.addAll(response.getData());
            loaded=true;
            com.example.project_pandora.notification.NotificationState.refresh(this);
            adapter.notifyDataSetChanged(); showSummary(); updateActions();
        }, error -> { listLoading=false; updateActions(); status.setText(getString(R.string.notifications_load_error,NetworkErrorClassifier.classify(error))); }));
    }

    private long unreadInList() { return rows.stream().filter(row -> !NumberCompat.toBool(value(row,"is_read"))).count(); }
    private void updateActions() {
        if(refresh==null) return;
        refresh.setEnabled(!listLoading && !bulkReading && marking.isEmpty());
        readAll.setEnabled(loaded && !listLoading && !bulkReading && marking.isEmpty() && unreadInList()>0);
        readAll.setTitle(bulkReading ? R.string.notifications_marking_all : R.string.notifications_read_all);
    }
    private void showSummary() {
        status.setText(rows.isEmpty() ? getString(R.string.notifications_empty)
                : getString(R.string.notifications_summary,rows.size(),unreadInList()));
    }
    private void markAll() {
        if(bulkReading || listLoading || !marking.isEmpty() || unreadInList()==0) return;
        bulkReading=true; loading.set(null); updateActions(); status.setText(R.string.notifications_marking_all);
        var session=com.example.project_pandora.core.network.TokenManager.getInstance(this);
        long user=session.getUserId(); String token=session.getToken();
        requests.add(api.markAllAsRead().subscribeOn(Schedulers.io()).timeout(20,java.util.concurrent.TimeUnit.SECONDS)
                .doOnNext(response -> {
                    if(response.isSuccess() && response.getData()!=null && Objects.equals(token,session.getToken()) && user==session.getUserId()) {
                        Long boundary=NumberCompat.toLong(value(response.getData(),"readThroughId"));
                        if(boundary!=null && boundary>=0)
                            com.example.project_pandora.notification.NotificationPollEngine.confirmReadThrough(getApplicationContext(),user,boundary);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
            bulkReading=false;
            if(!Objects.equals(token,session.getToken()) || user!=session.getUserId()) { finish(); return; }
            if(!response.isSuccess() || response.getData()==null) {
                updateActions(); status.setText(getString(R.string.notifications_mark_all_error,response.getMessage())); return;
            }
            Long boundary=NumberCompat.toLong(value(response.getData(),"readThroughId"));
            if(boundary==null || boundary<0) {
                updateActions(); status.setText(R.string.notifications_mark_error); return;
            }
            for(var row:rows) {
                Long id=NumberCompat.toLong(value(row,"id"));
                if(id!=null && id<=boundary) row.put("is_read",true);
            }
            Long remaining=NumberCompat.toLong(value(response.getData(),"unreadCount"));
            if(remaining!=null) com.example.project_pandora.notification.NotificationState.publish(this,user,remaining);
            com.example.project_pandora.notification.NotificationState.refresh(this);
            adapter.notifyDataSetChanged(); showSummary(); updateActions();
            Toast.makeText(this,getString(R.string.notifications_mark_all_done,
                    NumberCompat.toLongOrDefault(value(response.getData(),"updatedCount"),0)),Toast.LENGTH_SHORT).show();
            load(); // Fetch notifications that arrived during the bulk operation.
        }, error -> {
            bulkReading=false; updateActions();
            status.setText(getString(R.string.notifications_mark_all_error,NetworkErrorClassifier.classify(error)));
        }));
    }
    private void mark(Map<String, Object> notification) {
        Long id = NumberCompat.toLong(value(notification, "id"));
        if (bulkReading || listLoading || id == null || NumberCompat.toBool(value(notification, "is_read")) || !marking.add(id)) return;
        updateActions();
        requests.add(api.markAsRead(id).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
            marking.remove(id);
            if (response.isSuccess()) {
                notification.put("is_read", true); adapter.notifyDataSetChanged();
                androidx.core.app.NotificationManagerCompat.from(this).cancel(
                        com.example.project_pandora.notification.TaskNotificationService.accountTag(this),id.intValue());
                com.example.project_pandora.notification.NotificationState.refresh(this);
                showSummary();
            }
            else Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show();
            updateActions();
        }, error -> { marking.remove(id); updateActions(); Toast.makeText(this, R.string.notifications_mark_error, Toast.LENGTH_LONG).show(); }));
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
            holder.heading.setText(getString(read ? R.string.notifications_read_label : R.string.notifications_unread_label,
                    title(Objects.toString(value(row, "type"), ""))));
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
