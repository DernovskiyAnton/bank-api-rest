package com.example.bankcards.service;

import com.example.bankcards.dto.CreateUserRequestDto;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_ValidRequest_ReturnsUserDto() {

        CreateUserRequestDto request = new CreateUserRequestDto(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User"
        );

        String encodedPassword = "encodedPassword123";

        User savedUser = User.builder()
                .id(1L)
                .username(request.username())
                .email(request.email())
                .password(encodedPassword)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .roles(new HashSet<>(Set.of(Role.USER)))
                .build();

        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDto result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("testuser", result.username());
        assertEquals("test@example.com", result.email());
        assertEquals("Test", result.firstName());
        assertEquals("User", result.lastName());

        verify(userRepository).existsByUsername(request.username());
        verify(userRepository).existsByEmail(request.email());
        verify(passwordEncoder).encode(request.password());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_UsernameExists_ThrowsException() {

        CreateUserRequestDto request = new CreateUserRequestDto(
                "existinguser",
                "test@example.com",
                "password123",
                "Test",
                "User"
        );

        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(request)
        );

        assertEquals("Username already exists: existinguser", exception.getMessage());
        verify(userRepository).existsByUsername(request.username());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_EmailExists_ThrowsException() {

        CreateUserRequestDto request = new CreateUserRequestDto(
                "testuser",
                "existing@example.com",
                "password123",
                "Test",
                "User"
        );

        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(request)
        );

        assertEquals("Email already exists: existing@example.com", exception.getMessage());
        verify(userRepository).existsByUsername(request.username());
        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findById_ExistingUser_ReturnsUserDto() {

        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .roles(new HashSet<>(Set.of(Role.USER)))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDto result = userService.findById(userId);

        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals("testuser", result.username());
        assertEquals("test@example.com", result.email());

        verify(userRepository).findById(userId);
    }

    @Test
    void findById_NonExistingUser_ThrowsException() {

        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.findById(userId)
        );

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(userRepository).findById(userId);
    }

    @Test
    void deleteUser_ValidId_DeletesUser() {

        Long userIdToDelete = 2L;
        Long currentUserId = 1L;

        when(userRepository.existsById(userIdToDelete)).thenReturn(true);

        userService.deleteUser(userIdToDelete, currentUserId);

        verify(userRepository).existsById(userIdToDelete);
        verify(userRepository).deleteById(userIdToDelete);
    }

    @Test
    void deleteUser_SelfDeletion_ThrowsException() {

        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.deleteUser(userId, userId)
        );

        assertEquals("Cannot delete yourself", exception.getMessage());
        verify(userRepository).existsById(userId);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void getAllUsers_ReturnsPageOfUsers() {

        Pageable pageable = PageRequest.of(0, 10);

        User user1 = User.builder()
                .id(1L)
                .username("user1")
                .email("user1@example.com")
                .password("pass")
                .roles(new HashSet<>(Set.of(Role.USER)))
                .build();

        User user2 = User.builder()
                .id(2L)
                .username("user2")
                .email("user2@example.com")
                .password("pass")
                .roles(new HashSet<>(Set.of(Role.ADMIN)))
                .build();

        Page<User> userPage = new PageImpl<>(List.of(user1, user2), pageable, 2);
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserDto> result = userService.getAllUsers(pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("user1", result.getContent().get(0).username());
        assertEquals("user2", result.getContent().get(1).username());

        verify(userRepository).findAll(pageable);
    }
}