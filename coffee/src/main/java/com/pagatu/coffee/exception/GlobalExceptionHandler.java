package com.pagatu.coffee.exception;

import com.pagatu.coffee.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Coffee application.
 * This class provides centralized exception handling across the whole application
 * using Spring's @ControllerAdvice annotation. It handles various types of exceptions
 * and returns appropriate HTTP responses with structured error information.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles UserNotFoundException and returns a structured error response.
     * This method is triggered when a user is not found in the system.
     *
     * @param ex the UserNotFoundException that was thrown
     * @param request the HTTP servlet request that caused the exception
     * @return ResponseEntity containing ErrorResponse with NOT_FOUND status (404)
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(
            UserNotFoundException ex, HttpServletRequest request) {
        log.error("User not found: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                "USER_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value()
        );
        errorResponse.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles UserNotInGroup exception and returns a structured error response.
     * This method is triggered when a user is not a member of a specific group.
     *
     * @param ex the UserNotInGroup exception that was thrown
     * @param request the HTTP servlet request that caused the exception
     * @return ResponseEntity containing ErrorResponse with NOT_FOUND status (404)
     */
    @ExceptionHandler(UserNotInGroup.class)
    public ResponseEntity<ErrorResponse> handleUserNotInGroup(
            UserNotInGroup ex, HttpServletRequest request){
        log.error("User not in group: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                "USER_NOT_IN_GROUP",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value()
        );
        errorResponse.setPath(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles GroupNotFoundException and returns a structured error response.
     * This method is triggered when a group is not found in the system.
     *
     * @param ex the GroupNotFoundException that was thrown
     * @param request the HTTP servlet request that caused the exception
     * @return ResponseEntity containing ErrorResponse with NOT_FOUND status (404)
     */
    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGroupNotFoundException(
            GroupNotFoundException ex, HttpServletRequest request) {
        log.error("Group not found: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                "GROUP_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value()
        );
        errorResponse.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles BusinessException and returns a structured error response.
     * This method is triggered when business logic validation fails or
     * business rules are violated.
     *
     * @param ex the BusinessException that was thrown
     * @param request the HTTP servlet request that caused the exception
     * @return ResponseEntity containing ErrorResponse with BAD_REQUEST status (400)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.error("Business error: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                "BUSINESS_ERROR",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );
        errorResponse.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles NoContentAvailableException and returns a structured error response.
     * This method is triggered when requested content is not available or
     * when there is no content to return for a valid request.
     *
     * @param ex the NoContentAvailableException that was thrown
     * @param request the HTTP servlet request that caused the exception
     * @return ResponseEntity containing ErrorResponse with NO_CONTENT status (204)
     */
    @ExceptionHandler(NoContentAvailableException.class)
    public ResponseEntity<ErrorResponse> handleNoContentException(
            NoContentAvailableException  ex, HttpServletRequest request
    ){
        log.error("No Content: ", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                "NO_CONTENT",
                "An unexpected error occurred",
                HttpStatus.NO_CONTENT.value()
        );
        errorResponse.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(errorResponse);
    }

    /**
     * Handles MethodArgumentNotValidException for validation errors and returns a structured error response.
     * This method is triggered when request validation fails due to invalid input data.
     * It collects all field validation errors and returns them in a comprehensive format.
     *
     * @param ex the MethodArgumentNotValidException that was thrown containing validation errors
     * @param request the HTTP servlet request that caused the exception
     * @return ResponseEntity containing ErrorResponse with BAD_REQUEST status (400) and validation details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error-> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation error: {}", errors);

        ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "Validation failed: " + errors.toString(),
                HttpStatus.BAD_REQUEST.value()
        );
        errorResponse.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles ActiveUserMemberNotInGroup exception and returns a structured error response.
     * This method is triggered when an active user member is not found in a specific group.
     *
     * @param ex the ActiveUserMemberNotInGroup exception that was thrown
     * @param request the HTTP servlet request that caused the exception
     * @return ResponseEntity containing ErrorResponse with NOT_FOUND status (404)
     */
    @ExceptionHandler(ActiveUserMemberNotInGroup.class)
    public ResponseEntity<ErrorResponse> handleActiveUserMemberNotInGroup(
            ActiveUserMemberNotInGroup ex, HttpServletRequest request
    ){
        log.error("Active member not in group: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                "ACTIVE_MENBER_NOT_IN_GROUP",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value()
        );
        errorResponse.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

    }

    /**
     * Handles all other uncaught exceptions and returns a generic error response.
     * This method serves as a fallback handler for any exception not specifically
     * handled by other methods in this class. It prevents unhandled exceptions
     * from being exposed to the client.
     *
     * @param ex the generic Exception that was thrown
     * @param request the HTTP servlet request that caused the exception
     * @return ResponseEntity containing ErrorResponse with INTERNAL_SERVER_ERROR status (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: ", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        errorResponse.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}