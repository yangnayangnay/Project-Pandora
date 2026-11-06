package com.example.project_pandora.ui.home;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

import static org.junit.Assert.*;

public class HomeBoardLoaderTest {
    private static Map<String, Object> response(String event, String task) {
        Map<String, Object> data = new HashMap<>();
        data.put("events", Collections.singletonList(Collections.singletonMap("content", event)));
        data.put("tasks", Collections.singletonList(Collections.singletonMap("name", task)));
        return data;
    }

    private static final class Display implements HomeBoardLoader.Listener {
        final List<Boolean> loadingEvents = new ArrayList<>(), errorEvents = new ArrayList<>();
        final List<List<Map<String, Object>>> events = new ArrayList<>(), tasks = new ArrayList<>();
        @Override public void onLoading(boolean includeEvents) { loadingEvents.add(includeEvents); }
        @Override public void onEvents(List<Map<String, Object>> rows) { events.add(rows); }
        @Override public void onTasks(List<Map<String, Object>> rows) { tasks.add(rows); }
        @Override public void onError(Throwable error, boolean includeEvents) { errorEvents.add(includeEvents); }
    }

    @Test public void taskFilterLeavesEventViewsUntouchedEvenIfResponseContainsDifferentEvents() {
        Display display = new Display();
        List<Boolean> filters = new ArrayList<>();
        HomeBoardLoader loader = new HomeBoardLoader(completed -> {
            filters.add(completed);
            return Observable.just(response(completed ? "changed event" : "original event", completed ? "done" : "todo"));
        }, Schedulers.trampoline(), display);
        loader.load(false, true);
        loader.load(true, false);
        assertEquals(Arrays.asList(false, true), filters);
        assertEquals(Arrays.asList(true, false), display.loadingEvents);
        assertEquals(1, display.events.size());
        assertEquals("original event", display.events.get(0).get(0).get("content"));
        assertEquals("done", display.tasks.get(1).get(0).get("name"));
        loader.close();
    }

    @Test public void failedFilterAndRetryOnlyChangeTaskBoard() {
        Display display = new Display();
        Deque<Observable<Map<String, Object>>> responses = new ArrayDeque<>();
        responses.add(Observable.just(response("event", "todo")));
        responses.add(Observable.error(new IllegalStateException("offline")));
        responses.add(Observable.just(response("unrelated new event", "done")));
        HomeBoardLoader loader = new HomeBoardLoader(done -> responses.removeFirst(), Schedulers.trampoline(), display);
        loader.load(false, true);
        loader.load(true, false);
        loader.load(true, false);
        assertEquals(Collections.singletonList(false), display.errorEvents);
        assertEquals(Arrays.asList(true, false, false), display.loadingEvents);
        assertEquals(1, display.events.size());
        assertEquals(2, display.tasks.size());
        loader.close();
    }

    @Test public void changingFilterBeforeInitialResponseStillLoadsEventsAndCancelsOldResponse() {
        Display display = new Display();
        PublishSubject<Map<String, Object>> initial = PublishSubject.create(), filtered = PublishSubject.create();
        HomeBoardLoader loader = new HomeBoardLoader(done -> done ? filtered : initial, Schedulers.trampoline(), display);
        loader.load(false, true);
        assertTrue(initial.hasObservers());
        loader.load(true, false);
        assertFalse(initial.hasObservers());
        initial.onNext(response("stale event", "stale task"));
        filtered.onNext(response("current event", "done"));
        assertEquals(1, display.events.size());
        assertEquals("current event", display.events.get(0).get(0).get("content"));
        assertEquals(1, display.tasks.size());
        assertEquals("done", display.tasks.get(0).get(0).get("name"));
        loader.close();
    }

    @Test public void fastFilterChangesDoNotAllowSlowerResponsesToOverwriteLatestTasks() {
        Display display = new Display();
        PublishSubject<Map<String, Object>> pending = PublishSubject.create();
        HomeBoardLoader loader = new HomeBoardLoader(done -> done ? pending : Observable.just(response("event", "todo")),
                Schedulers.trampoline(), display);
        loader.load(false, true);
        loader.load(true, false);
        loader.load(false, false);
        pending.onNext(response("stale", "done"));
        assertFalse(pending.hasObservers());
        assertEquals(1, display.events.size());
        assertEquals(2, display.tasks.size());
        assertEquals("todo", display.tasks.get(1).get(0).get("name"));
        loader.close();
    }

    @Test public void resumeRefreshOnlyRebuildsEventsWhenTheirContentChanges() {
        Display display = new Display();
        Deque<Observable<Map<String, Object>>> responses = new ArrayDeque<>();
        responses.add(Observable.just(response("original", "todo")));
        responses.add(Observable.just(response("original", "todo")));
        responses.add(Observable.just(response("changed", "todo")));
        HomeBoardLoader loader = new HomeBoardLoader(done -> responses.removeFirst(), Schedulers.trampoline(), display);
        loader.load(false, true);
        loader.load(false, true);
        assertEquals(1, display.events.size());
        loader.load(false, true);
        assertEquals(2, display.events.size());
        assertEquals(Arrays.asList(true, false, false), display.loadingEvents);
        loader.close();
    }

