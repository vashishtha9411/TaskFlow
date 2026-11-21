package com.taskflow.auth.service;

import com.taskflow.auth.model.User;
import com.taskflow.auth.repository.UserRepository;
import com.taskflow.auth.dto.AuthResponse;
import com.taskflow.auth.dto.LoginRequest;
import com.taskflow.auth.dto.RegisterRequest;
import com.taskflow.auth.dto.UserResponse;
import com.taskflow.exception.DuplicateResourceException;
import com.taskflow.auth.mapper.UserMapper;
import com.taskflow.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private UserResponse sampleUserResponse;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("Alex Rivera")
                .email("alex@example.com")
                .password("encoded_pass")
                .build();

        sampleUserResponse = UserResponse.builder()
                .id(1L)
                .name("Alex Rivera")
                .email("alex@example.com")
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user and return JWT")
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alex Rivera")
                .email("alex@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("alex@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(1L, "alex@example.com", "Alex Rivera")).thenReturn("mock.jwt.token");
        when(userMapper.toResponse(sampleUser)).thenReturn(sampleUserResponse);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getUser().getEmail()).isEqualTo("alex@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when registering existing email")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alex Rivera")
                .email("alex@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("alex@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully login and return JWT")
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .email("alex@example.com")
                .password("password123")
                .build();

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(auth)).thenReturn("mock.login.jwt");
        when(userMapper.toResponse(sampleUser)).thenReturn(sampleUserResponse);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock.login.jwt");
        assertThat(response.getUser().getName()).isEqualTo("Alex Rivera");
    }

    @Test
    @DisplayName("Should propagate BadCredentialsException when credentials fail")
    void testLoginInvalidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("alex@example.com")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
