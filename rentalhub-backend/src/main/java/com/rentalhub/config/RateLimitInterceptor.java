package com.rentalhub.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RateLimitInterceptor — limits auth endpoint call frequency.
 *
 * Protects against:
 *   - Brute force password attacks (repeated login attempts)
 *   - Credential stuffing (automated credential testing)
 *
 * HOW IT WORKS:
 * Tracks request counts per IP address per minute window.
 * If an IP exceeds MAX_REQUESTS within WINDOW_MS, returns 429.
 *
 * LIMITATIONS:
 * This is an IN-MEMORY rate limiter — it resets on restart and
 * doesn't work across multiple server instances.
 * For production: use Redis-backed rate limiting (Bucket4j + Redis).
 *
 * Registered only for /auth/** paths in WebMvcConfig.
 */
@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    // Max 10 auth requests per IP per minute
    private static final int  MAX_REQUESTS = 10;
    private static final long WINDOW_MS    = 60_000;

    // IP → [request count, window start timestamp]
    private final ConcurrentHashMap<String, long[]> requestCounts =
        new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        String ip = getClientIp(request);
        long now  = System.currentTimeMillis();

        requestCounts.compute(ip, (key, value) -> {
            if (value == null || now - value[1] > WINDOW_MS) {
                // New window
                return new long[]{1, now};
            }
            value[0]++;
            return value;
        });

        long[] state = requestCounts.get(ip);

        if (state[0] > MAX_REQUESTS) {
            log.warn("Rate limit exceeded for IP: {} ({} requests/min)", ip, state[0]);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"status\":429,\"error\":\"TOO_MANY_REQUESTS\"," +
                "\"message\":\"Too many requests. Please wait a minute before trying again.\"}"
            );
            return false; // short-circuit — don't call the controller
        }

        return true; // allow the request
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isEmpty())
            ? xff.split(",")[0].trim()
            : request.getRemoteAddr();
    }
}