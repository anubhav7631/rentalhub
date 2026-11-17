package com.rentalhub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║              HealthController — API VERIFICATION                ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║                                                                  ║
 * ║  This controller exists to verify Phase 2 is working.          ║
 * ║  In real production systems, a /health endpoint is also         ║
 * ║  used by:                                                        ║
 * ║    • Load balancers (to check if the instance is alive)         ║
 * ║    • Container orchestrators (Kubernetes liveness probe)        ║
 * ║    • Monitoring tools (uptime checks)                           ║
 * ║                                                                  ║
 * ║  Note: Spring Boot Actuator (a separate dependency) provides    ║
 * ║  a much richer /actuator/health endpoint automatically.         ║
 * ║  Our manual endpoint here is for learning purposes.             ║
 * ║                                                                  ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * ANNOTATION: @RestController
 * ─────────────────────────────────────────────────────────────────
 * A composed annotation combining:
 *   @Controller   → "This class handles web requests"
 *   @ResponseBody → "Write return values directly to HTTP response body"
 *
 * Without @ResponseBody, Spring would treat return values as view
 * names (looking for a template file). With it, Spring uses Jackson
 * to serialize return values to JSON and writes them to the response.
 *
 * ANNOTATION: @RequestMapping("/health")
 * ─────────────────────────────────────────────────────────────────
 * All methods in this controller are mapped under the /health path.
 *
 * Combined with server.servlet.context-path=/api from application.yml,
 * the full URL becomes: http://localhost:8080/api/health
 *
 * @RequestMapping at class level = a "prefix" for all method mappings.
 * This is cleaner than repeating "/health" on every method.
 */
@RestController
@RequestMapping("/health")
public class Healthcontroller {

    /**
     * GET /api/health
     * ─────────────────────────────────────────────────────────────
     * Returns a JSON object confirming the API is running.
     *
     * RETURN TYPE: ResponseEntity<Map<String, Object>>
     *
     * ResponseEntity gives you full control over the HTTP response:
     *   • Status code (200, 201, 400, 404, etc.)
     *   • Headers
     *   • Body
     *
     * ResponseEntity.ok(body) is shorthand for:
     *   new ResponseEntity<>(body, HttpStatus.OK)  // 200 OK
     *
     * Map<String, Object> is serialized to JSON automatically by
     * Jackson: {"status": "UP", "service": "RentalHub API", ...}
     *
     * In production controllers, you'd typically return a DTO class
     * rather than a raw Map. We use Map here for simplicity since
     * this endpoint is only for testing.
     *
     * ANNOTATION: @GetMapping
     * Shorthand for @RequestMapping(method = RequestMethod.GET)
     * Maps HTTP GET requests to this method.
     * No path specified = inherits the class-level "/health" path.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "RentalHub API",
            "version", "0.0.1-SNAPSHOT",
            "timestamp", LocalDateTime.now().toString(),
            "message", "Phase 2 complete — Spring Boot is running!"
        ));
    }

    /**
     * GET /api/health/ping
     * ─────────────────────────────────────────────────────────────
     * Ultra-simple endpoint. Used by load balancers that just need
     * a fast 200 OK response to confirm the server is alive.
     *
     * Returns plain text ("pong") instead of JSON.
     * ResponseEntity<String> with a String body is returned as-is.
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}