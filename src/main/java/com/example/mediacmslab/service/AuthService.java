package com.example.mediacmslab.service;

import com.example.mediacmslab.dto.AuthRequest;
import com.example.mediacmslab.dto.AuthResponse;
import com.example.mediacmslab.dto.RegisterRequest;
import com.example.mediacmslab.entity.User;
import com.example.mediacmslab.repository.UserRepository;
import com.example.mediacmslab.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }

        var user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : User.Role.USER)
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        var jwtToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder().token(jwtToken).build();
    }

    public AuthResponse authenticate(AuthRequest request) {
        // Spring Security сам проверит пароль и выбросит исключение, если он неверный
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        var jwtToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder().token(jwtToken).build();
    }
}
