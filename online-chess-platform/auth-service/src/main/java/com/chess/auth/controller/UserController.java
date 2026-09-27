package com.chess.auth.controller;

import com.chess.auth.dto.UpdateProfileRequest;
import com.chess.auth.dto.UserProfileResponse;
import com.chess.auth.dto.UserStatsResponse;
import com.chess.auth.model.User;
import com.chess.auth.service.UserService;
import com.chess.common.exception.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable UUID id) {
        User user = userService.getById(id);
        return ResponseEntity.ok(UserProfileResponse.from(user));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<UserStatsResponse> getStats(@PathVariable UUID id) {
        User user = userService.getById(id);
        return ResponseEntity.ok(UserStatsResponse.from(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        // JwtAuthenticationFilter sets the principal name to the caller's
        // user id (as a string) — only the profile owner can edit it.
        // Note: when called through api-gateway in production, the gateway
        // has already enforced auth upstream; this check is what keeps
        // auth-service safe even when called directly.
        if (authentication == null || !id.toString().equals(authentication.getName())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only update your own profile");
        }

        User updated = userService.updateProfile(id, request);
        return ResponseEntity.ok(UserProfileResponse.from(updated));
    }
}
