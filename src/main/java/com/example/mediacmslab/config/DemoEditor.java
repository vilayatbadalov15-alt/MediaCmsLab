package com.example.mediacmslab.config;

import com.example.mediacmslab.entity.User;
import com.example.mediacmslab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

@Configuration
class DemoEditor {
    @Bean
    @ConditionalOnProperty("demo.editor.password")
    ApplicationRunner createEditor(UserRepository users, PasswordEncoder encoder,
                                   @Value("${demo.editor.password}") String password) {
        return args -> {
            String email = "editor@mediacms.local";
            if (users.findByEmail(email).isEmpty()) {
                users.save(User.builder().email(email).role(User.Role.EDITOR)
                        .passwordHash(encoder.encode(password)).createdAt(Instant.now()).build());
            }
        };
    }
}
