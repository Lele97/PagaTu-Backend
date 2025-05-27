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
        return groupRepository.getGroupByName(groupName)
                .orElseGet(() -> {
                    log.info("Creating new group: {} for user ID: {}", groupName, userId);

                    try {
                        // Crea una nuova richiesta per il gruppo
                        NuovoGruppoRequest nuovoGruppoRequest = new NuovoGruppoRequest();
                        nuovoGruppoRequest.setName(groupName);

                        // Usa il GroupService per creare il gruppo
                        groupService.createGroup(nuovoGruppoRequest, userId);

                        // Ritorna il gruppo appena creato
                        return groupRepository.getGroupByName(groupName)
                                .orElseThrow(() -> new RuntimeException("Failed to create group: " + groupName));
                    } catch (Exception e) {
                        log.error("Error creating group through GroupService: {}", e.getMessage());

                        // Fallback: crea il gruppo direttamente
                        // NOTA: Assicurati che la tua entità Group abbia il campo utente_id impostato correttamente
                        Group newGroup = new Group();
                        newGroup.setName(groupName);

                        // Se la tua entità Group ha un campo per l'utente proprietario, impostalo qui
                        // Esempio: newGroup.setOwnerId(userId); o newGroup.setOwner(utente);
                        // Questo dipende dal tuo modello dati specifico

                        return groupRepository.save(newGroup);
                    }
                });
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