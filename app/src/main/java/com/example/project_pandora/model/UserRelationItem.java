package com.example.project_pandora.model;

import com.example.project_pandora.core.util.PermissionLevel;

public class UserRelationItem {

    private Long id;
    private String realName;
    private String role;
    private int permissionLevel;
    private String departmentName;
    private String teamName;
    private int depth;

    public UserRelationItem(Long id, String realName, String role, String departmentName, String teamName) {
        this.id = id;
        this.realName = realName;
        this.role = role;
        this.permissionLevel = PermissionLevel.of(role);
        this.departmentName = departmentName;
        this.teamName = teamName;
        this.depth = 0;
    }

    public UserRelationItem(Long id, String realName, String role, String departmentName, String teamName, int depth) {
        this.id = id;
        this.realName = realName;
        this.role = role;
        this.permissionLevel = PermissionLevel.of(role);
        this.departmentName = departmentName;
        this.teamName = teamName;
        this.depth = depth;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}