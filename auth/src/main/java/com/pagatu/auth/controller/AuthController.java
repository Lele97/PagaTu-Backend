package com.pagatu.auth.controller;

import com.pagatu.auth.dto.AuthRequest;
import com.pagatu.auth.dto.AuthResponse;
import com.pagatu.auth.dto.RegisterRequest;
import com.pagatu.auth.dto.UserDto;
import com.pagatu.auth.service.AuthService;
import com.pagatu.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = authService.authenticate(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody RegisterRequest request) {
        UserDto user = userService.registerUser(request.getUsername(), request.getEmail(), request.getPassword());


        return ResponseEntity.ok(user);

    }

    @GetMapping("/user/{username}")
    public ResponseEntity<UserDto> getUserInfo(@PathVariable String username) {
        UserDto user = userService.getUserByUsername(username);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }
}