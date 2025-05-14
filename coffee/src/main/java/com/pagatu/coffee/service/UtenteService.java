package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.UtenteDto;
import com.pagatu.coffee.entity.Utente;
import com.pagatu.coffee.repository.UtenteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
public class UtenteService {

    private final UtenteRepository utenteRepository;

    public UtenteService(UtenteRepository utenteRepository) {
        this.utenteRepository = utenteRepository;
    }

    @Transactional
    public UtenteDto createUtente(UtenteDto utenteDto) {

        log.info("Creazione/aggiornamento utente: {}", utenteDto);

        // Controlla se l'utente esiste già3
        Optional<Utente> existingByAuthId = utenteRepository.findByAuthId(utenteDto.getAuthId());
        if (existingByAuthId.isPresent()) {
            Utente existing = existingByAuthId.get();
            log.info("Utente esistente trovato per authId: {}", utenteDto.getAuthId());
            return mapToDto(existing);
        }

        // Controlla per username (nel caso in cui l'authId sia cambiato)
        Optional<Utente> existingByUsername = utenteRepository.findByUsername(utenteDto.getUsername());
        if (existingByUsername.isPresent()) {
            Utente existing = existingByUsername.get();
            existing.setAuthId(utenteDto.getAuthId()); // Aggiorna authId
            existing.setEmail(utenteDto.getEmail()); // Aggiorna email
            Utente updated = utenteRepository.save(existing);
            log.info("Utente aggiornato per username: {}", utenteDto.getUsername());
            return mapToDto(updated);
        }

        // Crea nuovo utente
        Utente utente = new Utente();
        utente.setId(utenteDto.getId());
        utente.setAuthId(utenteDto.getAuthId());
        utente.setUsername(utenteDto.getUsername());
        utente.setEmail(utenteDto.getEmail());
        utente.setName(utenteDto.getName());
        utente.setLastname(utenteDto.getLastname());
        utente.setAttivo(true);

        Utente saved = utenteRepository.save(utente);
        log.info("Nuovo utente creato: {}", saved.getUsername());
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public Optional<Utente> findByUsername(String username) {
        return utenteRepository.findByUsername(username);
    }

    private UtenteDto mapToDto(Utente utente) {
        UtenteDto dto = new UtenteDto();
        dto.setId(utente.getId());
        dto.setAuthId(utente.getAuthId());
        dto.setUsername(utente.getUsername());
        dto.setEmail(utente.getEmail());
        dto.setAttivo(utente.getAttivo());
        return dto;
    }
}