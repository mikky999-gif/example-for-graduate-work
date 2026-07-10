package ru.skypro.homework.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.skypro.homework.dto.Register;
import ru.skypro.homework.entity.UserEntity;
import ru.skypro.homework.mapper.UserMapper;
import ru.skypro.homework.repository.UserRepository;
import ru.skypro.homework.user.NewPassword;
import ru.skypro.homework.user.UpdateUser;
import ru.skypro.homework.user.User;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

    //  Unit-тесты для UserServiceImpl

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Environment env;

    @InjectMocks
    private UserServiceImpl userService;

    private Authentication authentication;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new UserEntity();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encoded-password");
        user.setFirstName("Иван");
        user.setLastName("Иванов");
        user.setPhone("+79000000000");

        authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        given(env.getProperty("app.images.root-path")).willReturn("/tmp/uploads");
        given(env.getProperty("app.images.url-prefix")).willReturn("/api/images/");
    }

    @Test
    void getCurrentUser_shouldReturnUserProfile() {
        // Given
        User userDto = new User();
        userDto.setId(1);
        userDto.setEmail("testuser");
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(userMapper.entityToDto(user)).willReturn(userDto);

        // When
        User result = userService.getCurrentUser(authentication);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getEmail());
        assertEquals("Иван", result.getFirstName());
    }

    @Test
    void getCurrentUser_shouldThrowExceptionWhenUserNotFound() {
        // Given
        given(userRepository.findByUsername("nonexistent")).willReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userService.getCurrentUser(authentication);
        });
    }

    @Test
    void updateProfile_shouldUpdateUserProfile() {
        // Given
        UpdateUser updateUser = new UpdateUser();
        updateUser.setFirstName("Сергей");
        updateUser.setLastName("Петров");
        updateUser.setPhone("+79001111111");

        UserEntity updatedUser = new UserEntity();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setFirstName("Сергей");
        updatedUser.setLastName("Петров");
        updatedUser.setPhone("+79001111111");

        User updatedUserDto = new User();
        updatedUserDto.setId(1);
        updatedUserDto.setEmail("testuser");
        updatedUserDto.setFirstName("Сергей");
        updatedUserDto.setLastName("Петров");

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(userRepository.save(user)).willReturn(updatedUser);
        given(userMapper.entityToDto(updatedUser)).willReturn(updatedUserDto);

        // When
        User result = userService.updateProfile(updateUser, authentication);

        // Then
        assertNotNull(result);
        assertEquals("Сергей", result.getFirstName());
        assertEquals("Петров", result.getLastName());
        assertEquals("+79001111111", result.getPhone());
    }

    @Test
    void changePassword_shouldReturnTrueWhenSuccess() {
        // Given
        NewPassword newPassword = new NewPassword();
        newPassword.setCurrentPassword("oldpass123");
        newPassword.setNewPassword("newpass456");

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("oldpass123", "encoded-password")).willReturn(true);
        given(passwordEncoder.encode("newpass456")).willReturn("new-encoded-password");
        given(userRepository.save(user)).willReturn(user);

        // When
        Boolean result = userService.changePassword(newPassword, authentication);

        // Then
        assertTrue(result);
        assertEquals("new-encoded-password", user.getPassword());
    }

    @Test
    void changePassword_shouldThrowExceptionWhenOldPasswordWrong() {
        // Given
        NewPassword newPassword = new NewPassword();
        newPassword.setCurrentPassword("wrongpassword");
        newPassword.setNewPassword("newpass456");

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongpassword", "encoded-password")).willReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userService.changePassword(newPassword, authentication);
        });
    }

    @Test
    void uploadAvatar_shouldUpdateUserAvatar() throws Exception {
        // Given
        MockMultipartFile image = new MockMultipartFile("image", "avatar.jpg", "image/jpeg", "content".getBytes());
        String avatarPath = "/api/images/uuid.jpg";
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));

        // When
        userService.uploadAvatar(image, authentication);

        // Then
        assertEquals(avatarPath, user.getImageUrl());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void register_shouldSaveNewUser() {
        // Given
        Register register = new Register();
        register.setUsername("newuser@example.com");
        register.setPassword("1q2w3e4r");
        register.setFirstName("Петр");
        register.setLastName("Сидоров");

        UserEntity savedUser = new UserEntity();
        savedUser.setId(2L);
        given(userRepository.save(any(UserEntity.class))).willReturn(savedUser);
        given(userMapper.dtoToEntity(register)).willReturn(new UserEntity());

        // When
        Integer result = userService.register(register);

        // Then
        assertNotNull(result);
        assertEquals(2, result.intValue());
    }

    @Test
    void getCurrentUserEntity_shouldThrowExceptionWhenUserNotFound() {
        // Given
        given(userRepository.findByUsername("nonexistent")).willReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            java.lang.reflect.Method method = UserServiceImpl.class.getDeclaredMethod("getCurrentUserEntity", Authentication.class);
            method.setAccessible(true);
            method.invoke(userService, authentication);
        });
    }
}
