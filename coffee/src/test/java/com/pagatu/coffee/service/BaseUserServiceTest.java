package com.pagatu.coffee.service;

import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.exception.GroupNotFoundException;
import com.pagatu.coffee.exception.UserNotFoundException;
import com.pagatu.coffee.repository.CoffeeUserRepository;
import com.pagatu.coffee.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseUserServiceTest {

    @Mock
    private CoffeeUserRepository coffeeUserRepository;

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private BaseUserService baseUserService;

    private CoffeeUser testUser;
    private Group testGroup;

    @BeforeEach
    void setUp() {
        testUser = new CoffeeUser();
        testUser.setId(1L);
        testUser.setAuthId(123L);
        testUser.setUsername("testuser");

        testGroup = new Group();
        testGroup.setId(1L);
        testGroup.setName("testgroup");
    }

    @Test
    void findUserByAuthId_WhenUserExists_ShouldReturnUser() {
        // Given
        Long authId = 123L;
        when(coffeeUserRepository.findByAuthId(authId)).thenReturn(Optional.of(testUser));

        // When
        CoffeeUser result = baseUserService.findUserByAuthId(authId);

        // Then
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUsername(), result.getUsername());
        verify(coffeeUserRepository).findByAuthId(authId);
    }

    @Test
    void findUserByAuthId_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        // Given
        Long authId = 999L;
        when(coffeeUserRepository.findByAuthId(authId)).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, 
            () -> baseUserService.findUserByAuthId(authId));
        
        assertEquals("User not found with auth ID: " + authId, exception.getMessage());
        verify(coffeeUserRepository).findByAuthId(authId);
    }

    @Test
    void findUserByUsername_WhenUserExists_ShouldReturnUser() {
        // Given
        String username = "testuser";
        when(coffeeUserRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        CoffeeUser result = baseUserService.findUserByUsername(username);

        // Then
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUsername(), result.getUsername());
        verify(coffeeUserRepository).findByUsername(username);
    }

    @Test
    void findUserByUsername_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        // Given
        String username = "nonexistent";
        when(coffeeUserRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, 
            () -> baseUserService.findUserByUsername(username));
        
        assertEquals("User not found with username: " + username, exception.getMessage());
        verify(coffeeUserRepository).findByUsername(username);
    }

    @Test
    void findGroupByName_WhenGroupExists_ShouldReturnGroup() {
        // Given
        String groupName = "testgroup";
        when(groupRepository.getGroupByName(groupName)).thenReturn(Optional.of(testGroup));

        // When
        Group result = baseUserService.findGroupByName(groupName);

        // Then
        assertNotNull(result);
        assertEquals(testGroup.getId(), result.getId());
        assertEquals(testGroup.getName(), result.getName());
        verify(groupRepository).getGroupByName(groupName);
    }

    @Test
    void findGroupByName_WhenGroupNotFound_ShouldThrowGroupNotFoundException() {
        // Given
        String groupName = "nonexistent";
        when(groupRepository.getGroupByName(groupName)).thenReturn(Optional.empty());

        // When & Then
        GroupNotFoundException exception = assertThrows(GroupNotFoundException.class, 
            () -> baseUserService.findGroupByName(groupName));
        
        assertEquals("Group not found: " + groupName, exception.getMessage());
        verify(groupRepository).getGroupByName(groupName);
    }

    @Test
    void findGroupWithMembershipsByName_WhenGroupExists_ShouldReturnGroup() {
        // Given
        String groupName = "testgroup";
        when(groupRepository.findGroupWithMembershipsByName(groupName)).thenReturn(Optional.of(testGroup));

        // When
        Group result = baseUserService.findGroupWithMembershipsByName(groupName);

        // Then
        assertNotNull(result);
        assertEquals(testGroup.getId(), result.getId());
        assertEquals(testGroup.getName(), result.getName());
        verify(groupRepository).findGroupWithMembershipsByName(groupName);
    }

    @Test
    void findGroupWithMembershipsByName_WhenGroupNotFound_ShouldThrowGroupNotFoundException() {
        // Given
        String groupName = "nonexistent";
        when(groupRepository.findGroupWithMembershipsByName(groupName)).thenReturn(Optional.empty());

        // When & Then
        GroupNotFoundException exception = assertThrows(GroupNotFoundException.class, 
            () -> baseUserService.findGroupWithMembershipsByName(groupName));
        
        assertEquals("Group not found: " + groupName, exception.getMessage());
        verify(groupRepository).findGroupWithMembershipsByName(groupName);
    }
}