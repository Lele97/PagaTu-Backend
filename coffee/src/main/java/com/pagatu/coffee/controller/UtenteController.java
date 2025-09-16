package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.UtenteDetailDto;
import com.pagatu.coffee.dto.UtenteDto;
import com.pagatu.coffee.entity.Utente;
import com.pagatu.coffee.service.UtenteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for managing user operations.
 * <p>
 * This controller provides endpoints for user management including:
 * <ul>
 *   <li>Creating and updating user profiles</li>
 *   <li>Retrieving user information by username</li>
 *   <li>Looking up users by email</li>
 *   <li>Managing user authentication data</li>
 * </ul>
 * </p>
 * <p>
 * The controller handles both user creation and updates through the same endpoint,
 * using smart resolution logic to determine whether to create or update users.
 * </p>
 */
@RestController
@RequestMapping("/api/coffee/user")
public class UtenteController {


    private final UtenteService utenteService;

    public UtenteController(UtenteService utenteService) {
        this.utenteService = utenteService;
    }

    /**
     * Creates or updates a user profile.
     * <p>
     * This endpoint handles both user creation and updates using smart
     * resolution logic. It first checks by authId, then by username,
     * and creates a new user if neither is found.
     * </p>
     *
     * @param utenteDto the user data to create or update
     * @return ResponseEntity containing the created or updated user information
     */
    @PostMapping
    public ResponseEntity<UtenteDto> createUtente(@RequestBody UtenteDto utenteDto) {
        return ResponseEntity.ok(utenteService.createUtente(utenteDto));
    }

    /**
     * Retrieves detailed user information by username.
     * <p>
     * This GET endpoint returns comprehensive user details including
     * group memberships and other profile information.
     * </p>
     *
     * @param username the username to search for
     * @return Optional containing user details if found, empty otherwise
     */
    @GetMapping(params = "username")
    public Optional<UtenteDetailDto> findUserByUsername(@RequestParam("username") String username) {
        return utenteService.findUtenteByUsername(username);
    }

    /**
     * Retrieves detailed user information by username via POST.
     * <p>
     * This POST variant provides the same functionality as the GET endpoint
     * but may be preferred in certain client scenarios.
     * </p>
     *
     * @param username the username to search for
     * @return ResponseEntity containing optional user details
     */
    @PostMapping("/by-username")
    public ResponseEntity<Optional<UtenteDetailDto>> findUserByUsernamePostParam(@RequestParam String username) {
        return ResponseEntity.ok(utenteService.findUtenteByUsername(username));
    }

    /**
     * Finds a user by email address.
     * <p>
     * This endpoint allows user lookup by email address,
     * returning the user entity if found.
     * </p>
     *
     * @param email the email address to search for
     * @return Utente entity if found, null otherwise
     */
    @GetMapping(params = "email")
    public Utente findUserByEmail(@RequestParam("email") String email) {
        return utenteService.findByEmail(email);
    }
}