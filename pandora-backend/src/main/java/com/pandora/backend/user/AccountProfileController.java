package com.pandora.backend.user;

import com.pandora.backend.audit.Auditable;
import com.pandora.backend.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 账号资料控制器——管理员独占的账号信息维护接口。
 * 仅 ADMIN 角色可调用。
 */
@RestController
@RequestMapping("/user")
@PreAuthorize("hasRole('ADMIN')")
public class AccountProfileController {
    private final AccountProfileService profiles;
    public AccountProfileController(AccountProfileService profiles) { this.profiles = profiles; }

    /**
     * 更新账号资料（管理员独占）。
     * 支持修改工号、真实姓名、邮箱、手机、身份证、生日等字段。
     *
     * @param auth 认证信息
     * @param id 目标账号ID
     * @param request 包含待更新字段的请求体
     * @return 更新后的账号信息
     */
    @PutMapping("/{id}/profile")
    @Auditable(operationType = "USER_PROFILE_ADMIN_UPDATE", targetType = "USER")
    public ApiResponse<Map<String, Object>> updateProfile(Authentication auth, @PathVariable long id,
                                                          @RequestBody Map<String, Object> request) {
        return ApiResponse.success(profiles.update((Long) auth.getPrincipal(), id, request));
    }
}