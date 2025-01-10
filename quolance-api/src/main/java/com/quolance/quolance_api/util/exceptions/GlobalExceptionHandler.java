package com.quolance.quolance_api.util.exceptions;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    Map<String, String> errors = new HashMap<>();
    List<String> generalErrors = new ArrayList<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      if (error instanceof FieldError fieldErr) {
        String fieldName = fieldErr.getField();
        String errorMessage = fieldErr.getDefaultMessage();
        errors.put(fieldName, errorMessage);
      } else {
        generalErrors.add(error.getDefaultMessage());
      }
    });

    HttpErrorResponse response = HttpErrorResponse.of("Unprocessable entity", 422, errors, generalErrors);

    return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<HttpErrorResponse> handleException(ApiException e) {
    log.info("Handling ApiException: {}", e.getMessage());
    var response = HttpErrorResponse.of(e.getMessage(), e.getStatus(), e.getErrors(), null);
    return new ResponseEntity<>(response, HttpStatus.valueOf(e.getStatus()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<HttpErrorResponse> handleException(BadCredentialsException e) {
    log.info("Handling BadCredentialsException: {}", e.getMessage());
    var response = HttpErrorResponse.of(e.getMessage(), 401, null, null);
    return new ResponseEntity<>(response, HttpStatus.valueOf(401));
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<HttpErrorResponse> handleException(AuthorizationDeniedException e) {
    log.info("Handling AuthorizationDeniedException: {}", e.getMessage());
    var response = HttpErrorResponse.of(e.getMessage(), 403, null, null);
    return new ResponseEntity<>(response, HttpStatus.valueOf(403));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<HttpErrorResponse> handleException(Exception e) {
    log.error("Unhandled exception", e);
    var response = HttpErrorResponse.of("Unexpected error", 500);
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<HttpErrorResponse> handleException(ConstraintViolationException e) {
        log.info("Handling ConstraintViolationException: {}", e.getMessage());
        Map<String, String> errors = new HashMap<>();
        e.getConstraintViolations().forEach(violation -> {
        String fieldName = violation.getPropertyPath().toString();
        String errorMessage = violation.getMessage();
        errors.put(fieldName, errorMessage);
        });

        var response = HttpErrorResponse.of("Unprocessable entity", 422, errors, null);
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

}

