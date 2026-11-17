package com.rentalhub.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ErrorResponse — the ONLY error shape the API ever returns.
 *
 * Every exception handler in GlobalExceptionHandler produces
 * this exact structure. The Angular error interceptor can
 * reliably read error.error.message from every failed response.
 *
 * errors[] is populated only for validation failures —
 * it lists each field that failed and why.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private int    status;
    private String error;
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    private String path;

    // Only present for validation errors — null otherwise
    private List<FieldError> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}