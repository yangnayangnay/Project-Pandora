package com.pandora.backend.common;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.pandora.backend.user.OrganizationRules;

public class RoleRulesTest {
    @Test void 角色检查_ADMIN角色_预期true() { assertTrue(RoleChecker.isAdminOnly("ADMIN")); }
    @Test void 角色检查_FOUNDER角色_预期false() { assertFalse(RoleChecker.isAdminOnly("FOUNDER")); }
    @Test void 角色检查_DEPT_HEAD角色_预期false() { assertFalse(RoleChecker.isAdminOnly("DEPT_HEAD")); }
    @Test void 角色检查_null角色_预期false() { assertFalse(RoleChecker.isAdminOnly(null)); }
    @Test void 角色检查_空字符串_预期false() { assertFalse(RoleChecker.isAdminOnly("")); }
    @Test void 业务角色检查_FOUNDER_预期true() { assertTrue(OrganizationRules.isBusinessRole("FOUNDER")); }
    @Test void 业务角色检查_ADMIN_预期false() { assertFalse(OrganizationRules.isBusinessRole("ADMIN")); }
    @Test void 业务角色检查_无效角色_预期false() { assertFalse(OrganizationRules.isBusinessRole("INVALID")); }
    @Test void 部门需求检查_DEPT_HEAD需要部门_预期true() { assertTrue(OrganizationRules.hasDepartment("DEPT_HEAD")); }
    @Test void 团队需求检查_EMPLOYEE需要团队_预期true() { assertTrue(OrganizationRules.hasTeam("EMPLOYEE")); }
    @Test void 部门需求检查_FOUNDER不需要部门_预期false() { assertFalse(OrganizationRules.hasDepartment("FOUNDER")); }
}