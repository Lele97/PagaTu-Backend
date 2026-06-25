package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.CoffeeUserDto;
import com.pagatu.coffee.dto.GroupDto;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.repository.CoffeeUserRepository;
import com.pagatu.coffee.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoffeeUserServiceTest {

    @Mock
    private CoffeeUserRepository coffeeUserRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private CoffeeUserService coffeeUserService;

    private CoffeeUserDto inputDto;

    @BeforeEach
    void setUp() {
        inputDto = new CoffeeUserDto();
        inputDto.setAuthId(123L);
        inputDto.setUsername("testuser");
        inputDto.setEmail("test@example.com");
        inputDto.setName("Mario");
        inputDto.setLastname("Rossi");
        inputDto.setGroups(List.of("teamcoffee"));
    }

    @Test
    void createCoffeeUser_WhenUserExistsByAuthId_ShouldUpdateProfileAndSave() {
        CoffeeUser existing = new CoffeeUser();
        existing.setId(1L);
        existing.setAuthId(123L);
        existing.setUsername("testuser");
        existing.setEmail("test@example.com");

        when(coffeeUserRepository.findByAuthId(123L)).thenReturn(Optional.of(existing));
        when(coffeeUserRepository.save(existing)).thenReturn(existing);
        when(groupRepository.getGroupByName("teamcoffee")).thenReturn(Optional.of(new Group()));

        CoffeeUserDto result = coffeeUserService.createCoffeeUser(inputDto);

        assertEquals(123L, result.getAuthId());
        assertEquals("testuser", result.getUsername());
        assertEquals("Mario", existing.getName());
        assertEquals("Rossi", existing.getLastname());
        verify(coffeeUserRepository).save(existing);
    }

    @Test
    void createCoffeeUser_WhenUserExistsByUsername_ShouldUpdateAuthIdAndSave() {
        // Given
        CoffeeUser existing = new CoffeeUser();
        existing.setId(1L);
        existing.setUsername("testuser");
        existing.setEmail("old@example.com");

        when(coffeeUserRepository.findByAuthId(123L)).thenReturn(Optional.empty());
        when(coffeeUserRepository.findByUsername("testuser")).thenReturn(Optional.of(existing));
        when(coffeeUserRepository.save(existing)).thenReturn(existing);
        when(groupRepository.getGroupByName("teamcoffee")).thenReturn(Optional.of(new Group()));

        // When
        CoffeeUserDto result = coffeeUserService.createCoffeeUser(inputDto);

        // Then
        assertEquals(123L, existing.getAuthId());
        assertEquals("test@example.com", existing.getEmail());
        verify(coffeeUserRepository).save(existing);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void createCoffeeUser_WhenUserDoesNotExist_ShouldCreateNewUser() {
        // Given
        when(coffeeUserRepository.findByAuthId(123L)).thenReturn(Optional.empty());
        when(coffeeUserRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(coffeeUserRepository.save(any(CoffeeUser.class))).thenAnswer(invocation -> {
            CoffeeUser user = invocation.getArgument(0);
            user.setId(5L);
            return user;
        });
        when(groupRepository.getGroupByName("teamcoffee")).thenReturn(Optional.empty());

        GroupDto createdGroup = new GroupDto();
        createdGroup.setId(10L);
        createdGroup.setName("teamcoffee");
        when(groupService.createGroup(any(), eq(123L))).thenReturn(createdGroup);

        // When
        CoffeeUserDto result = coffeeUserService.createCoffeeUser(inputDto);

        // Then
        assertEquals(5L, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(coffeeUserRepository).save(any(CoffeeUser.class));
        verify(groupService).createGroup(any(), eq(123L));
    }

    @Test
    void createCoffeeUser_WhenGroupsIsNull_ShouldCreateUserWithoutGroupProcessing() {
        // Given
        inputDto.setGroups(null);

        when(coffeeUserRepository.findByAuthId(123L)).thenReturn(Optional.empty());
        when(coffeeUserRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(coffeeUserRepository.save(any(CoffeeUser.class))).thenAnswer(invocation -> {
            CoffeeUser user = invocation.getArgument(0);
            user.setId(5L);
            return user;
        });

        // When
        CoffeeUserDto result = coffeeUserService.createCoffeeUser(inputDto);

        // Then
        assertEquals("testuser", result.getUsername());
        verify(groupService, never()).createGroup(any(), any());
    }

    @Test
    void findByEmail_ShouldDelegateToRepository() {
        // Given
        CoffeeUser user = new CoffeeUser();
        user.setEmail("test@example.com");
        when(coffeeUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When
        CoffeeUser result = coffeeUserService.findByEmail("test@example.com");

        // Then
        assertEquals("test@example.com", result.getEmail());
    }
}