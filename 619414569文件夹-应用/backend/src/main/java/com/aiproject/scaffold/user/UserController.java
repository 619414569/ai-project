package com.aiproject.scaffold.user;

import com.aiproject.scaffold.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final DatabaseUserDetailsService userDetailsService;

    public UserController(DatabaseUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @GetMapping
    public ApiResponse<List<String>> listUsers() {
        return ApiResponse.success(userDetailsService.listActiveUsernames());
    }
}
