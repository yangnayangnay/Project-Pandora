package com.pandora.backend.audit;

import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.FileSystemResource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminOperationAuditTest {
    JdbcTemplate db; AuditService audit;
    @BeforeEach void setup() {
        var source=new DriverManagerDataSource("jdbc:h2:mem:audit-"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        new ResourceDatabasePopulator(new FileSystemResource("src/main/resources/db/h2_schema.sql")).execute(source);
        db=new JdbcTemplate(source); audit=new AuditService(db);
        for(String type:List.of("USER_CREATE","USER_DEPARTURE","USER_HANDOVER","USER_RESTORE","DEPARTMENT_UPDATE","TASK_CREATE","LOGIN"))
            audit.record(1L,"test","USER",2L,"详情",type,"王八测试"+type);
    }
    @Test void defaultListAndExportIncludeAdminOperationsWithTheSameFilter() {
        assertEquals(6L,audit.queryAuditLogs(1,20,null).get("total"));
        assertEquals(6,audit.exportAuditLogs(null,null,null,null,null).size());
    }
    @Test void handoverFilteringAndKeywordSearchWorkForListAndExport() {
        assertEquals(1L,audit.queryAuditLogs(1,20,null,null,null,"USER_HANDOVER","王八").get("total"));
        assertEquals(1,audit.exportAuditLogs(null,null,null,"USER_HANDOVER","王八").size());
        assertEquals("离职工作交接",AuditLabelMapper.toChineseOperationType("USER_HANDOVER"));
    }
}
