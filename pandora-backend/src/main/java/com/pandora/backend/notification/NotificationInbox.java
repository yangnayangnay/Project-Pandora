package com.pandora.backend.notification;

import com.pandora.backend.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Short asynchronous waits: no servlet thread or database connection held while waiting. */
@Service
public class NotificationInbox {
    private static final Logger log = LoggerFactory.getLogger(NotificationInbox.class);
    private final JdbcTemplate db;
    private final Set<Waiter> waiting = ConcurrentHashMap.newKeySet();
    public NotificationInbox(JdbcTemplate db) { this.db = db; }

    public ApiResponse<List<Map<String,Object>>> snapshot(long user, long after) {
        var totals = db.queryForMap("SELECT COALESCE(MAX(id),0) AS newest, COUNT(CASE WHEN is_read=0 THEN 1 END) AS unread FROM notifications WHERE target_user_id=?", user);
        long max = ((Number)totals.get("newest")).longValue();
        long cursor = after < 0 || after > max ? 0 : after;
        var response = ApiResponse.success(db.queryForList("SELECT * FROM notifications WHERE target_user_id=? AND id>? AND id<=? ORDER BY id ASC LIMIT 100", user, cursor, max));
        response.setMaxNotificationId(max);
        response.setUnreadCount(((Number)totals.get("unread")).longValue());
        return response;
    }

    public DeferredResult<ApiResponse<List<Map<String,Object>>>> await(long user, long after, long knownUnread, int seconds) {
        var result = new DeferredResult<ApiResponse<List<Map<String,Object>>>>(Math.max(1, Math.min(25,seconds))*1000L);
        var waiter = new Waiter(user, after, knownUnread, result);
        result.onCompletion(() -> waiting.remove(waiter));
        result.onTimeout(() -> { waiting.remove(waiter); complete(waiter, true); });
        // A bounded number of outstanding connections per account prevents abandoned wait accumulation.
        if (waiting.stream().filter(w -> w.user==user).count() >= 4) {
            result.setResult(snapshot(user,after));
            return result;
        }
        waiting.add(waiter);
        complete(waiter, false); // Register first so a simultaneous committed notification cannot be missed.
        return result;
    }

    @Scheduled(fixedDelay=1000)
    public void deliverChanges() {
        for (var waiter:waiting) complete(waiter, false);
    }

    private void complete(Waiter w, boolean timeout) {
        if (w.result.isSetOrExpired()) { waiting.remove(w); return; }
        try {
            var response = snapshot(w.user,w.after);
            if (timeout || !response.getData().isEmpty() || response.getUnreadCount()!=w.unread || w.after>response.getMaxNotificationId()) {
                w.result.setResult(response);
                waiting.remove(w);
            }
        } catch (Exception error) {
            log.warn("Notification wait failed for user {}",w.user,error);
            w.result.setErrorResult(error);
            waiting.remove(w);
        }
    }
    private record Waiter(long user,long after,long unread,DeferredResult<ApiResponse<List<Map<String,Object>>>> result) {}
}
