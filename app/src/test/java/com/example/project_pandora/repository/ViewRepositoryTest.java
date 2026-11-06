package com.example.project_pandora.repository;

import com.example.project_pandora.core.exception.BusinessException;
import com.example.project_pandora.core.network.ApiResponse;
import com.example.project_pandora.data.remote.LogApi;
import com.example.project_pandora.data.remote.ViewApi;
import com.example.project_pandora.ui.log.PersonalWorkSlots;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/** Regression contract for the editor-backed personal-important work data path. */
public class ViewRepositoryTest {

    @Before
    public void useTrampolineForIo() {
        RxJavaPlugins.setIoSchedulerHandler(ignored -> Schedulers.trampoline());
    }

    @After
    public void resetRxJavaPlugins() {
        RxJavaPlugins.reset();
    }

    @Test
    public void mindMapStripsLegacyPersonalRowsWithoutMutatingOverview() {
        Map<String, Object> account = map("name", "林晓", "birthday", "1998-05-20");
        List<Map<String, Object>> legacyRows = list(
                map("rankOrder", 1, "content", "账号信息-姓名：林晓"),
                map("rankOrder", 2, "content", "AI 生成：生日 1998-05-20"));
        List<Map<String, Object>> companyImportant = list(map("content", "公司重点"));
        List<Map<String, Object>> companyDispatch = list(map("task_name", "任务派发"));
        List<Map<String, Object>> personalLog = list(map("work_item", "个人日志"));
        Map<String, Object> overview = map(
                "ACCOUNT_INFO", account,
                "name", "林晓",
                "birthday", "1998-05-20",
                "personalImportant", legacyRows,
                "companyImportant", companyImportant,
                "companyDispatch", companyDispatch,
                "personalLog", personalLog);
        Map<String, Object> original = new LinkedHashMap<>(overview);
        ViewRepository repository = new ViewRepository(
                viewApi(() -> Observable.just(success(overview))),
                unusedLogApi());

        Map<String, Object> result = repository.getMindMap().blockingFirst();

        assertNotSame(overview, result);
        assertFalse(result.containsKey("personalImportant"));
        assertEquals(account, result.get("ACCOUNT_INFO"));
        assertEquals("林晓", result.get("name"));
        assertEquals("1998-05-20", result.get("birthday"));
        assertSame(companyImportant, result.get("companyImportant"));
        assertSame(companyDispatch, result.get("companyDispatch"));
        assertSame(personalLog, result.get("personalLog"));
        assertEquals(original, overview);
        assertSame(legacyRows, overview.get("personalImportant"));
    }

    @Test
    public void personalWorkUsesEditorEndpointInsteadOfOverviewProfileRows() {
        List<Map<String, Object>> saved = list(
                map("rankOrder", 2.0, "id", 22.0, "content", "整理发布清单"),
                map("rankOrder", 5.0, "id", 55.0, "content", "跟进跨部门依赖"));
        List<Map<String, Object>> legacyRows = list(
                map("rankOrder", 1, "content", "账号信息-姓名：林晓"),
                map("rankOrder", 2, "content", "AI 生成：生日 1998-05-20"));
        Map<String, Object> overview = map("personalImportant", legacyRows);
        List<String> requestedWorkTypes = new ArrayList<>();
        LogApi logApi = logApi(() -> Observable.just(success(saved)), requestedWorkTypes);
        ViewRepository repository = new ViewRepository(
                viewApi(() -> Observable.just(success(overview))),
                logApi);

        Map<String, Object> overviewResult = repository.getMindMap().blockingFirst();
        List<Map<String, Object>> fromRepository = repository.getPersonalImportantWork().blockingFirst();
        List<Map<String, Object>> fromEditorRepository =
                new LogRepository(logApi).getTop10("PERSONAL_IMPORTANT").blockingFirst();

        assertFalse(overviewResult.containsKey("personalImportant"));
        assertEquals(saved, fromRepository);
        assertEquals(saved, fromEditorRepository);
        assertEquals(list("PERSONAL_IMPORTANT", "PERSONAL_IMPORTANT"), requestedWorkTypes);
    }

