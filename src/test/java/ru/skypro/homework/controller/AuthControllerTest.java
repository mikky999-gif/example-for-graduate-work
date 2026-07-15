package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.skypro.homework.dto.Login;
import ru.skypro.homework.dto.Register;
import ru.skypro.homework.service.AuthService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

    // Unit-тесты для AuthController

class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void register_shouldReturnCreated() throws Exception {
        // Given
        Register register = new Register();
        register.setUsername("user@example.com");
        register.setPassword("1q2w3e4r");
        register.setFirstName("Иван");
        register.setLastName("Иванов");
        register.setPhone("+79000000000");
        register.setRole(ru.skypro.homework.dto.Role.USER);

        given(authService.register(any(Register.class))).willReturn(1);

        // When & Then
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());
    }

    @Test
    void register_shouldReturnBadRequestWhenInvalid() throws Exception {
        // Given
        Register register = new Register();
        register.setUsername("ab"); // Слишком короткий логин
        register.setPassword("123"); // Слишком короткий пароль

        // When & Then
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturnOkWhenCredentialsValid() throws Exception {
        // Given
        Login login = new Login();
        login.setUsername("user@example.com");
        login.setPassword("1q2w3e4r");

        given(authService.login(eq("user@example.com"), eq("1q2w3e4r"))).willReturn(true);

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk());
    }

    @Test
    void login_shouldReturnOkWhenCredentialsInvalid() throws Exception {
        // Given
        Login login = new Login();
        login.setUsername("user@example.com");
        login.setPassword("wrongpassword");

        given(authService.login(eq("user@example.com"), eq("wrongpassword"))).willReturn(false);

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk());
    }

    @Test
    void login_shouldReturnBadRequestWhenInvalid() throws Exception {
        // Given
        Login login = new Login();
        login.setUsername("ab");
        login.setPassword("123");

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest());
    }
}
