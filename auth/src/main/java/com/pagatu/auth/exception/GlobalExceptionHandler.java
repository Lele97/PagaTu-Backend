package com.pagatu.auth.exception;

import com.pagatu.auth.entity.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler for the authentication service.
 * This class provides centralized exception handling across all controllers,
 * ensuring consistent error responses and appropriate HTTP status codes.
 * It handles both custom application exceptions and standard Spring exceptions.
 *
 * @author PagaTu Auth Team
 * @version 1.0
 * @since 2025
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles authentication failures when user credentials are invalid.
     *
     * @param e the AuthenticationException containing details about the failure
     * @return ResponseEntity with UNAUTHORIZED status and error details
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED,
                e.getMessage(),
                "Authentication failed. Please check your credentials.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles exceptions during token cleanup batch operations.
     *
     * @param e the TokenCleanupBatchException containing details about the failure
     * @return ResponseEntity with INTERNAL_SERVER_ERROR status and error details
     */
    @ExceptionHandler(TokenCleanupBatchException.class)
    public ResponseEntity<ErrorResponse> handleTokenCleanupBatchException(TokenCleanupBatchException e) {
        log.warn("Token batch cleanup failed: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                e.getMessage(),
                "Token batch cleanup failed.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles Spring Security's BadCredentialsException for invalid login attempts.
     *
     * @param e the BadCredentialsException containing details about the failure
     * @return ResponseEntity with UNAUTHORIZED status and error details
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("Bad credentials: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials",
                "Username or password is incorrect.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles expired token exceptions for various token types.
     *
     * @param e the TokenExpiredException containing details about the expired token
     * @return ResponseEntity with UNAUTHORIZED status and error details
     */
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpiredException(TokenExpiredException e) {
        log.warn("Token expired: {} (Type: {})", e.getMessage(), e.getTokenType());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED,
                e.getMessage(),
                "Token has expired. Please request a new one.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles invalid or malformed token exceptions.
     *
     * @param e the InvalidTokenException containing details about the invalid token
     * @return ResponseEntity with UNAUTHORIZED status and error details
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(InvalidTokenException e) {
        log.warn("Invalid token: {} (Type: {})", e.getMessage(), e.getTokenType());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED,
                e.getMessage(),
                "Token is invalid or malformed.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles attempts to register a user that already exists in the system.
     *
     * @param e the UserAlreadyExistsException containing details about the duplicate user
     * @return ResponseEntity with CONFLICT status and error details
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException e) {
        log.warn("User already exists: {} (Field: {}, Value: {})", e.getMessage(), e.getField(), e.getValue());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT,
                e.getMessage(),
                "A user with this information already exists.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handles requests for user data when the user cannot be found.
     *
     * @param e the UserNotFoundException containing details about the missing user
     * @return ResponseEntity with NOT_FOUND status and error details
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e) {
        log.warn("User not found: {} (Identifier: {}, Type: {})",
                e.getMessage(), e.getIdentifier(), e.getIdentifierType());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND,
                e.getMessage(),
                "The requested user could not be found.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles rate limiting violations when clients exceed allowed request limits.
     *
     * @param e the RateLimiterException containing details about the rate limit violation
     * @return ResponseEntity with TOO_MANY_REQUESTS status, error details, and Retry-After header
     */
    @ExceptionHandler(RateLimiterException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(RateLimiterException e) {
        log.warn("Rate limit exceeded: {} (Client: {}, Wait: {}s)",
                e.getMessage(), e.getClientIdentifier(), e.getWaitTimeSeconds());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS,
                e.getMessage(),
                "Too many requests. Please try again later.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(e.getWaitTimeSeconds()))
                .body(errorResponse);
    }

    /**
     * Handles custom validation exceptions with field-level error details.
     *
     * @param e the ValidationException containing details about validation failures
     * @return ResponseEntity with BAD_REQUEST status and error details including field errors
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException e) {
        log.warn("Validation failed: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                e.getMessage(),
                "Input validation failed.",
                LocalDateTime.now()
        );

        // Add field errors if available
        if (e.getFieldErrors() != null && !e.getFieldErrors().isEmpty()) {
            errorResponse.setFieldErrors(e.getFieldErrors());
        }

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles Spring's MethodArgumentNotValidException for request body validation failures.
     *
     * @param e the MethodArgumentNotValidException containing validation error details
     * @return ResponseEntity with BAD_REQUEST status and error details including field errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Method argument validation failed: {}", e.getMessage());

        Map<String, List<String>> fieldErrors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.computeIfAbsent(fieldName, k -> List.of()).add(errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more fields contain invalid data.",
                LocalDateTime.now()
        );
        errorResponse.setFieldErrors(fieldErrors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles service unavailability exceptions for external dependencies.
     *
     * @param e the ServiceUnavailableException containing details about the unavailable service
     * @return ResponseEntity with SERVICE_UNAVAILABLE status and error details
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailableException(ServiceUnavailableException e) {
        log.error("Service unavailable: {} (Service: {})", e.getMessage(), e.getServiceName());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                e.getMessage(),
                "External service is temporarily unavailable.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Handles malformed JSON requests that cannot be parsed.
     *
     * @param e the HttpMessageNotReadableException containing details about the parsing failure
     * @return ResponseEntity with BAD_REQUEST status and error details
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Malformed JSON request: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request",
                "Request body contains invalid JSON.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles requests that are missing required parameters.
     *
     * @param e the MissingServletRequestParameterException containing details about the missing parameter
     * @return ResponseEntity with BAD_REQUEST status and error details
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("Missing request parameter: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Missing required parameter: " + e.getParameterName(),
                "A required request parameter is missing.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles parameter type mismatch exceptions.
     *
     * @param e the MethodArgumentTypeMismatchException containing details about the type mismatch
     * @return ResponseEntity with BAD_REQUEST status and error details
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch for parameter '{}': {}", e.getName(), e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid parameter type for: " + e.getName(),
                "Parameter value does not match expected type.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles requests to non-existent endpoints.
     *
     * @param e the NoHandlerFoundException containing details about the missing endpoint
     * @return ResponseEntity with NOT_FOUND status and error details
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("No handler found for {} {}", e.getHttpMethod(), e.getRequestURL());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND,
                "Endpoint not found",
                "The requested endpoint does not exist.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles generic runtime exceptions not caught by more specific handlers.
     *
     * @param e the RuntimeException containing details about the error
     * @return ResponseEntity with BAD_REQUEST status and error details
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("Runtime exception occurred: {}", e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                e.getMessage(),
                "An error occurred while processing your request.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Fallback handler for all other exceptions not handled by specific methods.
     *
     * @param e the Exception containing details about the unexpected error
     * @return ResponseEntity with INTERNAL_SERVER_ERROR status and generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error occurred: {}", e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred. Please try again later.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}