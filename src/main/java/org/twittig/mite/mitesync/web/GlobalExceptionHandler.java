package org.twittig.mite.mitesync.web;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.twittig.mite.mitesync.config.UnknownProfileException;
import org.twittig.mite.mitesync.facade.UnsupportedWorkflowException;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {

    Map<String, String> errors = new HashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.put(error.getField(), error.getDefaultMessage());
    }

    return ResponseEntity.badRequest().body(errors);
  }

  @ExceptionHandler(UnknownProfileException.class)
  public ResponseEntity<Map<String, String>> handleUnknownProfile(UnknownProfileException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("project", ex.getMessage()));
  }

  @ExceptionHandler(UnsupportedWorkflowException.class)
  public ResponseEntity<Map<String, String>> handleUnsupportedWorkflow(
      UnsupportedWorkflowException ex) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
        .body(Map.of("workflow", ex.getMessage()));
  }
}
