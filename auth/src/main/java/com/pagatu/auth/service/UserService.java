package com.pagatu.auth.service;

import com.pagatu.auth.dto.UserDto;

import com.pagatu.auth.entity.User;
import com.pagatu.auth.mapper.UserMapper;
import com.pagatu.auth.repository.UserRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    public UserDto registerUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username is already taken");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already in use");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        Set<String> roles = new HashSet<>();
        roles.add("USER");
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        UserDto userDto = userMapper.toDto(savedUser);

        try {
            // Send Kafka event
            Map<String, Object> userCreatedEvent = new HashMap<>();
            userCreatedEvent.put("eventType", "USER_CREATED");
            userCreatedEvent.put("userId", savedUser.getId());
            userCreatedEvent.put("username", savedUser.getUsername());
            userCreatedEvent.put("email", savedUser.getEmail());

            kafkaTemplate.send("user-events", String.valueOf(savedUser.getId()), userCreatedEvent);
        } catch (Exception e) {
            // Log the error but don't fail the registration
            System.err.println("Failed to send Kafka event. Registration will proceed: " + e.getMessage());
        }

        return userDto;
    }

    public UserDto getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toDto)
                .orElse(null);
    }
}