    @Test
    public void repeatedReadsSeeSavedChangesAndEmptyWithoutLegacyFallback() {
        Map<String, Object> first = map("rankOrder", 1.0, "content", "完成季度复盘");
        Map<String, Object> removed = map("rankOrder", 2.0, "content", "跟进跨部门依赖");
        Map<String, Object> updated = map("rankOrder", 1.0, "content", "完成季度复盘（已更新）");
        Deque<ApiResponse<List<Map<String, Object>>>> responses = new ArrayDeque<>();
        responses.add(success(list(first, removed)));
        responses.add(success(list(updated)));
        responses.add(success(Collections.<Map<String, Object>>emptyList()));
        List<String> requestedWorkTypes = new ArrayList<>();
        LogApi logApi = logApi(() -> Observable.just(responses.removeFirst()), requestedWorkTypes);
        Map<String, Object> legacyOverview = map(
                "personalImportant", list(map("rankOrder", 1, "content", "旧档案行")));
        ViewRepository repository = new ViewRepository(
                viewApi(() -> Observable.just(success(legacyOverview))),
                logApi);

        List<Map<String, Object>> initial = repository.getPersonalImportantWork().blockingFirst();
        List<Map<String, Object>> afterEdit = repository.getPersonalImportantWork().blockingFirst();
        List<Map<String, Object>> afterRemoval = repository.getPersonalImportantWork().blockingFirst();

        assertEquals(2, initial.size());
        assertEquals("完成季度复盘", initial.get(0).get("content"));
        assertEquals("跟进跨部门依赖", initial.get(1).get("content"));
        assertEquals(1, afterEdit.size());
        assertEquals("完成季度复盘（已更新）", afterEdit.get(0).get("content"));
        assertTrue(afterRemoval.isEmpty());
        assertEquals(
                list("PERSONAL_IMPORTANT", "PERSONAL_IMPORTANT", "PERSONAL_IMPORTANT"),
                requestedWorkTypes);
    }

    @Test
    public void personalWorkBusinessErrorIsPropagatedWithoutProfileFallback() {
        ApiResponse<List<Map<String, Object>>> failure = failure(4046, "无权访问个人工作");
        List<Map<String, Object>> legacyRows = list(
                map("rankOrder", 1, "content", "账号信息-姓名：林晓"));
        ViewRepository repository = new ViewRepository(
                viewApi(() -> Observable.just(success(map("personalImportant", legacyRows)))),
                logApi(() -> Observable.just(failure), new ArrayList<>()));

        TestObserver<List<Map<String, Object>>> observer =
                repository.getPersonalImportantWork().test();

        observer.assertNoValues().assertError(error ->
                error instanceof BusinessException
                        && ((BusinessException) error).getCode() == 4046
                        && "无权访问个人工作".equals(error.getMessage()));
    }

    @Test
    public void personalWorkTransportErrorIsPropagatedWithoutProfileFallback() {
        IOException failure = new IOException("personal work transport failed");
        List<Map<String, Object>> legacyRows = list(
                map("rankOrder", 1, "content", "AI 生成：生日 1998-05-20"));
        ViewRepository repository = new ViewRepository(
                viewApi(() -> Observable.just(success(map("personalImportant", legacyRows)))),
                logApi(() -> Observable.error(failure), new ArrayList<>()));

        TestObserver<List<Map<String, Object>>> observer =
                repository.getPersonalImportantWork().test();

        observer.assertNoValues().assertError(failure);
    }

    @Test
    public void overviewFailureDoesNotPreventIndependentPersonalWorkRead() {
        IOException overviewFailure = new IOException("overview transport failed");
        List<Map<String, Object>> saved = list(
                map("rankOrder", 1.0, "content", "独立读取的个人工作"));
        List<String> requestedWorkTypes = new ArrayList<>();
        ViewRepository repository = new ViewRepository(
                viewApi(() -> Observable.error(overviewFailure)),
                logApi(() -> Observable.just(success(saved)), requestedWorkTypes));

        TestObserver<Map<String, Object>> overviewObserver = repository.getMindMap().test();
        TestObserver<List<Map<String, Object>>> personalObserver =
                repository.getPersonalImportantWork().test();

        overviewObserver.assertNoValues().assertError(IOException.class);
        personalObserver.assertValue(saved).assertComplete();
        assertEquals(list("PERSONAL_IMPORTANT"), requestedWorkTypes);
    }

