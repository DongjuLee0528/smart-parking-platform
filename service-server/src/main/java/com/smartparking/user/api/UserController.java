package com.smartparking.user.api;

import com.smartparking.global.api.ApiResponse;
import com.smartparking.global.security.CurrentUserPrincipal;
import com.smartparking.user.application.UserService;
import com.smartparking.user.dto.response.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return new ApiResponse<>(userService.getCurrentUser(principal.userId()));
    }
}
