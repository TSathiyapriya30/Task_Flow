package com.taskflow.service;

import com.taskflow.dto.AuthResponse;
import com.taskflow.dto.LoginRequest;
import com.taskflow.dto.RegisterRequest;
import com.taskflow.exception.DuplicateEmailException;
import com.taskflow.exception.InvalidCredentialsException;
import com.taskflow.exception.PasswordMismatchException;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void cleanUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    private RegisterRequest validRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPassword("Password123");
        request.setConfirmPassword("Password123");
        return request;
    }

    @Test
    void register_withValidData_createsUserAndReturnsToken() {
        AuthResponse response = authService.register(validRegisterRequest());

        assertNotNull(response.getToken());
        assertEquals("jane@example.com", response.getUser().getEmail());
        assertEquals("Jane Doe", response.getUser().getName());
        assertTrue(userRepository.existsByEmail("jane@example.com"));
    }

    @Test
    void register_withMismatchedPasswords_throwsException() {
        RegisterRequest request = validRegisterRequest();
        request.setConfirmPassword("SomethingElse123");

        assertThrows(PasswordMismatchException.class, () -> authService.register(request));
    }

    @Test
    void register_withDuplicateEmail_throwsException() {
        authService.register(validRegisterRequest());

        RegisterRequest duplicate = validRegisterRequest();
        duplicate.setName("Another Name");

        assertThrows(DuplicateEmailException.class, () -> authService.register(duplicate));
    }

    @Test
    void register_neverStoresPlainTextPassword() {
        authService.register(validRegisterRequest());

        String storedHash = userRepository.findByEmail("jane@example.com").orElseThrow().getPassword();
        assertNotEquals("Password123", storedHash);
        assertTrue(storedHash.startsWith("$2")); // BCrypt hash prefix
    }

    @Test
    void login_withValidCredentials_returnsToken() {
        authService.register(validRegisterRequest());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("jane@example.com");
        loginRequest.setPassword("Password123");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response.getToken());
        assertEquals("jane@example.com", response.getUser().getEmail());
    }

    @Test
    void login_withWrongPassword_throwsException() {
        authService.register(validRegisterRequest());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("jane@example.com");
        loginRequest.setPassword("WrongPassword123");

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_withNonExistentEmail_throwsException() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nobody@example.com");
        loginRequest.setPassword("Password123");

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }
}
