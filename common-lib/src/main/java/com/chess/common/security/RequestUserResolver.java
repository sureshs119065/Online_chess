package com.chess.common.security;

import com.chess.common.exception.ApiException;

import java.util.UUID;

/**
 * Every service behind api-gateway gets the caller's verified user id as an
 * X-User-Id request header (JwtAuthFilter injects it after validating the
 * JWT). Sessions 3–7 built every controller to read the caller's id from
 * the request body or path instead (ResignRequest.playerId,
 * JoinQueueRequest.userId, the {userId} in /api/notifications/{userId},
 * etc.) — convenient for hitting a service directly while building it, but
 * it means a client could claim to be any user just by changing a field,
 * even once the gateway is in front of everything and doing real auth.
 *
 * Call this from a controller with whatever the client-supplied id was
 * (may be null) and the raw X-User-Id header value (may be null, e.g. a
 * request made directly against the service rather than through the
 * gateway). The header wins whenever it's present — a mismatched
 * client-supplied id is rejected outright rather than silently ignored,
 * since sending one that doesn't match your own token is far more likely
 * a bug (or an attack) than something to quietly paper over.
 */
public final class RequestUserResolver {

    private RequestUserResolver() {
    }

    /**
     * @param headerValue     raw X-User-Id header value, or null if absent
     * @param claimedUserId   the id the request body/path claims, or null if none was supplied
     * @return the user id this request should be trusted as acting for
     * @throws ApiException 401 if the header is present but not a valid UUID,
     *                       403 if both are present but don't match,
     *                       400 if neither is present
     */
    public static UUID resolve(String headerValue, UUID claimedUserId) {
        if (headerValue != null && !headerValue.isBlank()) {
            UUID authenticatedUserId;
            try {
                authenticatedUserId = UUID.fromString(headerValue);
            } catch (IllegalArgumentException ex) {
                throw ApiException.unauthorized("X-User-Id header is not a valid user id");
            }

            if (claimedUserId != null && !claimedUserId.equals(authenticatedUserId)) {
                throw ApiException.forbidden(
                        "The id in this request doesn't match the authenticated caller");
            }
            return authenticatedUserId;
        }

        // No header — either this call bypassed the gateway (documented
        // local/direct-to-service testing use case, e.g. Postman against
        // the service's own port) or hit one of the gateway's public
        // routes. Fall back to the client-supplied id so that workflow
        // keeps working; there's nothing more trustworthy to check it
        // against here.
        if (claimedUserId == null) {
            throw ApiException.badRequest("No X-User-Id header and no user id supplied with the request");
        }
        return claimedUserId;
    }
}
