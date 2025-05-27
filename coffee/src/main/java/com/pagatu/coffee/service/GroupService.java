package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.GroupDto;
import com.pagatu.coffee.dto.NuovoGruppoRequest;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.Utente;
import com.pagatu.coffee.repository.GroupRepository;
import com.pagatu.coffee.repository.UtenteRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
public class GroupService {

    private GroupRepository groupRepository;
    private UtenteRepository utenteRepository;

    public GroupService(GroupRepository groupRepository,  UtenteRepository utenteRepository) {
        this.groupRepository = groupRepository;
        this.utenteRepository = utenteRepository;
    }


    @Transactional
    public GroupDto createGroup(NuovoGruppoRequest nuovoGruppoRequest, Long userId) {
        try {

            // 1. Fetch user (or throw if not found)
            Utente utente = utenteRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 2. Check if group already exists (Optional<T> handling)
            groupRepository.getGroupByName(nuovoGruppoRequest.getName())
                    .ifPresent(g -> { throw new RuntimeException("Gruppo già esistente"); });

            // 3. Create and save the new group
            Group group = new Group();
            group.setName(nuovoGruppoRequest.getName());
            group.setUsers(Collections.singletonList(utente));

            Group savedGroup = groupRepository.save(group);

            // 4. Map to DTO and return
            return mapToDto(savedGroup);

        } catch (RuntimeException ex) {
            // Log properly (SLF4J/Log4j instead of printStackTrace)
            log.error("Error creating group: {}", ex.getMessage(), ex);
            throw ex; // Re-throw to let the controller handle HTTP response
        }
    }

    private GroupDto mapToDto(Group group) {
        GroupDto groupDto = new GroupDto();
        groupDto.setName(group.getName());
        groupDto.setUsers(group.getUsers());
        return groupDto;
    }
}
