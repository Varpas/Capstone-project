package com.bookworm.service;

import com.bookworm.exception.ApiException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Reads the authenticated user id (set by JwtAuthFilter) from the security context. */
public final class CurrentUser {

    private CurrentUser() {}

    /** Empty for guests, also on public endpoints. */
    public static Optional<Long> id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Long id) {
            return Optional.of(id);
        }
        return Optional.empty();
    }

    public static Long requireId() {
        return id().orElseThrow(() -> ApiException.unauthorized("Authentication required"));
    }
}
