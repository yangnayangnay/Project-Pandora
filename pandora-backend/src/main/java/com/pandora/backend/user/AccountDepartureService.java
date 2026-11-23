package com.pandora.backend.user;

import com.pandora.backend.auth.SessionStore;
import com.pandora.backend.common.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Keep historical work and login identity; revoke access and derived reporting lines. */
@Service
public class AccountDepartureService {
    private final JdbcTemplate db;
    private final SessionStore sessions;
    public AccountDepartureService(JdbcTemplate db, SessionStore sessions) { this.db = db; this.sessions = sessions; }

    @Transactional
    public void depart(long actor, long target) {
        if (db.queryForList("SELECT id FROM users WHERE id=? AND role='ADMIN' AND deleted=0 AND approval_status='APPROVED'", Long.class, actor).isEmpty())
            throw new BusinessException(ErrorCode.FORBIDDEN);
        var people = db.queryForList("SELECT role FROM users WHERE id=? AND deleted=0 FOR UPDATE", target);
        if (people.isEmpty()) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if (actor == target || "ADMIN".equals(people.get(0).get("role")))
            throw new BusinessException(ErrorCode.FORBIDDEN, "管理员账号不能办理离职");
        var hierarchy = new OrganizationHierarchy(db);
        var before = hierarchy.snapshot();
        db.update("UPDATE users SET deleted=1,department_id=NULL,team_id=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=?", target);
        hierarchy.synchronize(before, target);
        db.update("DELETE FROM user_hierarchy WHERE superior_id=? OR subordinate_id=?", target, target);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { sessions.remove(target); }
            });
        } else sessions.remove(target);
    }
}
