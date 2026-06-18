package com.twitter.social.service.exception;

import com.twitter.social.service.response.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SocialException.class)
    public ApiResponse<String> handleSocialException(SocialException ex) {

        return new ApiResponse<>(
                "error",
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleGenericException(Exception ex) {

        ex.printStackTrace(); // IMPORTANT (add this)

        return new ApiResponse<>(
                "error",
                ex.getMessage(),   // show real error
                null
        );
    }
}