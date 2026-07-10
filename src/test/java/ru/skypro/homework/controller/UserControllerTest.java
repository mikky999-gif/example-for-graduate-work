package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.skypro.homework.service.UserService;
import ru.skypro.homework.user.NewPassword;
import ru.skypro.homework.user.UpdateUser;
import ru.skypro.homework.user.User;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

    //  Unit-тесты для UserController

class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();

        authentication = new UsernamePasswordAuthenticationToken(
                "testUser",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void getCurrentUser_shouldReturnUserProfile() throws Exception {
        // Given
        User user = createTestUser(1, "user@example.com", "Иван", "Иванов", "+79000000000", ru.skypro.homework.dto.Role.USER, "avatar.jpg");

        given(userService.getCurrentUser(authentication)).willReturn(user);

        // When & Then
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.firstName").value("Иван"));
    }

    @Test
    void updateUser_shouldReturnUpdatedUser() throws Exception {
        // Given
        UpdateUser updateUser = new UpdateUser();
        updateUser.setFirstName("Сергей");
        updateUser.setLastName("Петров");
        updateUser.setPhone("+79001111111");

        User updatedUser = createTestUser(1, "user@example.com", "Сергей", "Петров", "+79001111111", ru.skypro.homework.dto.Role.USER, "avatar.jpg");

        given(userService.updateProfile(eq(updateUser), eq(authentication))).willReturn(updatedUser);

        // When & Then
        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Сергей"))
                .andExpect(jsonPath("$.lastName").value("Петров"));
    }

    @Test
    void changePassword_shouldReturnOk() throws Exception {
        // Given
        NewPassword newPassword = new NewPassword();
        newPassword.setCurrentPassword("oldpass123");
        newPassword.setNewPassword("newpass456");

        given(userService.changePassword(eq(newPassword), eq(authentication))).willReturn(true);

        // When & Then
        mockMvc.perform(post("/users/set_password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPassword)))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_shouldThrowExceptionWhenOldPasswordWrong() throws Exception {
        // Given
        NewPassword newPassword = new NewPassword();
        newPassword.setCurrentPassword("wrongpassword");
        newPassword.setNewPassword("newpass456");

        given(userService.changePassword(eq(newPassword), eq(authentication)))
                .willThrow(new RuntimeException("Old password mismatch"));

        // When & Then
        mockMvc.perform(post("/users/set_password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPassword)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void uploadAvatar_shouldReturnNoContent() throws Exception {
        // Given
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "avatar.jpg",
                "image/jpeg",
                "avatar-content".getBytes()
        );

        doNothing().when(userService).uploadAvatar(eq(image), eq(authentication));

        // When & Then
        mockMvc.perform(multipart("/users/me/image")
                        .file(image)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNoContent());
    }

    // Helper methods
    private User createTestUser(int id, String email, String firstName, String lastName, String phone, ru.skypro.homework.dto.Role role, String image) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setRole(role);
        user.setImage(image);
        return user;
    }
}
