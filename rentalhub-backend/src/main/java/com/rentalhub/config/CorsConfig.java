package com.rentalhub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                   CorsConfig — WHAT IS CORS?                    ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║                                                                  ║
 * ║  CORS = Cross-Origin Resource Sharing                           ║
 * ║                                                                  ║
 * ║  PROBLEM: Your browser enforces the "Same-Origin Policy."       ║
 * ║  It blocks JavaScript from making HTTP requests to a DIFFERENT  ║
 * ║  origin than the page was loaded from.                          ║
 * ║                                                                  ║
 * ║  Your Angular app runs at: http://localhost:4200                ║
 * ║  Your Spring Boot API is at: http://localhost:8080              ║
 * ║  These are DIFFERENT origins (different port = different origin)║
 * ║                                                                  ║
 * ║  Without CORS config: browser BLOCKS every API call from Angular║
 * ║  Error: "Access to XMLHttpRequest blocked by CORS policy"       ║
 * ║                                                                  ║
 * ║  SOLUTION: The server tells the browser which origins are        ║
 * ║  allowed to make cross-origin requests, via HTTP response        ║
 * ║  headers:                                                        ║
 * ║    Access-Control-Allow-Origin: http://localhost:4200           ║
 * ║    Access-Control-Allow-Methods: GET, POST, PUT, DELETE         ║
 * ║    Access-Control-Allow-Headers: Authorization, Content-Type    ║
 * ║                                                                  ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
@Configuration
public class CorsConfig {

    /**
     * ANNOTATION: @Value
     * ─────────────────────────────────────────────────────────────
     * Injects a value from application.yml into this field.
     * Syntax: @Value("${property.key}")
     *
     * Spring reads app.cors.allowed-origins from application.yml
     * and injects it here as a String at startup.
     *
     * In dev: "http://localhost:4200" (Angular dev server)
     * In prod: "https://www.rentalhub.com" (deployed frontend)
     *
     * By externalizing this to config, you can change the allowed
     * origin without modifying code — just change application.yml
     * or set an environment variable.
     */
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * BEAN: CorsFilter
     * ─────────────────────────────────────────────────────────────
     * Creates a servlet filter that adds CORS headers to every
     * HTTP response. This filter runs BEFORE Spring Security filters,
     * which is critical — preflight OPTIONS requests need CORS headers
     * before Security can process them.
     *
     * HOW CORS PREFLIGHT WORKS:
     * Before a browser sends a PUT/DELETE/POST request with custom
     * headers, it first sends an OPTIONS "preflight" request to ask:
     * "Am I allowed to make this request?"
     *
     * Without CORS filter: Spring Security rejects the OPTIONS request
     * with 401. Browser sees this as CORS failure and blocks the
     * actual request.
     *
     * With this filter: OPTIONS requests get a 200 with CORS headers,
     * browser proceeds, actual request succeeds.
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        /*
         * allowCredentials(true): Allows the browser to send cookies
         * and Authorization headers in cross-origin requests.
         * REQUIRED when Angular sends the JWT Authorization header.
         *
         * IMPORTANT: When allowCredentials=true, you CANNOT use
         * allowedOrigins("*") — you must specify exact origins.
         * Using "*" with credentials is a security risk and browsers
         * reject it.
         */
        config.setAllowCredentials(true);

        /*
         * addAllowedOrigin: The exact origin(s) allowed to make requests.
         * In production, this would be "https://www.yourapp.com"
         *
         * We split by comma in case multiple origins are configured:
         * allowed-origins: http://localhost:4200,http://localhost:3000
         */
        for (String origin : allowedOrigins.split(",")) {
            config.addAllowedOrigin(origin.trim());
        }

        /*
         * Allowed HTTP methods.
         * PATCH is included for partial updates (e.g., update just the
         * status field of a maintenance request).
         * OPTIONS is required for preflight requests.
         */
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        /*
         * Allowed request headers.
         * "Authorization" is critical — this is how JWT tokens are sent:
         *   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
         * "Content-Type" allows sending JSON bodies.
         * "*" means allow any header — fine for development.
         */
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "*"));

        /*
         * Exposed headers: which response headers Angular can read.
         * By default, browsers only expose a few standard headers.
         * If you add custom response headers (like X-Total-Count for
         * pagination), list them here so Angular can access them.
         */
        config.setExposedHeaders(List.of("Authorization", "X-Total-Count"));

        /*
         * How long (seconds) browsers cache the preflight response.
         * 3600 = 1 hour. Browser won't send another OPTIONS request
         * for the same route for 1 hour — improves performance.
         */
        config.setMaxAge(3600L);

        /*
         * Apply this CORS config to ALL paths ("/**").
         * UrlBasedCorsConfigurationSource maps URL patterns to
         * CORS configurations — you could have different CORS rules
         * for different paths, but /* is simplest for an API.
         */
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}