    @Test
    public void delayedOverviewCannotExposeLegacyProfileRowsAfterPersonalResult() {
        PublishSubject<ApiResponse<Map<String, Object>>> overviewSubject = PublishSubject.create();
        List<Map<String, Object>> saved = list(
                map("rankOrder", 1.0, "content", "编辑器保存的个人工作"));
        List<Map<String, Object>> legacyRows = list(
                map("rankOrder", 1, "content", "账号信息-姓名：林晓"),
                map("rankOrder", 2, "content", "AI 生成：生日 1998-05-20"));
        List<Map<String, Object>> companyImportant = list(map("content", "公司重点"));
        List<Map<String, Object>> companyDispatch = list(map("task_name", "任务派发"));
        List<Map<String, Object>> personalLog = list(map("work_item", "个人日志"));
        Map<String, Object> overview = map(
                "personalImportant", legacyRows,
                "companyImportant", companyImportant,
                "companyDispatch", companyDispatch,
                "personalLog", personalLog);
        ViewRepository repository = new ViewRepository(
                viewApi(() -> overviewSubject),
                logApi(() -> Observable.just(success(saved)), new ArrayList<>()));

        TestObserver<Map<String, Object>> overviewObserver = repository.getMindMap().test();
        TestObserver<List<Map<String, Object>>> personalObserver =
                repository.getPersonalImportantWork().test();

        personalObserver.assertValue(saved).assertComplete();
        overviewObserver.assertNoValues();

        overviewSubject.onNext(success(overview));
        overviewSubject.onComplete();

        overviewObserver.assertValueCount(1).assertComplete();
        Map<String, Object> overviewResult = overviewObserver.values().get(0);
        assertFalse(overviewResult.containsKey("personalImportant"));
        assertSame(companyImportant, overviewResult.get("companyImportant"));
        assertSame(companyDispatch, overviewResult.get("companyDispatch"));
        assertSame(personalLog, overviewResult.get("personalLog"));
        personalObserver.assertValue(saved);
        assertEquals(1, personalObserver.values().size());
    }

    @Test
    public void sparseUppercaseRanksMatchEditorNumbering() {
        List<Map<String, Object>> saved = list(
                map("RANK_ORDER", 5.0, "CONTENT", "整理年度路线图"),
                map("RANK_ORDER", 2.0, "CONTENT", "跟进跨部门依赖"));
        ViewRepository repository = new ViewRepository(
                viewApi(() -> Observable.just(success(map()))),
                logApi(() -> Observable.just(success(saved)), new ArrayList<>()));

        List<Map<String, Object>> fromRepository = repository.getPersonalImportantWork().blockingFirst();
        List<Map<String, Object>> slots = PersonalWorkSlots.fromSaved(fromRepository);

        assertEquals(10, slots.size());
        assertEquals(2, slots.get(1).get("rankOrder"));
        assertEquals("跟进跨部门依赖", slots.get(1).get("content"));
        assertEquals(5, slots.get(4).get("rankOrder"));
        assertEquals("整理年度路线图", slots.get(4).get("content"));
        assertEquals("", slots.get(0).get("content"));
        assertEquals("", slots.get(2).get("content"));
        assertEquals(
                list("2. 跟进跨部门依赖", "5. 整理年度路线图"),
                numberedContent(slots));
    }

    private static List<String> numberedContent(List<Map<String, Object>> slots) {
        List<String> lines = new ArrayList<>();
        for (Map<String, Object> slot : slots) {
            String content = String.valueOf(slot.get("content"));
            if (!content.trim().isEmpty()) {
                lines.add(slot.get("rankOrder") + ". " + content);
            }
        }
        return lines;
    }

    private static ViewApi viewApi(
            Supplier<Observable<ApiResponse<Map<String, Object>>>> mindMapResponse) {
        return apiProxy(ViewApi.class, (proxy, method, args) -> {
            if ("getMindMap".equals(method.getName())) {
                return mindMapResponse.get();
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static LogApi logApi(
            Supplier<Observable<ApiResponse<List<Map<String, Object>>>>> top10Response,
            List<String> requestedWorkTypes) {
        return apiProxy(LogApi.class, (proxy, method, args) -> {
            if ("getTop10".equals(method.getName())) {
                requestedWorkTypes.add((String) args[0]);
                return top10Response.get();
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static LogApi unusedLogApi() {
        return logApi(
                () -> {
                    throw new AssertionError("LogApi must not be used by getMindMap");
                },
                new ArrayList<>());
    }

    private static <T> T apiProxy(Class<T> type, InvocationHandler handler) {
        InvocationHandler objectSafeHandler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if ("toString".equals(method.getName())) {
                    return "Fake" + type.getSimpleName();
                }
                if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(method.getName())) {
                    return proxy == args[0];
                }
            }
            return handler.invoke(proxy, method, args);
        };
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                objectSafeHandler));
    }

    private static <T> ApiResponse<T> success(T data) {
        return response(200, "ok", data);
    }

    private static <T> ApiResponse<T> failure(int code, String message) {
        return response(code, message, null);
    }

    private static <T> ApiResponse<T> response(int code, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    private static Map<String, Object> map(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("map entries must be key/value pairs");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put((String) entries[i], entries[i + 1]);
        }
        return result;
    }

    @SafeVarargs
    private static <T> List<T> list(T... values) {
        return new ArrayList<>(Arrays.asList(values));
    }
}