    @Test public void initialFailureCanRecoverBothBoardsOnFilterRetry() {
        Display display = new Display();
        HomeBoardLoader loader = new HomeBoardLoader(done -> done ? Observable.just(response("event", "done"))
                : Observable.error(new IllegalStateException("offline")), Schedulers.trampoline(), display);
        loader.load(false, true);
        assertEquals(Collections.singletonList(true), display.errorEvents);
        loader.load(true, false);
        assertEquals(1, display.events.size());
        assertEquals(1, display.tasks.size());
        loader.close();
    }

    @Test public void destroyingViewCancelsPendingCallbacksAndFurtherLoads() {
        Display display = new Display();
        PublishSubject<Map<String, Object>> pending = PublishSubject.create();
        HomeBoardLoader loader = new HomeBoardLoader(done -> pending, Schedulers.trampoline(), display);
        loader.load(false, true);
        loader.close();
        assertFalse(pending.hasObservers());
        pending.onNext(response("event", "todo"));
        loader.load(true, false);
        assertTrue(display.events.isEmpty());
        assertTrue(display.tasks.isEmpty());
        assertEquals(1, display.loadingEvents.size());
    }

    @Test public void returningWithUnchangedContentDoesNotShowLoadingOrRebuildEitherBoard() {
        Display display = new Display();
        HomeBoardLoader loader = new HomeBoardLoader(done -> Observable.just(response("event", "todo")), Schedulers.trampoline(), display);
        loader.refresh(false);
        loader.refresh(false);
        assertEquals(Collections.singletonList(true), display.loadingEvents);
        assertEquals(1, display.events.size());
        assertEquals(1, display.tasks.size());
        loader.close();
    }

    @Test public void backgroundRefreshUpdatesOnlyChangedBoardWithoutLoadingPlaceholder() {
        Display display = new Display();
        Deque<Observable<Map<String, Object>>> responses = new ArrayDeque<>();
        responses.add(Observable.just(response("event", "todo")));
        responses.add(Observable.just(response("event", "updated task")));
        responses.add(Observable.just(response("updated event", "updated task")));
        HomeBoardLoader loader = new HomeBoardLoader(done -> responses.removeFirst(), Schedulers.trampoline(), display);
        loader.refresh(false); loader.refresh(false);
        assertEquals(1, display.events.size()); assertEquals(2, display.tasks.size());
        loader.refresh(false);
        assertEquals(2, display.events.size()); assertEquals(2, display.tasks.size());
        assertEquals(1, display.loadingEvents.size()); loader.close();
    }

    @Test public void offlineReturnPreservesLoadedContentAndCanRefreshAgain() {
        Display display = new Display();
        Deque<Observable<Map<String, Object>>> responses = new ArrayDeque<>();
        responses.add(Observable.just(response("event", "todo")));
        responses.add(Observable.error(new IllegalStateException("offline")));
        responses.add(Observable.just(response("event", "changed")));
        HomeBoardLoader loader = new HomeBoardLoader(done -> responses.removeFirst(), Schedulers.trampoline(), display);
        loader.refresh(false); loader.refresh(false);
        assertTrue(display.errorEvents.isEmpty()); assertEquals(1, display.loadingEvents.size());
        assertEquals(1, display.events.size()); assertEquals(1, display.tasks.size());
        loader.refresh(false); assertEquals(2, display.tasks.size()); loader.close();
    }

    @Test public void refreshWithDifferentFilterMustLoadTasksAndReportItsFailure() {
        Display display = new Display();
        HomeBoardLoader loader = new HomeBoardLoader(done -> done ? Observable.error(new IllegalStateException("offline"))
                : Observable.just(response("event", "todo")), Schedulers.trampoline(), display);
        loader.refresh(false); loader.refresh(true);
        assertEquals(Arrays.asList(true, false), display.loadingEvents);
        assertEquals(Collections.singletonList(false), display.errorEvents);
        loader.refresh(false);
        assertEquals(Arrays.asList(true, false, false), display.loadingEvents);
        assertEquals(2, display.tasks.size()); loader.close();
    }

    @Test public void closingDuringBackgroundRefreshPreventsUpdatesAndFurtherRequests() {
        Display display = new Display();
        PublishSubject<Map<String, Object>> pending = PublishSubject.create();
        AtomicInteger requests = new AtomicInteger();
        HomeBoardLoader loader = new HomeBoardLoader(done -> requests.incrementAndGet() == 1
                ? Observable.just(response("event", "todo")) : pending, Schedulers.trampoline(), display);
        loader.refresh(false); loader.refresh(false); assertTrue(pending.hasObservers());
        loader.close(); assertFalse(pending.hasObservers());
        pending.onNext(response("changed", "changed")); loader.refresh(false);
        assertEquals(2, requests.get()); assertEquals(1, display.tasks.size()); assertEquals(1, display.events.size());
    }
}
