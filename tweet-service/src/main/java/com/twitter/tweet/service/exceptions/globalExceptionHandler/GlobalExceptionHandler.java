package com.twitter.tweet.service.exceptions.globalExceptionHandler;

import com.twitter.tweet.service.exceptions.customExceptions.HashtagNotFoundException;
import com.twitter.tweet.service.exceptions.customExceptions.TweetNotFoundException;
import com.twitter.tweet.service.exceptions.customExceptions.UnauthorizedTweetAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TweetNotFoundException.class)
    public ResponseEntity<String> handleTweetNotFound(TweetNotFoundException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorizedTweetAccessException.class)
    public ResponseEntity<String> handleUnauthorized(UnauthorizedTweetAccessException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(HashtagNotFoundException.class)
    public ResponseEntity<String> handleHashtagNotFound(HashtagNotFoundException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }
}
