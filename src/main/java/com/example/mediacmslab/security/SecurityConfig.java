package com.example.mediacmslab.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Регистрация и логин доступны всем
                        .requestMatchers("/api/auth/**").permitAll()
                        // Чтение контента доступно всем (или можно ограничить USER)
                        .requestMatchers(HttpMethod.GET, "/api/content/**").permitAll()
                        // Создание/редактирование только для EDITOR и ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/content/**").hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/content/**").hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/content/**").hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers("/api/content/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // Рекомендации только для авторизованных
                        .requestMatchers("/api/recommendations/**").authenticated()
                        // Аналитика только для EDITOR/ADMIN
                        .requestMatchers("/api/analytics/**").hasAnyRole("EDITOR", "ADMIN")
                        // Остальное требует авторизации
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
