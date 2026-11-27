package com.pandora.backend.user;

import com.pandora.backend.auth.SessionStore;
import com.pandora.backend.common.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;
import java.math.BigDecimal;

/** Keep historical work and login identity; revoke access and derived reporting lines. */
@Service
public class AccountDepartureService {
    private final JdbcTemplate db;
    private final SessionStore sessions;
    public AccountDepartureService(JdbcTemplate db, SessionStore sessions) { this.db = db; this.sessions = sessions; }

    @Transactional
    public void depart(long actor, long target) {
        depart(actor,target,Map.of());
    }

    private static final String OPEN_WORK = "SELECT t.id,t.name,t.assignee_id,t.dispatcher_id,t.revision FROM tasks t "
            + "WHERE t.deleted=0 AND t.status<>'COMPLETED' AND (t.assignee_id=? OR t.dispatcher_id=? "
            + "OR EXISTS(SELECT 1 FROM task_assignees a WHERE a.task_id=t.id AND a.user_id=?)) ORDER BY t.id";

    public Map<String,Object> preview(long actor,long target) {
        requireAdmin(actor);
        var person=person(target,false);
        if("ADMIN".equals(person.get("role"))) throw new BusinessException(ErrorCode.FORBIDDEN,"管理员账号不能办理离职");
        var candidates=db.queryForList("SELECT u.id,u.username,u.real_name,u.role,u.department_id,u.team_id,d.name AS department_name,t.name AS team_name "
                + "FROM users u LEFT JOIN departments d ON d.id=u.department_id AND d.deleted=0 LEFT JOIN teams t ON t.id=u.team_id AND t.deleted=0 "
                + "WHERE u.deleted=0 AND u.approval_status='APPROVED' AND u.role IN ('FOUNDER','DEPT_HEAD','TEAM_LEADER','EMPLOYEE') AND u.id<>? ORDER BY u.id",target);
        return Map.of("person",person,"positionRequired",!"EMPLOYEE".equals(person.get("role")),
                "positionCandidates",candidates.stream().filter(p->canTakePosition(person,p)).toList(),
                "taskCandidates",candidates,"tasks",db.queryForList(OPEN_WORK,target,target,target));
    }

    @Transactional
    public void depart(long actor,long target,Map<String,Object> request) {
        requireAdmin(actor);
        if(!Set.of("positionSuccessorId","taskSuccessorId","expectedRole","expectedDepartmentId","expectedTeamId").containsAll(request.keySet()))
            throw new BusinessException(ErrorCode.BAD_REQUEST,"不支持的离职交接字段");
        // Lock recipients as well as the leaver so concurrent handovers cannot reuse a departing recipient.
        db.queryForList("SELECT id FROM users WHERE deleted=0 ORDER BY id FOR UPDATE",Long.class);
        var person=person(target,true);
        if(actor==target || "ADMIN".equals(person.get("role"))) throw new BusinessException(ErrorCode.FORBIDDEN,"管理员账号不能办理离职");
        if(request.containsKey("expectedRole") && (!Objects.equals(person.get("role"),request.get("expectedRole"))
                || !sameId(person.get("department_id"),request.get("expectedDepartmentId")) || !sameId(person.get("team_id"),request.get("expectedTeamId"))))
            throw new BusinessException(ErrorCode.CONFLICT,"人员岗位已变更，请重新打开离职交接窗口");
        boolean positionRequired=!"EMPLOYEE".equals(person.get("role"));
        Long positionId=optionalId(request.get("positionSuccessorId"));
        Long taskId=optionalId(request.get("taskSuccessorId"));
        Map<String,Object> successor=null;
        if(positionRequired) {
            if(positionId==null) throw new BusinessException(ErrorCode.BAD_REQUEST,"请选择岗位交接人后再办理离职");
            successor=person(positionId,true);
            if(positionId==target || !canTakePosition(person,successor)) throw new BusinessException(ErrorCode.BAD_REQUEST,"岗位交接人必须是相应组织内符合职级的在职人员");
        } else if(positionId!=null) throw new BusinessException(ErrorCode.BAD_REQUEST,"员工无需交接管理岗位");
        var work=db.queryForList(OPEN_WORK+" FOR UPDATE",target,target,target);
        if(!work.isEmpty()) {
            if(taskId==null) throw new BusinessException(ErrorCode.BAD_REQUEST,"存在未完成任务，请选择任务交接人");
            var receiver=person(taskId,true);
            if(taskId==target || !OrganizationRules.isBusinessRole(Objects.toString(receiver.get("role"),"")) || !"APPROVED".equals(receiver.get("approval_status")))
                throw new BusinessException(ErrorCode.BAD_REQUEST,"任务交接人必须是其他已批准的在职人员");
        }
        var hierarchy = new OrganizationHierarchy(db);
        var before = hierarchy.snapshot();
        if(successor!=null) {
            db.update("UPDATE users SET role=?,department_id=?,team_id=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                    person.get("role"),OrganizationRules.hasDepartment(person.get("role").toString())?person.get("department_id"):null,
                    OrganizationRules.hasTeam(person.get("role").toString())?person.get("team_id"):null,positionId);
            if("DEPT_HEAD".equals(person.get("role"))) db.update("UPDATE departments SET head_user_id=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",positionId,person.get("department_id"));
            if("TEAM_LEADER".equals(person.get("role"))) db.update("UPDATE teams SET leader_user_id=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",positionId,person.get("team_id"));
        }
        for(var task:work) {
            long workId=((Number)task.get("id")).longValue();
            // Preserve every other collaborator, including when the selected receiver is already on the task.
            var assignees=new LinkedHashSet<>(db.queryForList("SELECT user_id FROM task_assignees WHERE task_id=? ORDER BY user_id FOR UPDATE",Long.class,workId));
            boolean executing=sameId(task.get("assignee_id"),target) || assignees.contains(target);
            if(executing) {
                boolean alreadyAssigned=assignees.contains(taskId) || sameId(task.get("assignee_id"),taskId);
                if(assignees.isEmpty() && task.get("assignee_id") instanceof Number number) assignees.add(number.longValue());
                assignees.remove(target); assignees.add(taskId);
                db.update("DELETE FROM task_assignees WHERE task_id=? AND user_id=?",workId,target);
                for(long receiver:assignees) if(db.queryForObject("SELECT COUNT(*) FROM task_assignees WHERE task_id=? AND user_id=?",Integer.class,workId,receiver)==0)
                    db.update("INSERT INTO task_assignees(task_id,user_id) VALUES(?,?)",workId,receiver);
                if(!alreadyAssigned) new com.pandora.backend.notification.NotificationService(db).recordEvent("NEW_TASK",workId,taskId,"离职交接任务："+task.get("name"));
            }
            db.update("UPDATE tasks SET assignee_id=?,dispatcher_id=?,revision=revision+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                    sameId(task.get("assignee_id"),target)?taskId:task.get("assignee_id"),
                    sameId(task.get("dispatcher_id"),target)?taskId:task.get("dispatcher_id"),workId);
        }
        finishDeparture(target,hierarchy,before);
    }

