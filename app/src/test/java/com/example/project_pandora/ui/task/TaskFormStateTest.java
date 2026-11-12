package com.example.project_pandora.ui.task;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class TaskFormStateTest {
    private TaskFormState state(String name, String body, String start, String end, boolean urgent,
                                java.util.List<Long> tags, java.util.List<Long> people) {
        return new TaskFormState(name, body, start, end, urgent, tags, people);
    }
    private TaskFormState original() {
        return state("任务", "已有正文", "2026-11-10 09:00", "2026-11-11 18:00", false,
                Arrays.asList(1L, 2L), Arrays.asList(10L, 20L));
    }
    @Test public void untouchedOrRevertedValuesDoNotNeedExitConfirmation() {
        assertEquals(original(), original());
        TaskFormState edited = state("改名", "新正文", "2026-11-10 09:00", "2026-11-11 18:00", false,
                Arrays.asList(1L, 2L), Arrays.asList(10L, 20L));
        assertNotEquals(original(), edited);
        assertEquals(original(), state("任务", "已有正文", "2026-11-10 09:00", "2026-11-11 18:00", false,
                Arrays.asList(1L, 2L), Arrays.asList(20L, 10L)));
    }
    @Test public void everyEditableFieldParticipatesInUnsavedChangeCheck() {
        assertNotEquals(original(), state("新名", "已有正文", "2026-11-10 09:00", "2026-11-11 18:00", false, Arrays.asList(1L, 2L), Arrays.asList(10L, 20L)));
        assertNotEquals(original(), state("任务", "改正文", "2026-11-10 09:00", "2026-11-11 18:00", false, Arrays.asList(1L, 2L), Arrays.asList(10L, 20L)));
        assertNotEquals(original(), state("任务", "已有正文", "2026-11-10 10:00", "2026-11-11 18:00", false, Arrays.asList(1L, 2L), Arrays.asList(10L, 20L)));
        assertNotEquals(original(), state("任务", "已有正文", "2026-11-10 09:00", "2026-11-11 19:00", false, Arrays.asList(1L, 2L), Arrays.asList(10L, 20L)));
        assertNotEquals(original(), state("任务", "已有正文", "2026-11-10 09:00", "2026-11-11 18:00", true, Arrays.asList(1L, 2L), Arrays.asList(10L, 20L)));
        assertNotEquals(original(), state("任务", "已有正文", "2026-11-10 09:00", "2026-11-11 18:00", false, Arrays.asList(2L, 1L), Arrays.asList(10L, 20L)));
        assertNotEquals(original(), state("任务", "已有正文", "2026-11-10 09:00", "2026-11-11 18:00", false, Arrays.asList(1L, 2L), Collections.singletonList(10L)));
    }
    @Test public void snapshotCopiesCollectionsAndSurvivesActivityStateSerialization() throws Exception {
        java.util.ArrayList<Long> tags = new java.util.ArrayList<>(Arrays.asList(1L, 2L));
        TaskFormState value = state("任务", "已有正文", "2026-11-10 09:00", "2026-11-11 18:00", false, tags, Arrays.asList(10L, 20L));
        tags.clear(); assertEquals(original(), value);
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bytes)) { out.writeObject(value); }
        try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals(original(), in.readObject());
        }
    }
    @Test public void creationEntryAcceptsOnlyExplicitCreationDrafts() {
        Map<String, Object> draft = new HashMap<>(); draft.put("name", "未完成内容");
        assertFalse(TaskFormState.isCreationDraft(null)); assertFalse(TaskFormState.isCreationDraft(draft));
        draft.put("editorMode", "EDIT"); assertFalse(TaskFormState.isCreationDraft(draft));
        draft.put("editorMode", "CREATE"); assertTrue(TaskFormState.isCreationDraft(draft));
    }
}
