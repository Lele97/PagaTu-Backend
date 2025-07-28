package com.pagatu.coffee.controller;

import com.pagatu.coffee.dto.UtenteDetailDto;
import com.pagatu.coffee.dto.UtenteDto;
import com.pagatu.coffee.entity.Utente;
import com.pagatu.coffee.service.UtenteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/coffee/user")
public class UtenteController {

    private final UtenteService utenteService;

    public UtenteController(UtenteService utenteService) {
        this.utenteService = utenteService;
    }

    @PostMapping
    public ResponseEntity<UtenteDto> createUtente(@RequestBody UtenteDto utenteDto) {
        return ResponseEntity.ok(utenteService.createUtente(utenteDto));
    }

    @GetMapping(params = "username")
    public Optional<UtenteDetailDto> findUserByUsername(@RequestParam("username") String username) {
        return utenteService.findUtenteByUsername(username);
    }

    @PostMapping("/by-username")
    public ResponseEntity<Optional<UtenteDetailDto>> findUserByUsernamePostParam(@RequestParam String username) {
        return ResponseEntity.ok(utenteService.findUtenteByUsername(username));
    }

    @GetMapping(params = "email")
    public Utente findUserByEmail(@RequestParam("email") String email) {
        return utenteService.findByEmail(email);
    }
}