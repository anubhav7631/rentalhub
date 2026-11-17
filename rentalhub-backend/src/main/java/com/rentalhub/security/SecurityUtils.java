package com.rentalhub.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityUtils — static helpers for accessing the current
 * authenticated user from anywhere in the application (services,
 * custom security beans, audit logging).
 *
 * WHY A UTILITY CLASS?
 * Without this, every service method that needs "who is the current
 * user" repeats the same SecurityContextHolder boilerplate:
 *
 *   Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *   UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
 *   Long userId = principal.getId();
 *
 * With SecurityUtils:
 *   Long userId = SecurityUtils.getCurrentUserId();
 *
 * All methods are static — this class is never instantiated.
 */
public final class SecurityUtils {

    // Private constructor — prevents instantiation of a utility class
    private SecurityUtils() {}

    /**
     * Returns the UserPrincipal of the currently authenticated request.
     * Returns null if no one is authenticated (shouldn't happen on
     * protected endpoints, but defensive coding matters).
     */
    public static UserPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }

        return null;
    }

    /**
     * Returns the database ID of the currently authenticated user.
     * Throws IllegalStateException if called outside an authenticated context —
     * this should never happen on a @PreAuthorize-protected endpoint,
     * so failing loudly here surfaces configuration mistakes early.
     */
    public static Long getCurrentUserId() {
        UserPrincipal principal = getCurrentPrincipal();
        if (principal == null) {
            throw new IllegalStateException("No authenticated user found in SecurityContext");
        }
        return principal.getId();
    }

    /**
     * Returns the email of the currently authenticated user.
     */
    public static String getCurrentUserEmail() {
        UserPrincipal principal = getCurrentPrincipal();
        if (principal == null) {
            throw new IllegalStateException("No authenticated user found in SecurityContext");
        }
        return principal.getUsername();
    }

    /**
     * Checks if the current user has the given authority/role.
     * Example: SecurityUtils.hasAuthority("ROLE_ADMIN")
     */
    public static boolean hasAuthority(String authority) {
        UserPrincipal principal = getCurrentPrincipal();
        if (principal == null) return false;

        return principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(authority));
    }
}