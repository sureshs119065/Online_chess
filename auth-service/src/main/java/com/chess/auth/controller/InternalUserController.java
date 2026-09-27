package com.chess.auth.controller;

import com.chess.auth.dto.GameResultUpdateRequest;
import com.chess.auth.service.UserService;
import com.chess.common.exception.ApiException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Service-to-service endpoints only — never routed through api-gateway's
 * public route table (see file 02-MICROSERVICES-DETAIL.md; the gateway
 * only exposes /api/auth/** and /api/users/**, not /api/internal/**), so
 * this stays unreachable from the public internet as long as the gateway
 * is every client's only entry point.
 *
 * As defense in depth (e.g. if this service is ever accidentally exposed
 * directly, or called from inside the same network by something other than
 * game-engine-service), every call must also present the shared
 * X-Internal-Api-Key header, checked against internal.service-key —
 * configure the SAME value here and in game-engine-service's
 * internal.service-key property.
 */
@RestController
@RequestMapping("/api/internal/users")
public class InternalUserController {

    private final UserService userService;
    private final String internalServiceKey;

    public InternalUserController(
            UserService userService,
            @Value("${internal.service-key}") String internalServiceKey
    ) {
        this.userService = userService;
        this.internalServiceKey = internalServiceKey;
    }

    @PutMapping("/{id}/game-result")
    public ResponseEntity<Void> applyGameResult(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedKey,
            @Valid @RequestBody GameResultUpdateRequest request
    ) {
        if (providedKey == null || !providedKey.equals(internalServiceKey)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid or missing internal service key");
        }
        userService.applyGameResult(id, request);
        return ResponseEntity.noContent().build();
    }
}
