package com.taskflow.auth.service;

import com.taskflow.auth.model.User;
import com.taskflow.auth.repository.UserRepository;
import com.taskflow.auth.dto.AuthResponse;
import com.taskflow.auth.dto.LoginRequest;
import com.taskflow.auth.dto.RegisterRequest;
import com.taskflow.auth.dto.UserResponse;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.DuplicateResourceException;
import com.taskflow.auth.mapper.UserMapper;
import com.taskflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("An account with email '" + normalizedEmail + "' already exists.");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getName());
        return AuthResponse.of(token, userMapper.toResponse(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid email or password."));

        String token = jwtService.generateToken(authentication);
        return AuthResponse.of(token, userMapper.toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found."));
        return userMapper.toResponse(user);
    }
}
