package com.example.project_pandora.ui.task;

import com.example.project_pandora.core.util.TaskTags;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class TaskDetailFilterTest {
    private Map<String, Object> task(long id, long assignee, long dispatcher) {
        Map<String, Object> row = new HashMap<>(); row.put("id", id); row.put("name", "发布版本"); row.put("content", "完成Android联调"); row.put("assignee_id", assignee); row.put("dispatcher_id", dispatcher); row.put("status", "IN_PROGRESS"); return row;
    }
    private Map<String, Object> tag(long id, int color) { Map<String, Object> row = new HashMap<>(); row.put("id", id); row.put("name", "研发"); row.put("color_index", color); return row; }
    private List<Map<String, Object>> filter(Map<String, Object> task, TaskDetailFilter.Query query) { return TaskDetailFilter.apply(Collections.singletonList(task), 5L, query); }

    @Test public void mergeNormalizesIdsAndDeduplicatesSelfAssignedTasks() {
        Map<String, Object> self = task(1, 5, 5), other = task(2, 6, 5);
        Map<String, Object> duplicate = new HashMap<>(self); duplicate.put("id", "1");
        assertEquals(2, TaskDetailFilter.merge(Arrays.asList(self), Arrays.asList(duplicate, other)).size());
        for (int scope = 0; scope <= 2; scope++) { TaskDetailFilter.Query query = new TaskDetailFilter.Query(); query.scope = scope; assertEquals(1, filter(self, query).size()); }
    }
    @Test public void unrelatedTasksAndMissingActorAreExcluded() {
        assertTrue(filter(task(1, 8, 9), new TaskDetailFilter.Query()).isEmpty());
        assertTrue(TaskDetailFilter.apply(Arrays.asList(task(1, 5, 5)), null, new TaskDetailFilter.Query()).isEmpty());
    }
    @Test public void allFiltersAreCombinedAndSupportContentAndHashtags() {
        Map<String, Object> row = task(1, 5, 6); row.put("tags", Arrays.asList(tag(10, 3), tag(11, 1)));
        row.put("start_time", "2026-11-01 09:00"); row.put("end_time", "2026-11-05 18:00");
        TaskDetailFilter.Query query = new TaskDetailFilter.Query(); query.keyword = "android"; query.status = "IN_PROGRESS"; query.assignee = 5L; query.tag = 11L; query.from = "2026-11-03"; query.to = "2026-11-04";
        assertEquals(1, filter(row, query).size()); query.keyword = "＃研发"; assertEquals(1, filter(row, query).size());
        query.status = "COMPLETED"; assertTrue(filter(row, query).isEmpty()); query.status = "IN_PROGRESS";
        query.tag = 99L; assertTrue(filter(row, query).isEmpty()); query.tag = 11L;
        query.scope = 2; assertTrue(filter(row, query).isEmpty());
    }
    @Test public void timedMidnightDoesNotOverlapTheNextDay() {
        Map<String, Object> row = task(1, 5, 6); row.put("start_time", "2026-11-02 18:00"); row.put("end_time", "2026-11-03 00:00");
        TaskDetailFilter.Query query = new TaskDetailFilter.Query(); query.from = "2026-11-03";
        assertTrue(filter(row, query).isEmpty()); query.from = "2026-11-02"; assertEquals(1, filter(row, query).size());
    }
    @Test public void dateOnlyAndAllDayRangesRemainInclusive() {
        Map<String, Object> row = task(1, 5, 6); row.put("start_time", "2026-11-02"); row.put("end_time", "2026-11-03");
        TaskDetailFilter.Query query = new TaskDetailFilter.Query(); query.from = "2026-11-03"; query.to = "2026-11-03";
        assertEquals(1, filter(row, query).size()); row.put("start_time", "2026-11-02 00:00"); row.put("end_time", "2026-11-03 00:00"); assertEquals(1, filter(row, query).size());
    }
    @Test public void noDatesAreKeptUntilADateFilterIsSet() {
        Map<String, Object> row = task(1, 5, 6); TaskDetailFilter.Query query = new TaskDetailFilter.Query();
        assertEquals(1, filter(row, query).size()); query.to = "2026-11-03"; assertTrue(filter(row, query).isEmpty());
        row.put("end_time", "2026-11-03 12:00"); assertEquals(1, filter(row, query).size());
    }
    @Test public void firstTagAlwaysDeterminesColorAndReorderingChangesIt() {
        Map<String, Object> row = task(1, 5, 6); row.put("tags", Arrays.asList(tag(10, 3), tag(11, 1)));
        assertEquals(3, TaskTags.colorIndex(row, 8)); assertEquals(Arrays.asList(10L, 11L), TaskTags.ids(row));
        row.put("tags", Arrays.asList(tag(11, 1), tag(10, 3))); assertEquals(1, TaskTags.colorIndex(row, 8));
        row.put("tags", Collections.singletonList(tag(10, 3))); assertEquals(3, TaskTags.colorIndex(row, 8));
    }
    @Test public void noTagsAndInvalidColorsUseExistingNameFallback() {
        Map<String, Object> row = task(1, 5, 6); int fallback = ("发布版本".hashCode() & Integer.MAX_VALUE) % 8;
        assertEquals(fallback, TaskTags.colorIndex(row, 8)); row.put("tags", Collections.emptyList()); assertEquals(fallback, TaskTags.colorIndex(row, 8));
        row.put("tags", Collections.singletonList(tag(10, 99))); assertEquals(fallback, TaskTags.colorIndex(row, 8));
        row.put("tags", "bad-wire-data"); assertEquals("", TaskTags.names(row)); assertEquals(fallback, TaskTags.colorIndex(row, 8));
    }
    @Test public void uppercaseWireKeysAreSupported() {
        Map<String, Object> row = task(1, 5, 6); Map<String, Object> tag = new HashMap<>(); tag.put("ID", 10.0); tag.put("NAME", "研发"); tag.put("COLOR_INDEX", 4.0); row.put("TAGS", Collections.singletonList(tag));
        assertEquals(4, TaskTags.colorIndex(row, 8)); assertEquals("#研发", TaskTags.names(row));
    }
    @Test public void managementIsStrictlyFounderOnlyAndSearchAcceptsFullWidthHash() {
        assertTrue(TaskTags.canManage("FOUNDER")); for (String role : Arrays.asList("ADMIN", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE", null)) assertFalse(TaskTags.canManage(role));
        assertEquals("研发", TaskTags.searchText(" ＃研发 ")); assertEquals("release", TaskTags.searchText("#Release"));
    }
}
