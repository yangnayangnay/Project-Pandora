import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandora.backend.user.AccountCreationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Explicit one-off demo enrichment. Never called during application startup. */
public class EnrichNovemberDemo {
    static final ObjectMapper JSON=new ObjectMapper();
    static final Set<String> WRITABLE=Set.of("USERS","USER_HIERARCHY","TASKS","TASK_ASSIGNEES","TASK_TAGS","TASK_TAG_LINKS","WORK_LOGS","LOG_COMMENTS");
    static long id(Object value) { return ((Number)value).longValue(); }
    static String text(Object value) { return Objects.toString(value,""); }
    @SuppressWarnings("unchecked") static List<Map<String,Object>> list(Map<String,Object> p,String key) { return (List<Map<String,Object>>)p.get(key); }
    static String sha(String value) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }

    public static void main(String[] args) throws Exception {
        if(args.length<5 || !args[0].contains("IFEXISTS=TRUE")) throw new IllegalArgumentException("Existing DB URL, user, password, plan, preview|apply [backup,receipt] required");
        Map<String,Object> plan=JSON.readValue(Files.readString(Path.of(args[3])),Map.class);
        if(!"pandora-demo-20261101-1123-v1".equals(plan.get("batch"))) throw new IllegalArgumentException("Unexpected demo plan");
        try(Connection connection=DriverManager.getConnection(args[0],args[1],args[2])) {
            JdbcTemplate db=new JdbcTemplate(new SingleConnectionDataSource(connection,true));
            verifyTargets(db,plan);
            if(args[4].equals("preview")) {
                Map<String,Object> result=new LinkedHashMap<>();
                result.put("mode","preview"); result.put("batch",plan.get("batch"));
                result.put("teams",teams(db)); result.put("focus",plan.get("focus"));
                result.put("planned_tasks",list(plan,"tasks").size()); result.put("planned_logs_before_existing_date_check",list(plan,"logs").size());
                System.out.println("RESULT_JSON="+JSON.writeValueAsString(result)); return;
            }
            if(!args[4].equals("apply") || args.length!=7) throw new IllegalArgumentException("apply requires backup and receipt paths");
            Path backup=Path.of(args[5]).toAbsolutePath();
            if(Files.exists(backup)) throw new IllegalStateException("Refusing to overwrite backup");
            Files.createDirectories(backup.getParent());
            try(var statement=connection.createStatement()) { statement.execute("BACKUP TO '"+backup.toString().replace("'","''")+"'"); }
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            try {
                db.queryForList("SELECT id FROM users ORDER BY id FOR UPDATE");
                verifyTargets(db,plan);
                var before=snapshot(connection);
                Map<String,Long> newIds=new HashMap<>();
                List<Map<String,Object>> accounts=new ArrayList<>();
                int createdUsers=0,createdTasks=0,createdLogs=0,createdComments=0,createdTags=0;
                String demoPassword=System.getenv("PANDORA_DEMO_PASSWORD");
                if(demoPassword==null || demoPassword.length()<6 || demoPassword.length()>16) throw new IllegalStateException("PANDORA_DEMO_PASSWORD must have 6 to 16 characters");
                var accountService=new AccountCreationService(db,new BCryptPasswordEncoder());
                for(var employee:list(plan,"newEmployees")) {
                    long team=id(employee.get("teamId")),department=id(employee.get("departmentId"));
                    var existing=db.queryForList("SELECT id,username,real_name,role,department_id,team_id,deleted FROM users WHERE LOWER(email)=LOWER(?)",employee.get("email"));
                    long person; String username; boolean created=false;
                    if(!existing.isEmpty()) {
                        var row=existing.get(0);
                        if(existing.size()!=1 || !employee.get("name").equals(row.get("real_name")) || !"EMPLOYEE".equals(row.get("role"))
                                || id(row.get("department_id"))!=department || id(row.get("team_id"))!=team || id(row.get("deleted"))!=0)
                            throw new IllegalStateException("Demo email is occupied by an unrelated account");
                        person=id(row.get("id")); username=text(row.get("username"));
                    } else {
                        if(employeeCount(db,team)>=2) continue;
                        Map<String,Object> request=new HashMap<>();
                        request.putAll(employee); request.keySet().retainAll(Set.of("email","phone","birthday","idCard","departmentId","teamId"));
                        request.put("realName",employee.get("name")); request.put("role","EMPLOYEE"); request.put("password",demoPassword);
                        var result=accountService.create(request); person=id(result.get("id")); username=text(result.get("employeeNo")); created=true; createdUsers++;
                    }
                    newIds.put(text(employee.get("key")),person);
                    accounts.add(Map.of("id",person,"employeeNo",username,"name",employee.get("name"),"departmentId",department,"teamId",team,"created",created));
                }
                Map<String,Long> tagIds=new HashMap<>();
                for(var tag:list(plan,"tags")) {
                    var found=db.queryForList("SELECT id FROM task_tags WHERE LOWER(name)=LOWER(?) AND deleted=0 ORDER BY id",Long.class,tag.get("name"));
                    long tagId;
                    if(found.isEmpty()) {
                        tagId=insert(connection,"INSERT INTO task_tags(name,name_key,color_index,deleted) VALUES(?,?,?,0)",tag.get("name"),text(tag.get("name")).toLowerCase(Locale.ROOT),tag.get("color"));
                        createdTags++;
                    } else tagId=found.get(0);
                    tagIds.put(text(tag.get("name")),tagId);
                }
                for(var task:list(plan,"tasks")) {
                    Long person=resolve(task.get("assignee"),newIds); if(person==null) continue;
                    var existing=db.queryForList("SELECT id FROM tasks WHERE assignee_id=? AND name=? AND content LIKE '【11月演示】%'",Long.class,person,task.get("name"));
                    if(!existing.isEmpty()) continue;
                    Timestamp start=Timestamp.valueOf(text(task.get("start"))),end=Timestamp.valueOf(text(task.get("end")));
                    if(!end.after(start) || start.toLocalDateTime().toLocalDate().isBefore(LocalDate.of(2026,11,1)) || end.toLocalDateTime().toLocalDate().isAfter(LocalDate.of(2026,11,23)))
                        throw new IllegalStateException("Invalid demo task date range");
                    long taskId=insert(connection,"INSERT INTO tasks(name,content,status,assignee_id,dispatcher_id,is_urgent,start_time,end_time,progress_note,created_at,updated_at,deleted) VALUES(?,?,?,?,?,?,?,?,?,?,?,0)",
                            task.get("name"),task.get("content"),task.get("status"),person,task.get("dispatcher"),Boolean.TRUE.equals(task.get("urgent"))?1:0,start,end,
                            task.get("progress"),Timestamp.valueOf(text(task.get("created"))),Timestamp.valueOf(text(task.get("updated"))));
                    for(Object value:(List<?>)task.get("assignees")) {
                        Long colleague=resolve(value,newIds);
                        if(colleague==null || db.queryForObject("SELECT COUNT(*) FROM users WHERE id=? AND role<>'ADMIN' AND deleted=0 AND approval_status='APPROVED'",Integer.class,colleague)!=1)
                            throw new IllegalStateException("Invalid task collaborator");
                        db.update("INSERT INTO task_assignees(task_id,user_id,assigned_at) VALUES(?,?,?)",taskId,colleague,Timestamp.valueOf(text(task.get("created"))));
                    }
                    int order=0;
                    for(Object tag:new LinkedHashSet<>((List<?>)task.get("tags")))
                        db.update("INSERT INTO task_tag_links(task_id,tag_id,sort_order) VALUES(?,?,?)",taskId,tagIds.get(text(tag)),order++);
                    createdTasks++;
                }
                for(var log:list(plan,"logs")) {
                    Long person=resolve(log.get("user"),newIds); if(person==null) continue;
                    if(db.queryForObject("SELECT COUNT(*) FROM work_logs WHERE user_id=? AND log_date=?",Integer.class,person,log.get("date"))>0) continue;
                    LocalDate date=LocalDate.parse(text(log.get("date")));
                    insert(connection,"INSERT INTO work_logs(user_id,work_item,work_content,completion_status,time_cost,log_date,created_at,updated_at,deleted) VALUES(?,?,?,?,?,?,?,?,0)",
                            person,date.getMonthValue()+"月"+date.getDayOfMonth()+"日日志",log.get("body"),log.get("status"),log.get("hours"),java.sql.Date.valueOf(date),
                            Timestamp.valueOf(date.atTime(17,30)),Timestamp.valueOf(date.atTime(17,45)));
                    createdLogs++;
                }
                for(var comment:list(plan,"comments")) {
                    Long person=resolve(comment.get("user"),newIds); if(person==null) continue;
                    var rows=db.queryForList("SELECT id FROM work_logs WHERE user_id=? AND log_date=? AND deleted=0 AND work_content LIKE '【11月演示】%'",Long.class,person,comment.get("date"));
                    if(rows.isEmpty()) continue;
                    long logId=rows.get(0),author=id(comment.get("author"));
                    if(db.queryForObject("SELECT COUNT(*) FROM user_hierarchy WHERE superior_id=? AND subordinate_id=?",Integer.class,author,person)!=1)
                        throw new IllegalStateException("Comment author is not the direct superior");
                    if(db.queryForObject("SELECT COUNT(*) FROM log_comments WHERE log_id=? AND author_id=? AND content=?",Integer.class,logId,author,comment.get("content"))>0) continue;
                    insert(connection,"INSERT INTO log_comments(log_id,author_id,content,created_at) VALUES(?,?,?,?)",logId,author,comment.get("content"),Timestamp.valueOf(text(comment.get("date"))+" 18:10:00"));
                    createdComments++;
                }
                verifyPreserved(before,snapshot(connection));
                List<Map<String,Object>> teamSummary=teams(db);
                for(var team:teamSummary) if(id(team.get("employee_count"))<2) throw new IllegalStateException("Team still has fewer than two employees");
                List<Map<String,Object>> focusSummary=new ArrayList<>();
                for(var focus:list(plan,"focus")) {
                    long person=id(focus.get("id"));
                    int logs=db.queryForObject("SELECT COUNT(*) FROM work_logs WHERE user_id=? AND deleted=0 AND log_date BETWEEN '2026-11-01' AND '2026-11-23'",Integer.class,person);
                    int logDays=db.queryForObject("SELECT COUNT(DISTINCT log_date) FROM work_logs WHERE user_id=? AND deleted=0 AND log_date BETWEEN '2026-11-01' AND '2026-11-23'",Integer.class,person);
                    int reservedDeletedDates=db.queryForObject("SELECT COUNT(DISTINCT l.log_date) FROM work_logs l WHERE l.user_id=? AND l.deleted<>0 AND l.log_date BETWEEN '2026-11-01' AND '2026-11-23' AND NOT EXISTS (SELECT 1 FROM work_logs active WHERE active.user_id=l.user_id AND active.log_date=l.log_date AND active.deleted=0)",Integer.class,person);
                    if(logDays!=23-reservedDeletedDates) throw new IllegalStateException("Focus user "+focus.get("name")+" has "+logDays+" active log dates; expected "+(23-reservedDeletedDates)+" after preserving deleted dates");
                    var summary=db.queryForMap("SELECT COUNT(*) AS tasks,SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) AS completed,SUM(CASE WHEN status='IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress,SUM(CASE WHEN is_urgent=1 THEN 1 ELSE 0 END) AS urgent FROM tasks WHERE assignee_id=? AND deleted=0 AND start_time<'2026-11-24' AND end_time>='2026-11-01'",person);
                    if(id(summary.get("tasks"))<16 || id(summary.get("completed"))<1 || id(summary.get("in_progress"))<1 || id(summary.get("urgent"))<1)
                        throw new IllegalStateException("Focus task variety is insufficient");
                    Map<String,Object> row=new LinkedHashMap<>(); summary.forEach((k,v)->row.put(k.toLowerCase(Locale.ROOT),v));
                    row.putAll(focus); row.put("logs",logs); row.put("logDays",logDays); row.put("preservedDeletedDates",reservedDeletedDates); focusSummary.add(row);
                }
                if(db.queryForObject("SELECT COUNT(*) FROM user_hierarchy h JOIN users s ON s.id=h.superior_id JOIN users u ON u.id=h.subordinate_id WHERE s.role='ADMIN' OR u.role IN ('ADMIN','FOUNDER')",Integer.class)!=0)
                    throw new IllegalStateException("Invalid organization links");
                Map<String,Object> receipt=new LinkedHashMap<>();
                receipt.put("batch",plan.get("batch")); receipt.put("added",Map.of("employees",createdUsers,"tasks",createdTasks,"logs",createdLogs,"comments",createdComments,"tags",createdTags));
                receipt.put("teams",teamSummary); receipt.put("focus",focusSummary); receipt.put("demoAccounts",accounts);
                receipt.put("existing_rows_and_passwords_unchanged",true); receipt.put("dateFrom",plan.get("dateFrom")); receipt.put("dateTo",plan.get("dateTo"));
                connection.commit();
                Files.writeString(Path.of(args[6]),JSON.writerWithDefaultPrettyPrinter().writeValueAsString(receipt));
                System.out.println("RESULT_JSON="+JSON.writeValueAsString(receipt));
            } catch(Exception error) { connection.rollback(); throw error; }
        }
    }

    static Long resolve(Object value,Map<String,Long> ids) { return value instanceof Number?id(value):ids.get(text(value)); }
    static int employeeCount(JdbcTemplate db,long team) {
        return db.queryForObject("SELECT COUNT(*) FROM users u JOIN teams t ON t.id=u.team_id AND t.department_id=u.department_id WHERE t.id=? AND u.role='EMPLOYEE' AND u.deleted=0 AND u.approval_status='APPROVED'",Integer.class,team);
    }
    static List<Map<String,Object>> teams(JdbcTemplate db) {
        var rows=db.queryForList("SELECT t.id,t.name,t.department_id,d.name AS department_name,COUNT(u.id) AS employee_count FROM teams t JOIN departments d ON d.id=t.department_id LEFT JOIN users u ON u.team_id=t.id AND u.department_id=t.department_id AND u.role='EMPLOYEE' AND u.deleted=0 AND u.approval_status='APPROVED' WHERE t.deleted=0 AND d.deleted=0 GROUP BY t.id,t.name,t.department_id,d.name ORDER BY t.id");
        List<Map<String,Object>> result=new ArrayList<>();
        for(var row:rows) { Map<String,Object> normalized=new LinkedHashMap<>(); row.forEach((k,v)->normalized.put(k.toLowerCase(Locale.ROOT),v)); result.add(normalized); }
        return result;
    }
    static void verifyTargets(JdbcTemplate db,Map<String,Object> plan) {
        for(var focus:list(plan,"focus")) {
            var row=db.queryForMap("SELECT real_name,role,deleted,approval_status FROM users WHERE id=?",focus.get("id"));
            if(!focus.get("name").equals(row.get("real_name")) || !focus.get("role").equals(row.get("role")) || id(row.get("deleted"))!=0 || !"APPROVED".equals(row.get("approval_status")))
                throw new IllegalStateException("Focus person or post changed; review plan first");
        }
        Set<Long> expected=Set.of(1L,2L,3L,4L);
        var actual=new HashSet<>(db.queryForList("SELECT id FROM teams WHERE deleted=0",Long.class));
        if(!actual.equals(expected)) throw new IllegalStateException("Team scope changed; review plan first");
    }
    static long insert(Connection db,String sql,Object...values) throws SQLException {
        try(var statement=db.prepareStatement(sql,new String[]{"id"})) {
            for(int i=0;i<values.length;i++) statement.setObject(i+1,values[i]);
            if(statement.executeUpdate()!=1) throw new IllegalStateException("Insert count mismatch");
            try(var key=statement.getGeneratedKeys()) { if(!key.next()) throw new IllegalStateException("Missing inserted ID"); return key.getLong(1); }
        }
    }
    static Map<String,Map<String,String>> snapshot(Connection db) throws Exception {
        Map<String,Map<String,String>> result=new TreeMap<>();
        List<String> tables=new ArrayList<>();
        try(var rows=db.createStatement().executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME")) {
            while(rows.next()) tables.add(rows.getString(1));
        }
        for(String table:tables) {
            Map<Integer,String> primary=new TreeMap<>();
            try(var keys=db.getMetaData().getPrimaryKeys(db.getCatalog(),"PUBLIC",table)) { while(keys.next()) primary.put(keys.getInt("KEY_SEQ"),keys.getString("COLUMN_NAME")); }
            if(primary.isEmpty()) throw new IllegalStateException("Snapshot requires a primary key: "+table);
            Map<String,String> values=new TreeMap<>();
            try(var rows=db.createStatement().executeQuery("SELECT * FROM \""+table+"\"")) {
                var meta=rows.getMetaData();
                while(rows.next()) {
                    StringBuilder body=new StringBuilder(),key=new StringBuilder();
                    for(int i=1;i<=meta.getColumnCount();i++) { String v=rows.getString(i); body.append(meta.getColumnName(i)).append(':').append(v==null?-1:v.length()).append(':').append(v).append(';'); }
                    for(String column:primary.values()) key.append(column).append('=').append(rows.getString(column)).append(';');
                    values.put(key.toString(),sha(body.toString()));
                }
            }
            result.put(table,values);
        }
        return result;
    }
    static void verifyPreserved(Map<String,Map<String,String>> before,Map<String,Map<String,String>> after) {
        if(!before.keySet().equals(after.keySet())) throw new IllegalStateException("Unexpected schema change");
        for(String table:before.keySet()) {
            for(var row:before.get(table).entrySet()) if(!Objects.equals(row.getValue(),after.get(table).get(row.getKey())))
                throw new IllegalStateException("Existing record changed: "+table+" "+row.getKey());
            if(!WRITABLE.contains(table) && !before.get(table).equals(after.get(table))) throw new IllegalStateException("Unexpected write to "+table);
        }
    }
}
