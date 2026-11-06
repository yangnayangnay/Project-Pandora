package com.example.project_pandora.ui.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.SerialDisposable;

/** Keeps task filtering independent of the event board, including retries and pending requests. */
public final class HomeBoardLoader {
    public interface Listener {
        void onLoading(boolean events);
        void onEvents(List<Map<String, Object>> events);
        void onTasks(List<Map<String, Object>> tasks);
        void onError(Throwable error, boolean events);
    }

    private final Function<Boolean, Observable<Map<String, Object>>> source;
    private final Scheduler scheduler;
    private final Listener listener;
    private final SerialDisposable request = new SerialDisposable();
    private List<Map<String, Object>> events;
    private List<Map<String, Object>> tasks;
    private boolean tasksCompleted;

    public HomeBoardLoader(Function<Boolean, Observable<Map<String, Object>>> source,
                           Scheduler scheduler, Listener listener) {
        this.source = source;
        this.scheduler = scheduler;
        this.listener = listener;
    }

    public void load(boolean completed, boolean refreshEvents) {
        load(completed, refreshEvents, false);
    }

    /** Refresh a visible board without replacing its content with a loading placeholder. */
    public void refresh(boolean completed) {
        load(completed, true, tasks != null && tasksCompleted == completed);
    }

    private void load(boolean completed, boolean refreshEvents, boolean background) {
        if (request.isDisposed()) return;
        request.set(null);
        // A filter selected before the first response must still finish loading the events.
        boolean includeEvents = refreshEvents || events == null;
        if (!background) {
            tasks = null;
            listener.onLoading(includeEvents && events == null);
        }
        request.set(Observable.defer(() -> source.apply(completed)).observeOn(scheduler).subscribe(data -> {
            if (includeEvents) {
                List<Map<String, Object>> nextEvents = rows(data, "events");
                if (events == null || !events.equals(nextEvents)) {
                    events = new ArrayList<>(nextEvents);
                    listener.onEvents(nextEvents);
                }
            }
            List<Map<String, Object>> nextTasks = rows(data, "tasks");
            boolean changed = tasks == null || !tasks.equals(nextTasks);
            tasks = new ArrayList<>(nextTasks);
            tasksCompleted = completed;
            if (!background || changed) listener.onTasks(nextTasks);
        }, error -> {
            if (!background) listener.onError(error, includeEvents && events == null);
        }));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof List ? (List<Map<String, Object>>) value : Collections.emptyList();
    }

    public void close() { request.dispose(); }
}
