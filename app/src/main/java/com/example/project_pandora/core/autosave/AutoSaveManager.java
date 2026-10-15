package com.example.project_pandora.core.autosave;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.project_pandora.repository.DraftRepository;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class AutoSaveManager {

    public enum SaveStatus { IDLE, SAVING, SAVED, SAVE_FAILED }

    public interface ContentProvider {
        Map<String, Object> getContent();
    }

    public interface StatusCallback {
        void onStatusChanged(SaveStatus status);
    }

    private static final long FIRST_SAVE_DELAY_MS = 5000;
    private static final long PERIODIC_SAVE_INTERVAL_MS = 30000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final DraftRepository draftRepository;
    private final String draftType;
    private final ContentProvider contentProvider;
    private final StatusCallback statusCallback;

    private final AtomicBoolean isSaving = new AtomicBoolean(false);
    private final AtomicBoolean hasContentChanged = new AtomicBoolean(false);
    private boolean firstSaveScheduled = false;
    private boolean periodicSaveRunning = false;

    private final Runnable firstSaveRunnable = () -> {
        firstSaveScheduled = false;
        startPeriodicSave();
        doSave();
    };

    private final Runnable periodicSaveRunnable = () -> doPeriodicSave();

    public AutoSaveManager(Context context, String draftType,
                           ContentProvider contentProvider, StatusCallback statusCallback) {
        this.draftRepository = new DraftRepository(context);
        this.draftType = draftType;
        this.contentProvider = contentProvider;
        this.statusCallback = statusCallback;
    }

    public void onContentChanged() {
        hasContentChanged.set(true);
        if (!firstSaveScheduled && !periodicSaveRunning) {
            firstSaveScheduled = true;
            handler.postDelayed(firstSaveRunnable, FIRST_SAVE_DELAY_MS);
        }
    }

    public void triggerImmediateSave() {
        if (hasContentChanged.get()) {
            doSave();
        }
    }

    private void startPeriodicSave() {
        periodicSaveRunning = true;
        handler.postDelayed(periodicSaveRunnable, PERIODIC_SAVE_INTERVAL_MS);
    }

    private void doPeriodicSave() {
        doSave();
        if (periodicSaveRunning) {
            handler.postDelayed(periodicSaveRunnable, PERIODIC_SAVE_INTERVAL_MS);
        }
    }

    private void doSave() {
        Map<String, Object> content = contentProvider.getContent();
        if (content == null || content.isEmpty() || isAllEmpty(content)) {
            return;
        }
        if (!isSaving.compareAndSet(false, true)) {
            return;
        }
        hasContentChanged.set(false);
        notifyStatus(SaveStatus.SAVING);

        disposables.add(draftRepository.saveDraft(draftType, content)
                .subscribe(
                        result -> {
                            isSaving.set(false);
                            notifyStatus(SaveStatus.SAVED);
                        },
                        error -> {
                            isSaving.set(false);
                            notifyStatus(SaveStatus.SAVE_FAILED);
                        }
                ));
    }

    public void stop() {
        firstSaveScheduled = false;
        periodicSaveRunning = false;
        handler.removeCallbacks(firstSaveRunnable);
        handler.removeCallbacks(periodicSaveRunnable);
        disposables.clear();
    }

    private void notifyStatus(SaveStatus status) {
        if (statusCallback != null) {
            handler.post(() -> statusCallback.onStatusChanged(status));
        }
    }

    private boolean isAllEmpty(Map<String, Object> content) {
        return content.values().stream().allMatch(v ->
                v == null || (v instanceof String && ((String) v).trim().isEmpty()));
    }
}