package com.example.project_pandora.repository;

import com.example.project_pandora.core.network.ApiResponse;
import com.example.project_pandora.data.remote.TaskApi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.junit.Assert.*;

/** Protects calendar/table visibility with the two-state task model. */
public class TaskVisibilityTest {
    private final List<Map<String, Object>> rows = Arrays.asList(
            task(1, "IN_PROGRESS"), task(2, "COMPLETED"));
    private final List<Object[]> calls = new ArrayList<>();
    private final TaskApi api = (TaskApi) Proxy.newProxyInstance(TaskApi.class.getClassLoader(), new Class<?>[]{TaskApi.class},
            (proxy, method, args) -> {
                if (!"listTasksExtended".equals(method.getName())) throw new AssertionError(method.getName());
                calls.add(args);
                ApiResponse<List<Map<String, Object>>> response = new ApiResponse<>();
                response.setCode(200); response.setData(Collections.unmodifiableList(rows));
                return Observable.just(response);
            });

    @Before public void directIo() { RxJavaPlugins.setIoSchedulerHandler(ignored -> Schedulers.trampoline()); }
    @After public void resetRx() { RxJavaPlugins.reset(); }

    @Test public void offHidesCompletedWhileKeepingInProgressTasks() {
        List<Map<String, Object>> result = new TaskRepository(api).listTasksExtended(null, 7L, null, false).blockingFirst();
        assertEquals(Collections.singletonList("IN_PROGRESS"), statuses(result));
        assertArrayEquals(new Object[]{null, 7L, null, false}, calls.get(0));
        assertEquals(2, rows.size()); assertTrue(rows.get(0).containsKey("STATUS"));
    }

    @Test public void onRestoresCompletedForAssignedAndDispatchedScopes() {
        TaskRepository repository = new TaskRepository(api);
        List<Map<String, Object>> assigned = repository.listTasksExtended(null, 7L, null, true).blockingFirst();
        List<Map<String, Object>> dispatched = repository.listTasksExtended(null, null, 7L, true).blockingFirst();
        assertEquals(Arrays.asList("IN_PROGRESS", "COMPLETED"), statuses(assigned));
        assertEquals(statuses(assigned), statuses(dispatched));
        assertArrayEquals(new Object[]{null, null, 7L, true}, calls.get(1));
    }

    @Test public void explicitCompletedFilterCannotLeakCompletedRowsWhenSettingIsOff() {
        List<Map<String, Object>> result = new TaskRepository(api).listTasksExtended("COMPLETED", 7L, null, false).blockingFirst();
        assertEquals(Collections.singletonList("IN_PROGRESS"), statuses(result));
        assertArrayEquals(new Object[]{"COMPLETED", 7L, null, false}, calls.get(0));
    }

    private static Map<String, Object> task(int id, String status) {
        Map<String, Object> task = new LinkedHashMap<>(); task.put("ID", id); task.put("STATUS", status); return task;
    }
    private static List<String> statuses(List<Map<String, Object>> tasks) {
        return tasks.stream().map(task -> task.get("status").toString()).collect(Collectors.toList());
    }
}
