package com.twitter.social.service.exception;

import com.twitter.social.service.response.ApiResponse;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(SocialException.class)
    public ResponseEntity<ApiResponse<String>> handleSocialException(SocialException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ApiResponse<>("error", ex.getMessage(), null));
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleProfileNotFound(ProfileNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>("error", ex.getMessage(), null));
    }

    @ExceptionHandler(UnauthorizedProfileActionException.class)
    public ResponseEntity<ApiResponse<String>> handleUnauthorized(UnauthorizedProfileActionException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>("error", ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity
                .badRequest()
                .body(new ApiResponse<>("error", "Validation failed", fieldErrors));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiResponse<>("error", "File too large. Max upload size exceeded.", null));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<String>> handleMultipart(MultipartException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ApiResponse<>("error", "Invalid or missing file in request: " + ex.getMessage(), null));
    }

    /**
     * Feign calls to tweet-service/auth-service/media-service throw
     * FeignException when the downstream service returns a non-2xx status,
     * or is unreachable. We propagate the same status code so e.g. a 404
     * from media-service surfaces as 404 here, not a generic 500.
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<String>> handleFeignException(FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }
        String message = "Downstream service call failed: " + ex.getMessage();
        return ResponseEntity
                .status(status)
                .body(new ApiResponse<>("error", message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGenericException(Exception ex) {
        ex.printStackTrace(); // IMPORTANT (kept from your original)
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("error", ex.getMessage(), null));
    }

}