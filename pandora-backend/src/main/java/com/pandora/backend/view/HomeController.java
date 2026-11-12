package com.pandora.backend.view;

import com.pandora.backend.common.ApiResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/view/home")
public class HomeController {
    private final HomeService home;
    public HomeController(HomeService home) { this.home = home; }
    @GetMapping
    public ApiResponse<Map<String, Object>> overview() {
        long actor = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(home.overview(actor));
    }
}
