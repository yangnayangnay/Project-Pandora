package com.example.project_pandora.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class User implements Serializable {

    @SerializedName("id")
    private Long id;

    @SerializedName("username")
    private String username;

    @SerializedName("role")
    private String role;

    @SerializedName("department_id")
    private Long departmentId;

    @SerializedName("team_id")
    private Long teamId;

    @SerializedName("avatar")
    private String avatar;

    @SerializedName("theme_mode")
    private String themeMode;

    @SerializedName("manual_theme")
    private String manualTheme;

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getThemeMode() { return themeMode; }
    public void setThemeMode(String themeMode) { this.themeMode = themeMode; }
    public String getManualTheme() { return manualTheme; }
    public void setManualTheme(String manualTheme) { this.manualTheme = manualTheme; }

    public String getRoleDisplayName() {
        if (role == null) return "员工";
        switch (role) {
            case "ADMIN": return "管理员";
            case "FOUNDER": return "创始人";
            case "DEPT_HEAD": return "部门老总";
            case "TEAM_LEADER": return "团队长";
            case "EMPLOYEE": return "员工";
            default: return role;
        }
    }
}