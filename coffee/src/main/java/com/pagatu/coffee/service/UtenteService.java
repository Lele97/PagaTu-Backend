package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.NuovoGruppoRequest;
import com.pagatu.coffee.dto.UtenteDto;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.Status;
import com.pagatu.coffee.entity.Utente;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.UtenteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UtenteService {

    private final UtenteRepository utenteRepository;
    private final GroupRepository groupRepository;
    private final GroupService groupService;

    public UtenteService(UtenteRepository utenteRepository, GroupRepository groupRepository, GroupService groupService) {
        this.utenteRepository = utenteRepository;
        this.groupRepository = groupRepository;
        this.groupService = groupService;
    }

    @Transactional
    public UtenteDto createUtente(UtenteDto utenteDto) {
        log.info("Creating/updating user: {}", utenteDto);

        // Controlla se l'utente esiste già per authId
        Optional<Utente> existingByAuthId = utenteRepository.findByAuthId(utenteDto.getAuthId());
        if (existingByAuthId.isPresent()) {
            Utente existing = existingByAuthId.get();
            log.info("Existing user found by authId: {}", utenteDto.getAuthId());

            // Aggiorna i gruppi se necessario
            if (utenteDto.getGroups() != null) {
                existing.setGroups(processGroups(utenteDto.getGroups(), existing.getId()));
                existing = utenteRepository.save(existing);
            }

            return mapToDto(existing);
        }

        // Controlla se l'utente esiste già per username
        Optional<Utente> existingByUsername = utenteRepository.findByUsername(utenteDto.getUsername());
        if (existingByUsername.isPresent()) {
            Utente existing = existingByUsername.get();
            existing.setAuthId(utenteDto.getAuthId());
            existing.setEmail(utenteDto.getEmail());

            // Aggiorna i gruppi
            if (utenteDto.getGroups() != null) {
                existing.setGroups(processGroups(utenteDto.getGroups(), existing.getId()));
            }

            Utente updated = utenteRepository.save(existing);
            log.info("Updated existing user by username: {}", utenteDto.getUsername());
            return mapToDto(updated);
        }

        // Crea nuovo utente
        Utente utente = new Utente();
        utente.setAuthId(utenteDto.getAuthId());
        utente.setUsername(utenteDto.getUsername());
        utente.setEmail(utenteDto.getEmail());
        utente.setName(utenteDto.getName());
        utente.setLastname(utenteDto.getLastname());
        utente.setStatus(Status.NON_PAGATO);

        // Salva prima l'utente per generare l'ID
        Utente savedUser = utenteRepository.save(utente);

        // Processa i gruppi con l'ID dell'utente salvato (NON authId)
        if (utenteDto.getGroups() != null) {
            savedUser.setGroups(processGroups(utenteDto.getGroups(), savedUser.getId()));
            savedUser = utenteRepository.save(savedUser);
        }

        log.info("New user created: {}", savedUser.getUsername());
        return mapToDto(savedUser);
    }

    /**
     * Processa i gruppi convertendo da List<String> a List<Group>
     * Gestisce diversi tipi di input per massima flessibilità
     */
    private List<Group> processGroups(Object groupsInput, Long userId) {
        if (groupsInput == null) {
            return Collections.emptyList();
        }

        List<String> groupNames = new ArrayList<>();

        // Gestisce List<String> direttamente
        if (groupsInput instanceof List<?>) {
            List<?> groupList = (List<?>) groupsInput;
            for (Object item : groupList) {
                String groupName = extractGroupName(item);
                if (groupName != null && !groupName.trim().isEmpty()) {
                    groupNames.add(groupName.trim());
                }
            }
        } else if (groupsInput instanceof String) {
            // Gestisce singola stringa
            String groupName = ((String) groupsInput).trim();
            if (!groupName.isEmpty()) {
                groupNames.add(groupName);
            }
        }

        // Rimuove duplicati e converte in Group entities
        return groupNames.stream()
                .distinct()
                .map(groupName -> findOrCreateGroup(groupName, userId))
                .collect(Collectors.toList());
    }

    /**
     * Estrae il nome del gruppo da diversi tipi di oggetti
     */
    private String extractGroupName(Object groupObj) {
        if (groupObj == null) {
            return null;
        }

        if (groupObj instanceof String) {
            return (String) groupObj;
        } else if (groupObj instanceof Group) {
            return ((Group) groupObj).getName();
        } else if (groupObj instanceof Map) {
            Map<?, ?> groupMap = (Map<?, ?>) groupObj;
            Object name = groupMap.get("name");
            return name != null ? name.toString() : null;
        }

        // Try toString as fallback
        return groupObj.toString();
    }

    /**
     * Trova un gruppo esistente o ne crea uno nuovo
     */
    private Group findOrCreateGroup(String groupName, Long userId) {

        // Controlla se esiste già
        Optional<Group> existingGroup = groupRepository.getGroupByName(groupName);

        if (existingGroup.isPresent()) {
            return existingGroup.get();
        }

        log.info("Creating new group: {} for user ID: {}", groupName, userId);

        // Crea il gruppo ma NON lo salvi ancora
        Group newGroup = new Group();
        newGroup.setName(groupName);


        groupRepository.save(newGroup);

        // Carica l'utente
        Utente utente = utenteRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Aggiunge il gruppo alla lista dei gruppi dell'utente
        utente.getGroups().add(newGroup);

        // Salva l'utente. Hibernate capisce che deve creare anche il gruppo e la join
        utenteRepository.save(utente);

        return newGroup;
    }

    @Transactional(readOnly = true)
    public Optional<Utente> findByUsername(String username) {
        return utenteRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<Utente> findAll() {
        return utenteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Utente> findById(Long id) {
        return utenteRepository.findById(id);
    }

    /**
     * Converte Utente entity in DTO
     */
    private UtenteDto mapToDto(Utente utente) {
        UtenteDto dto = new UtenteDto();
        dto.setId(utente.getId());
        dto.setAuthId(utente.getAuthId());
        dto.setUsername(utente.getUsername());
        dto.setEmail(utente.getEmail());
        dto.setName(utente.getName());
        dto.setLastname(utente.getLastname());
        dto.setStatus(utente.getStatus());

        // Converte i gruppi da List<Group> a List<String> per il DTO
        if (utente.getGroups() != null) {
            List<String> groupNames = utente.getGroups().stream()
                    .map(Group::getName)
                    .toList();
            dto.setGroups(groupNames);
        }

        return dto;
    }
}