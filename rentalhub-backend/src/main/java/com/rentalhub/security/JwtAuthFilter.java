package com.rentalhub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService            jwtService;
    private final UserDetailsServiceImpl userDetailsService;

   
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.trace("No Bearer token found in request to: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 2: Extract the token ─────────────────────────────
        // Remove "Bearer " prefix (7 characters) to get the raw JWT string
        final String jwt = authHeader.substring(7);

        // ── Step 3: Extract username (email) from token ───────────
        String userEmail;
        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            
            log.debug("JWT token validation failed for {}: {}",
                request.getRequestURI(), e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        
        if (userEmail != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load the full UserDetails from DB
            // This validates the user still exists and is active
            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(userEmail);
            } catch (Exception e) {
                log.debug("User not found for email extracted from JWT: {}", userEmail);
                filterChain.doFilter(request, response);
                return;
            }

            // Validate token against the UserDetails
            // (checks username match + not expired)
            if (jwtService.isTokenValid(jwt, userDetails)) {
                
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                
                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated user: {} for request: {}",
                    userEmail, request.getRequestURI());
            } else {
                log.debug("Token invalid or expired for user: {}", userEmail);
            }
        }

        // ── Step 5: Continue the filter chain ─────────────────────
        // ALWAYS call this — even if we didn't authenticate.
        // The authorization filter downstream handles unauthenticated requests.
        filterChain.doFilter(request, response);
    }
}