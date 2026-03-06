package com.jobrixa.api.service;

import com.jobrixa.api.dto.AuthResponse;
import com.jobrixa.api.dto.LoginRequest;
import com.jobrixa.api.dto.RegisterRequest;
import com.jobrixa.api.dto.UserDto;
import com.jobrixa.api.entity.User;
import com.jobrixa.api.repository.UserRepository;
import com.jobrixa.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        var user = User.builder()
                .name(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        repository.save(user);

        var jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(jwtToken)
                .user(UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getName())
                        .build())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
                
        var jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(jwtToken)
                .user(UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getName())
                        .build())
                .build();
    }
}
