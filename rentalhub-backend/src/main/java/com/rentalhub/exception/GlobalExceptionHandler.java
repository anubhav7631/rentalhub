package com.rentalhub.exception;

import com.rentalhub.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║          GlobalExceptionHandler — CENTRALIZED ERROR HANDLING    ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║                                                                  ║
 * ║  @RestControllerAdvice = @ControllerAdvice + @ResponseBody      ║
 * ║  Applies to ALL controllers in the application.                 ║
 * ║                                                                  ║
 * ║  EXCEPTION PRIORITY:                                            ║
 * ║  Spring picks the MOST SPECIFIC handler for each exception.     ║
 * ║  If a handler for the exact exception class exists, it wins     ║
 * ║  over a handler for its parent class.                           ║
 * ║                                                                  ║
 * ║  ORDER IN THIS FILE:                                            ║
 * ║  1. Security exceptions (401, 403)                              ║
 * ║  2. Validation exceptions (400)                                 ║
 * ║  3. Business exceptions (400, 404)                              ║
 * ║  4. Infrastructure exceptions (409, 413, 415)                   ║
 * ║  5. Generic fallback (500)                                      ║
 * ║                                                                  ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 1. SECURITY EXCEPTIONS ────────────────────────────────────

    /**
     * 401 — Bad credentials (wrong password)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        log.warn("Authentication failed for request: {}", request.getRequestURI());

        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
            "Invalid email or password",
            request.getRequestURI());
    }

    /**
     * 401 — Disabled account
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(
            DisabledException ex, HttpServletRequest request) {

        return build(HttpStatus.UNAUTHORIZED, "ACCOUNT_DISABLED",
            "Your account has been deactivated. Please contact support.",
            request.getRequestURI());
    }

    /**
     * 401 — Locked account
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(
            LockedException ex, HttpServletRequest request) {

        return build(HttpStatus.UNAUTHORIZED, "ACCOUNT_LOCKED",
            "Your account is locked. Please contact support.",
            request.getRequestURI());
    }

    /**
     * 403 — Authenticated but wrong role or not the owner
     *
     * AccessDeniedException is thrown by @PreAuthorize when the
     * SpEL expression evaluates to false.
     *
     * WHY NOT HANDLED BY SECURITY CONFIG?
     * We want a JSON body, not Spring's default HTML 403 page.
     * This handler intercepts it before Spring Security's default
     * AccessDeniedHandler writes its response.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied to {} for {}",
            request.getRequestURI(), request.getRemoteUser());

        return build(HttpStatus.FORBIDDEN, "FORBIDDEN",
            "You do not have permission to perform this action",
            request.getRequestURI());
    }

    // ── 2. VALIDATION EXCEPTIONS ──────────────────────────────────

    /**
     * 400 — @Valid annotation on @RequestBody triggered validation errors.
     *
     * Returns the errors[] array listing each failed field.
     *
     * Example:
     *   POST /api/properties with missing propertyName
     *   → 400 { errors: [{ field: "propertyName", message: "required" }] }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        BindingResult binding = ex.getBindingResult();

        List<ErrorResponse.FieldError> fieldErrors = binding.getFieldErrors()
            .stream()
            .map(fe -> ErrorResponse.FieldError.builder()
                .field(fe.getField())
                .message(fe.getDefaultMessage())
                .rejectedValue(fe.getRejectedValue())
                .build())
            .collect(Collectors.toList());

        // Also collect global errors (class-level constraints)
        binding.getGlobalErrors().forEach(ge ->
            fieldErrors.add(ErrorResponse.FieldError.builder()
                .field(ge.getObjectName())
                .message(ge.getDefaultMessage())
                .build())
        );

        log.debug("Validation failed for {}: {} error(s)",
            request.getRequestURI(), fieldErrors.size());

        ErrorResponse response = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("VALIDATION_FAILED")
            .message(fieldErrors.size() + " validation error(s). See 'errors' field for details.")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .errors(fieldErrors)
            .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 400 — Request body cannot be parsed (malformed JSON)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY",
            "Request body is malformed or missing. Please check the request format.",
            request.getRequestURI());
    }

    /**
     * 400 — Required @RequestParam is missing
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
            "Required parameter '" + ex.getParameterName() + "' is missing",
            request.getRequestURI());
    }

    /**
     * 400 — @PathVariable or @RequestParam has wrong type
     * e.g. GET /api/properties/abc when id is expected as Long
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String expected = ex.getRequiredType() != null
            ? ex.getRequiredType().getSimpleName()
            : "unknown";

        return build(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH",
            "Parameter '" + ex.getName() + "' must be of type " + expected +
            ". Received: '" + ex.getValue() + "'",
            request.getRequestURI());
    }

    // ── 3. BUSINESS EXCEPTIONS ────────────────────────────────────

    /**
     * 400 — Business rule violation
     * e.g. "Property already has a current tenant"
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {

        log.debug("Bad request at {}: {}", request.getRequestURI(), ex.getMessage());

        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
            ex.getMessage(),
            request.getRequestURI());
    }

    /**
     * 404 — Entity not found
     * e.g. "Property with id 99 not found"
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.debug("Resource not found at {}: {}", request.getRequestURI(), ex.getMessage());

        return build(HttpStatus.NOT_FOUND, "NOT_FOUND",
            ex.getMessage(),
            request.getRequestURI());
    }

    // ── 4. INFRASTRUCTURE EXCEPTIONS ─────────────────────────────

    /**
     * 409 — Database unique constraint violation
     * e.g. duplicate payment for same month/property/tenant
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.warn("Data integrity violation at {}: {}", request.getRequestURI(), ex.getMessage());

        // Extract meaningful message without exposing DB internals
        String message = "A duplicate record already exists. " +
            "Please check for existing entries before creating a new one.";

        return build(HttpStatus.CONFLICT, "DUPLICATE_ENTRY", message,
            request.getRequestURI());
    }

    /**
     * 404 — No handler found for the URL
     * e.g. GET /api/nonexistent
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(
            NoHandlerFoundException ex, HttpServletRequest request) {

        return build(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND",
            "No endpoint found for " + ex.getHttpMethod() + " " + ex.getRequestURL(),
            request.getRequestURI());
    }

    /**
     * 405 — Wrong HTTP method
     * e.g. POST to an endpoint that only accepts GET
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
            ex.getMethod() + " is not supported for this endpoint. " +
            "Supported: " + ex.getSupportedHttpMethods(),
            request.getRequestURI());
    }

    /**
     * 413 — File upload exceeds max size
     * Triggered by spring.servlet.multipart.max-file-size limit
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
            "Uploaded file exceeds the maximum allowed size of 10MB",
            request.getRequestURI());
    }

    // ── 5. GENERIC FALLBACK ───────────────────────────────────────

    /**
     * 500 — Anything not caught above.
     *
     * Logs the full stack trace (important for debugging) but returns
     * a generic message to the client (never expose internals).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please try again or contact support.",
            request.getRequestURI());
    }

    // ── Private builder ───────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String error, String message, String path) {

        ErrorResponse body = ErrorResponse.builder()
            .status(status.value())
            .error(error)
            .message(message)
            .timestamp(LocalDateTime.now())
            .path(path)
            .build();

        return ResponseEntity.status(status).body(body);
    }
}