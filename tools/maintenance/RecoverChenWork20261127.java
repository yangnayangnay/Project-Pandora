import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import com.pandora.backend.user.OrganizationHierarchy;

/** Explicitly approved recovery from archived 000011 to active 000020. No name-based matching. */
public class RecoverChenWork20261127 {
    public static void main(String[] args) throws Exception {
        if(args.length!=4 || !args[0].contains("IFEXISTS=TRUE")) throw new IllegalArgumentException("existing URL,user,password,new-backup.zip required");
        try(Connection c=DriverManager.getConnection(args[0],args[1],args[2])) {
            var db=new JdbcTemplate(new SingleConnectionDataSource(c,true));
            Path backup=Path.of(args[3]);
            if(Files.exists(backup)) throw new IllegalStateException("Backup already exists");
            try(var statement=c.createStatement()) { statement.execute("BACKUP TO '"+backup.toString().replace("'","''")+"'"); }
            c.setAutoCommit(false);
            try {
                var users=db.queryForList("SELECT id,username,password_hash,real_name,role,department_id,team_id,deleted FROM users ORDER BY id FOR UPDATE");
                var old=db.queryForMap("SELECT username,real_name,deleted FROM users WHERE id=104");
                var next=db.queryForMap("SELECT username,real_name,deleted FROM users WHERE id=115");
                if(!"000011".equals(old.get("username")) || !"陈十".equals(old.get("real_name")) || ((Number)old.get("deleted")).intValue()!=1
                        || !"000020".equals(next.get("username")) || !"陈十".equals(next.get("real_name")) || ((Number)next.get("deleted")).intValue()!=0)
                    throw new IllegalStateException("Account identities changed; refusing automatic recovery");
                int tasks=db.queryForObject("SELECT COUNT(*) FROM tasks WHERE assignee_id=104 AND deleted=0",Integer.class);
                int logs=db.queryForObject("SELECT COUNT(*) FROM work_logs WHERE user_id=104 AND deleted=0",Integer.class);
                if(tasks==0 && logs==0) {
                    if(db.queryForObject("SELECT COUNT(*) FROM users WHERE id=115 AND role='DEPT_HEAD' AND department_id=2 AND team_id IS NULL",Integer.class)!=1)
                        throw new IllegalStateException("Existing recovery has a different post");
                    System.out.println("ALREADY_RECOVERED"); c.rollback(); return;
                }
                if(tasks!=16 || logs!=25) throw new IllegalStateException("Historical work changed; inspect before recovery");
                if(db.queryForObject("SELECT COUNT(*) FROM work_logs active JOIN work_logs old ON old.user_id=104 AND active.user_id=115 AND active.log_date=old.log_date",Integer.class)>0)
                    throw new IllegalStateException("Conflicting log dates; no data changed");
                if(db.queryForObject("SELECT COUNT(*) FROM tasks WHERE assignee_id=115",Integer.class)>0)
                    throw new IllegalStateException("New account already has tasks; inspect first");
                if(db.queryForObject("SELECT COUNT(*) FROM departments WHERE id=2 AND name='产品部' AND deleted=0",Integer.class)!=1)
                    throw new IllegalStateException("Product department changed");
                var hierarchy=new OrganizationHierarchy(db); var before=hierarchy.snapshot();
                var affected=db.queryForList("SELECT id FROM tasks WHERE assignee_id=104 OR dispatcher_id=104 OR EXISTS(SELECT 1 FROM task_assignees a WHERE a.task_id=tasks.id AND a.user_id=104) ORDER BY id FOR UPDATE",Long.class);
                for(long task:affected) {
                    boolean receiverExists=db.queryForObject("SELECT COUNT(*) FROM task_assignees WHERE task_id=? AND user_id=115",Integer.class,task)>0;
                    if(receiverExists) db.update("DELETE FROM task_assignees WHERE task_id=? AND user_id=104",task);
                    else db.update("UPDATE task_assignees SET user_id=115 WHERE task_id=? AND user_id=104",task);
                    db.update("UPDATE tasks SET assignee_id=CASE WHEN assignee_id=104 THEN 115 ELSE assignee_id END,dispatcher_id=CASE WHEN dispatcher_id=104 THEN 115 ELSE dispatcher_id END,revision=revision+1 WHERE id=?",task);
                }
                db.update("UPDATE notifications SET target_user_id=115 WHERE target_user_id=104 AND log_id IN (SELECT id FROM work_logs WHERE user_id=104)");
                db.update("UPDATE work_logs SET user_id=115 WHERE user_id=104");
                db.update("UPDATE users SET role='DEPT_HEAD',department_id=2,team_id=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=115");
                hierarchy.synchronize(before,115);
                if(db.queryForObject("SELECT COUNT(*) FROM tasks WHERE assignee_id=115 AND deleted=0",Integer.class)!=16
                        || db.queryForObject("SELECT COUNT(*) FROM work_logs WHERE user_id=115 AND deleted=0",Integer.class)!=25)
                    throw new IllegalStateException("Work readback mismatch");
                for(var previous:users) {
                    long id=((Number)previous.get("id")).longValue();
                    var current=db.queryForMap("SELECT id,username,password_hash,real_name,role,department_id,team_id,deleted FROM users WHERE id=?",id);
                    for(var entry:previous.entrySet()) {
                        if(id==115 && Set.of("ROLE","DEPARTMENT_ID","TEAM_ID").contains(entry.getKey().toUpperCase(Locale.ROOT))) continue;
                        if(!Objects.equals(entry.getValue(),current.get(entry.getKey()))) throw new IllegalStateException("Unexpected account change");
                    }
                }
                c.commit();
                System.out.println("RECOVERED old=000011 current=000020 tasks=16 logs=25 department=产品部 passwords_and_profiles_unchanged=true");
            } catch(Exception error) {c.rollback();throw error;}
        }
    }
}
