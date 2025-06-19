package com.pagatu.auth.controller;

import com.pagatu.auth.dto.LoginRequest;
import com.pagatu.auth.dto.LoginResponse;
import com.pagatu.auth.dto.RegisterRequest;
import com.pagatu.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<String> sendEmailForPasswordChange(@RequestParam("email") String email) {
        try {
            authService.sendEmailForResetPassword(email);
            return new ResponseEntity<>("Password reset email sent successfully for: " + email, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Unable to process password reset request");
        }
    }

    @PutMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody LoginRequest loginRequest) {
        return null;
    }
}