package com.pagatu.auth.controller;



import com.pagatu.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/api/test/token")
    public String testToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            boolean isValid = jwtUtil.isValidToken(token);
            return "Token is " + (isValid ? "valid" : "invalid");
        }
        return "No token provided";
    }
}