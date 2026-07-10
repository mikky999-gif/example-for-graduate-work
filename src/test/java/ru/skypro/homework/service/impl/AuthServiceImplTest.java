package ru.skypro.homework.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.skypro.homework.dto.Register;
import ru.skypro.homework.entity.UserEntity;
import ru.skypro.homework.repository.UserRepository;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

    //  Unit-тесты для AuthServiceImpl

class AuthServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register_shouldSaveUser() {
        // Given
        Register register = new Register();
        register.setUsername("user@example.com");
        register.setPassword("1q2w3e4r");
        register.setFirstName("Иван");
        register.setLastName("Иванов");
        register.setPhone("+79000000000");

        UserEntity savedUser = new UserEntity();
        savedUser.setId(1L);
        given(userRepository.existsByUsername("user@example.com")).willReturn(false);
        given(passwordEncoder.encode("1q2w3e4r")).willReturn("encoded-password");
        given(userRepository.save(any(UserEntity.class))).willReturn(savedUser);

        // When
        Integer result = authService.register(register);

        // Then
        assertNotNull(result);
        assertEquals(1, result.intValue());
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    void register_shouldThrowExceptionWhenUserExists() {
        // Given
        Register register = new Register();
        register.setUsername("existing@example.com");
        given(userRepository.existsByUsername("existing@example.com")).willReturn(true);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            authService.register(register);
        });
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void login_shouldReturnTrueWhenCredentialsValid() {
        // Given
        UserEntity user = new UserEntity();
        user.setPassword("encoded-password");

        given(userRepository.findByUsername("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("1q2w3e4r", "encoded-password")).willReturn(true);

        // When
        Boolean result = authService.login("user@example.com", "1q2w3e4r");

        // Then
        assertTrue(result);
        verify(passwordEncoder, times(1)).matches("1q2w3e4r", "encoded-password");
    }

    @Test
    void login_shouldReturnFalseWhenUserNotFound() {
        // Given
        given(userRepository.findByUsername("nonexistent@example.com")).willReturn(Optional.empty());

        // When
        Boolean result = authService.login("nonexistent@example.com", "12345678");

        // Then
        assertFalse(result);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_shouldReturnFalseWhenPasswordInvalid() {
        // Given
        UserEntity user = new UserEntity();
        user.setPassword("encoded-password");

        given(userRepository.findByUsername("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongpassword", "encoded-password")).willReturn(false);

        // When
        Boolean result = authService.login("user@example.com", "wrongpassword");

        // Then
        assertFalse(result);
    }

    @Test
    void register_shouldEncodePassword() {
        // Given
        final Register register = new Register();
        register.setUsername("user@example.com");
        register.setPassword("1q2w3e4r");

        final UserEntity[] capturedUser = {null};
        given(userRepository.save(argThat(user -> {
            capturedUser[0] = user;
            return true;
        }))).willAnswer(invocation -> invocation.getArgument(0));


        // When
        authService.register(register);

        // Then
        assertNotNull(capturedUser[0]);
        assertEquals("encoded-password", capturedUser[0].getPassword());
    }

    @Test
    void register_shouldSaveUserWithAllFields() {
        // Given
        Register register = new Register();
        register.setUsername("user@example.com");
        register.setPassword("1q2w3e4r");
        register.setFirstName("Иван");
        register.setLastName("Иванов");
        register.setPhone("+79000000000");

        AtomicReference<UserEntity> capturedUser = new AtomicReference<>();
        given(userRepository.existsByUsername("user@example.com")).willReturn(false);
        given(passwordEncoder.encode("1q2w3e4r")).willReturn("encoded-password");
        given(userRepository.save(argThat(user -> {
            capturedUser.set(user);
            return true;
        }))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        authService.register(register);

        // Then
        assertNotNull(capturedUser);
        assertEquals("user@example.com", capturedUser.get().getUsername());
        assertEquals("Иван", capturedUser.get().getFirstName());
        assertEquals("Иванов", capturedUser.get().getLastName());
        assertEquals("+79000000000", capturedUser.get().getPhone());
    }
}
