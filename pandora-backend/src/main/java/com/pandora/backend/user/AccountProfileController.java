package com.pandora.backend.user;

import com.pandora.backend.audit.Auditable;
import com.pandora.backend.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@PreAuthorize("hasRole('ADMIN')")
public class AccountProfileController {
    private final AccountProfileService profiles;
    public AccountProfileController(AccountProfileService profiles) { this.profiles = profiles; }

    @PutMapping("/{id}/profile")
    @Auditable(operationType = "USER_PROFILE_ADMIN_UPDATE", targetType = "USER")
    public ApiResponse<Map<String, Object>> updateProfile(Authentication auth, @PathVariable long id,
                                                          @RequestBody Map<String, Object> request) {
        return ApiResponse.success(profiles.update((Long) auth.getPrincipal(), id, request));
    }
}
