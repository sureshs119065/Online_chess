package com.chess.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Both fields optional — only non-null fields get applied by AuthService.updateProfile(). */
public record UpdateProfileRequest(

        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Email(message = "Email must be a valid address")
        String email
) {
}
