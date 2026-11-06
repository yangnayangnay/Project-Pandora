package com.example.project_pandora.repository;

import com.example.project_pandora.core.exception.BusinessException;
import com.example.project_pandora.core.network.ApiResponse;
import com.example.project_pandora.data.remote.UserApi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

import static org.junit.Assert.*;

public class TaskDisplaySettingsRepositoryTest {
    private final AtomicBoolean cache = new AtomicBoolean(true);
    private final Stub stub = new Stub();

    @Before public void directIo() { RxJavaPlugins.setIoSchedulerHandler(ignored -> Schedulers.trampoline()); }
    @After public void resetRx() { RxJavaPlugins.reset(); }

    @Test public void readsServerRepresentationsAndCachesTheAccountChoice() {
        Object[] values = {0.0, 1.0, false, true, "0", "1", "false", "true"};
        for (String key : new String[]{"showCompletedTasks", "SHOW_COMPLETED_TASKS", "show_completed_tasks"}) {
            for (int index = 0; index < values.length; index++) {
                Object value = values[index];
                stub.read = () -> Observable.just(success(Collections.singletonMap(key, value)));
                boolean expected = index % 2 == 1;
                assertEquals(expected, repository().loadShowCompleted().blockingFirst());
                assertEquals(expected, cache.get());
            }
        }
        assertNull("reading must never write settings", stub.saved);
    }

    @Test public void missingOrUnrecognizedSettingKeepsThePreviousChoice() {
        cache.set(false);
        stub.read = () -> Observable.just(success(Collections.singletonMap("birthday", "2000-01-01")));
        repository().loadShowCompleted().test().assertResult(false);
        stub.read = () -> Observable.just(success(Collections.singletonMap("showCompletedTasks", "invalid")));
        repository().loadShowCompleted().test().assertResult(false);
        assertFalse(cache.get());
    }

    @Test public void settingsReadFailureIsReportedWhileListsUseTheLastSavedChoice() {
        cache.set(false);
        IOException offline = new IOException("offline");
        stub.read = () -> Observable.error(offline);
        repository().loadShowCompleted().test().assertNoValues().assertError(offline);
        repository().showCompletedForLists().test().assertResult(false);
        assertFalse(cache.get());
    }

    @Test public void businessReadErrorDoesNotResetTheCacheOrBreakTaskLists() {
        cache.set(false);
        stub.read = () -> Observable.just(failure(403, "forbidden"));
        repository().loadShowCompleted().test().assertNoValues().assertError(error ->
                error instanceof BusinessException && ((BusinessException) error).getCode() == 403);
        repository().showCompletedForLists().test().assertResult(false);
        assertFalse(cache.get());
    }

    @Test public void nullSettingsPayloadUsesTheListFallback() {
        cache.set(false);
        stub.read = () -> Observable.just(success(null));
        repository().loadShowCompleted().test().assertNoValues().assertError(IllegalStateException.class);
        repository().showCompletedForLists().test().assertResult(false);
    }

    @Test public void successfulSaveWritesOnlyTheRequestedSettingAndIsVisibleToANewReader() {
        repository().saveShowCompleted(false).test().assertResult(false);
        assertEquals(Collections.singletonMap("showCompletedTasks", false), stub.saved);
        assertFalse(repository().cachedShowCompleted());
        repository().saveShowCompleted(true).test().assertResult(true);
        assertEquals(Collections.singletonMap("showCompletedTasks", true), stub.saved);
        assertTrue(repository().cachedShowCompleted());
    }

    @Test public void unsuccessfulSavePreservesTheLastChoiceForRollback() {
        stub.write = Observable.just(failure(500, "save failed"));
        repository().saveShowCompleted(false).test().assertNoValues().assertError(BusinessException.class);
        assertTrue(cache.get());
        cache.set(false);
        stub.write = Observable.error(new IOException("offline"));
        repository().saveShowCompleted(true).test().assertNoValues().assertError(IOException.class);
        assertFalse(cache.get());
    }

    @Test public void cacheChangesOnlyAfterTheServerAcknowledgesTheWrite() {
        PublishSubject<ApiResponse<Void>> pending = PublishSubject.create(); stub.write = pending;
        TestObserver<Boolean> saved = repository().saveShowCompleted(false).test();
        assertTrue(cache.get()); saved.assertNoValues();
        pending.onNext(success(null)); pending.onComplete();
        saved.assertResult(false); assertFalse(cache.get());
    }

    @Test public void cancellingAnOldReadPreventsItFromOverwritingANewerSave() {
        PublishSubject<ApiResponse<Map<String, Object>>> pending = PublishSubject.create();
        stub.read = () -> pending;
        TestObserver<Boolean> oldRead = repository().loadShowCompleted().test();
        oldRead.dispose();
        repository().saveShowCompleted(false).test().assertResult(false);
        pending.onNext(success(Collections.singletonMap("showCompletedTasks", 1)));
        assertFalse(cache.get());
    }

    @Test public void cacheKeysKeepDifferentAccountsAndServersSeparate() {
        assertNotEquals(TaskDisplaySettingsRepository.cacheKey("https://one/api/", 1L),
                TaskDisplaySettingsRepository.cacheKey("https://one/api/", 2L));
        assertNotEquals(TaskDisplaySettingsRepository.cacheKey("https://one/api/", 1L),
                TaskDisplaySettingsRepository.cacheKey("https://two/api/", 1L));
    }

    private TaskDisplaySettingsRepository repository() {
        return new TaskDisplaySettingsRepository(stub.api, cache::get, cache::set);
    }

    private static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>(); response.setCode(200); response.setData(data); return response;
    }
    private static <T> ApiResponse<T> failure(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>(); response.setCode(code); response.setMessage(message); return response;
    }

    private static final class Stub {
        Supplier<Observable<ApiResponse<Map<String, Object>>>> read = () -> Observable.just(success(Collections.emptyMap()));
        Observable<ApiResponse<Void>> write = Observable.just(success(null));
        Map<?, ?> saved;
        final UserApi api = (UserApi) Proxy.newProxyInstance(UserApi.class.getClassLoader(), new Class<?>[]{UserApi.class},
                (proxy, method, args) -> {
                    if ("getUserSettings".equals(method.getName())) return read.get();
                    if ("updateUserSettings".equals(method.getName())) { saved = (Map<?, ?>) args[0]; return write; }
                    throw new AssertionError("Unexpected API call: " + method.getName());
                });
    }
}