    private void requireAdmin(long actor) {
        if (db.queryForList("SELECT id FROM users WHERE id=? AND role='ADMIN' AND deleted=0 AND approval_status='APPROVED'", Long.class, actor).isEmpty())
            throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    private Map<String,Object> person(long id,boolean lock) {
        var rows=db.queryForList("SELECT id,username,real_name,role,department_id,team_id,approval_status FROM users WHERE id=? AND deleted=0"+(lock?" FOR UPDATE":""),id);
        if(rows.isEmpty()) throw new BusinessException(ErrorCode.USER_NOT_FOUND,"所选人员已离职或不存在，请刷新后重试");
        return rows.get(0);
    }
    private boolean canTakePosition(Map<String,Object> outgoing,Map<String,Object> next) {
        String role=Objects.toString(outgoing.get("role"),""), candidate=Objects.toString(next.get("role"),"");
        if(!"APPROVED".equals(next.get("approval_status")) && next.containsKey("approval_status")) return false;
        if(!OrganizationRules.isBusinessRole(candidate) || rank(candidate)>rank(role)) return false;
        // Do not silently vacate another management post when taking over this one.
        if(!"EMPLOYEE".equals(candidate) && !role.equals(candidate)) return false;
        if(!"FOUNDER".equals(role) && !sameId(outgoing.get("department_id"),next.get("department_id"))) return false;
        if("TEAM_LEADER".equals(role) && !sameId(outgoing.get("team_id"),next.get("team_id"))) return false;
        return !"EMPLOYEE".equals(role);
    }
    private static int rank(String role) { return switch(role) {case "FOUNDER"->4;case "DEPT_HEAD"->3;case "TEAM_LEADER"->2;case "EMPLOYEE"->1;default->0;}; }
    private static Long optionalId(Object raw) {
        if(raw==null || "".equals(raw)) return null;
        try { long result=new BigDecimal(raw.toString()).longValueExact(); if(result<=0) throw new ArithmeticException(); return result; }
        catch(RuntimeException error) { throw new BusinessException(ErrorCode.BAD_REQUEST,"交接人员编号格式错误"); }
    }
    private static boolean sameId(Object a,Object b) { return Objects.equals(optionalId(a),optionalId(b)); }
    private void finishDeparture(long target,OrganizationHierarchy hierarchy,java.util.List<Map<String,Object>> before) {
        // Retain the last post for explicit recovery of a mistaken departure.
        db.update("UPDATE users SET deleted=1,updated_at=CURRENT_TIMESTAMP WHERE id=?", target);
        hierarchy.synchronize(before, target);
        db.update("DELETE FROM user_hierarchy WHERE superior_id=? OR subordinate_id=?", target, target);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { sessions.remove(target); }
            });
        } else sessions.remove(target);
    }
}
