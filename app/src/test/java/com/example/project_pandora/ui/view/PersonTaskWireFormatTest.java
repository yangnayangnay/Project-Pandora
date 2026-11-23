package com.example.project_pandora.ui.view;

import com.example.project_pandora.core.util.TaskTags;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;

public class PersonTaskWireFormatTest {
    @Test public void directoryUppercaseTaskKeepsItsNameStatusAndDeadline() {
        Map<String,Object> row = new Gson().fromJson("{\"ID\":91,\"NAME\":\"十一月演示彩排\",\"STATUS\":\"COMPLETED\",\"END_TIME\":\"2026-11-23T18:00:00\"}",
                new TypeToken<Map<String,Object>>(){}.getType());
        assertEquals("十一月演示彩排", TaskTags.title(row));
        assertEquals("COMPLETED", TaskTags.value(row, "status"));
        assertEquals("2026-11-23T18:00:00", TaskTags.value(row, "end_time"));
        assertEquals("个人任务", TaskTags.title(Map.of("name", "个人任务")));
    }
    @Test public void absentOrBlankNamesUseTheFallback() {
        assertEquals("未命名任务", TaskTags.title(null));
        assertEquals("未命名任务", TaskTags.title(Map.of("NAME", " ")));
    }
}
